package kirilloff.restaurantservice.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import kirilloff.common.event.ResponseEvent;
import kirilloff.common.event.RestEvent;
import kirilloff.restaurantservice.model.RestOrder;
import kirilloff.restaurantservice.repository.RestOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.TopicPartition;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class RestService {

  private final RestOrderRepository repository;
  private final KafkaTemplate<String, ResponseEvent> kafkaTemplate;

  @CircuitBreaker(name = "restServiceToOrderCircuitBreaker")
  @KafkaListener(topicPartitions = @TopicPartition(topic = "rest", partitions = "1"))
  public void eventListener(RestEvent event) {
    log.info("Received rest event: {}", event);
    repository.save(RestOrder.builder().orderId(event.getOrderId()).items(event.getItems()).build());
    kafkaTemplate.send("rest", 2, String.valueOf(event.getOrderId()), ResponseEvent.builder().success(true).orderId(event.getOrderId()).build());
  }
}
