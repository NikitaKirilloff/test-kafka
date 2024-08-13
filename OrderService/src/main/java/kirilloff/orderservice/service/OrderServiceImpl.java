package kirilloff.orderservice.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.Random;
import kirilloff.common.event.PaymentEvent;
import kirilloff.common.event.ResponseEvent;
import kirilloff.common.event.RestEvent;
import kirilloff.orderservice.mapper.OrderMapper;
import kirilloff.orderservice.model.Order;
import kirilloff.orderservice.model.OrderDto;
import kirilloff.orderservice.model.Status;
import kirilloff.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.TopicPartition;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
@Slf4j
public class OrderServiceImpl implements OrderService {

  private final OrderRepository orderRepository;
  private final OrderMapper orderMapper;
  @Qualifier("paymentKafkaTemplate")
  private final KafkaTemplate<String, PaymentEvent> paymentEventKafkaTemplate;
  @Qualifier("restKafkaTemplate")
  private final KafkaTemplate<String, RestEvent> restKafkaTemplate;

  @CircuitBreaker(name = "orderServiceToPaymentCircuitBreaker")
  @Override
  public OrderDto create(OrderDto dto) {
    Order order = orderMapper.toEntity(dto);
    Order resultOrder = orderRepository.save(order);
    paymentEventKafkaTemplate.send("payment", 1, dto.getUserId(),
        PaymentEvent.builder().userId(dto.getUserId()).cost(dto.getCost())
            .success(new Random().nextBoolean()).orderId(order.getId()).build());
    log.info("OrderService. Order send to Payment Service: " + order);
    return orderMapper.toDto(resultOrder);
  }


  public OrderDto getOrder(Long orderId) throws NotFoundException {
    log.info("OrderService. Get Order byId: " + orderId);
    Order order = orderRepository.findById(orderId).orElseThrow(()-> {
      log.warn("OrderService.Not Found byId: {}", orderId);
      return new NotFoundException();
    });
    return orderMapper.toDto(order);
  }

  @CircuitBreaker(name = "orderServiceToRestCircuitBreaker")
  @KafkaListener(topicPartitions = @TopicPartition(topic = "payment", partitions = "2"))
  public void eventListenerPayment(ResponseEvent event) {
    log.info("OrderService. Received Payment Event: " + event);
    Order order = orderRepository.findById(event.getOrderId()).orElseThrow();
    if (event.isSuccess()) {
      order.setStatus(Status.PAYED);
      orderRepository.save(order);
      restKafkaTemplate.send("rest", 1, String.valueOf(event.getOrderId()),
          RestEvent.builder().items(order.getItems()).orderId(order.getId()).build());
    } else {
      order.setStatus(Status.FAILED);
      orderRepository.save(order);
    }
  }

  @CircuitBreaker(name = "orderServiceToRestCircuitBreaker")
  @KafkaListener(topicPartitions = @TopicPartition(topic = "rest", partitions = "2"))
  public void eventListenerRest(ResponseEvent event) {
    log.info("OrderService. Received Rest Event: " + event);
    Order order = orderRepository.findById(event.getOrderId()).orElseThrow();
    if (event.isSuccess()) {
      order.setStatus(Status.IN_PROGRESS);
      orderRepository.save(order);
    } else {
      paymentEventKafkaTemplate.send("payment", 1, order.getUserId(),
          PaymentEvent.builder().userId(order.getUserId()).cost(order.getCost())
              .success(false).build());
      order.setStatus(Status.FAILED);
      orderRepository.save(order);
    }
  }

}
