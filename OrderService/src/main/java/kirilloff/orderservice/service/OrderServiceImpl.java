package kirilloff.orderservice.service;

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

  @Override
  public OrderDto create(OrderDto dto) {
    Order order = orderMapper.toEntity(dto);
    Order resultOrder = orderRepository.save(order);
    paymentEventKafkaTemplate.send("payment", 1, dto.getUserId(),
        PaymentEvent.builder().userId(dto.getUserId()).cost(dto.getCost())
            .success(true).orderId(order.getId()).build());
    log.info("Order send to Payment Service: " + order);
    return orderMapper.toDto(resultOrder);
  }


  @KafkaListener(topicPartitions = @TopicPartition(topic = "payment", partitions = "2"))
  public void eventListenerPayment(ResponseEvent event) {
    log.info("Received Payment Event: " + event);
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

  @KafkaListener(topicPartitions = @TopicPartition(topic = "rest", partitions = "2"))
  public void eventListenerRest(ResponseEvent event) {
    log.info("Received Rest Event: " + event);
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
