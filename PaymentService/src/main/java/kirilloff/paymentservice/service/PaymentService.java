package kirilloff.paymentservice.service;

import kirilloff.common.event.PaymentEvent;
import kirilloff.common.event.ResponseEvent;
import kirilloff.paymentservice.model.Payment;
import kirilloff.paymentservice.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.TopicPartition;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

  private final PaymentRepository paymentRepository;
  private final KafkaTemplate<String, ResponseEvent> kafkaTemplate;

  @KafkaListener(topicPartitions = @TopicPartition(topic = "payment", partitions = "1"), containerFactory = "kafkaListenerContainerFactory")
  public void eventListener(PaymentEvent event) {
    log.info("Received payment event: {}", event);
    if (event.isSuccess()) {
      paymentRepository.save(Payment.builder().userId(event.getUserId()).cost(-event.getCost()).build());
      kafkaTemplate.send("payment", 2, event.getUserId(), ResponseEvent.builder().success(true).orderId(
          event.getOrderId()).build());
    } else {
      paymentRepository.save(Payment.builder().userId(event.getUserId()).cost(event.getCost()).build());
    }
  }

}
