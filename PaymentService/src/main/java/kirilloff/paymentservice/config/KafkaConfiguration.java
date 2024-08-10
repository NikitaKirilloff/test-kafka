package kirilloff.paymentservice.config;

import java.util.Map;
import kirilloff.common.event.PaymentEvent;
import kirilloff.common.event.ResponseEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

@Configuration
public class KafkaConfiguration {

  @Bean
  DefaultKafkaProducerFactory<String, ResponseEvent> responseEventProducerFactory(
      KafkaProperties properties) {
    Map<String, Object> producerProperties = properties.buildProducerProperties(null);
    producerProperties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    producerProperties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
    return new DefaultKafkaProducerFactory<>(producerProperties);
  }

  @Bean
  KafkaTemplate<String, ResponseEvent> responseEventKafkaTemplate(
      DefaultKafkaProducerFactory<String, ResponseEvent> responseEventProducerFactory) {
    return new KafkaTemplate<>(responseEventProducerFactory);
  }

  @Bean
  public ConsumerFactory<String, PaymentEvent> responseEventConsumerFactory(
      KafkaProperties kafkaProperties) {
    Map<String, Object> props = kafkaProperties.buildConsumerProperties(null);
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
    props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
    return new DefaultKafkaConsumerFactory<>(props);
  }


  @Bean
  public KafkaListenerContainerFactory<?> kafkaListenerContainerFactory(
      ConsumerFactory<String, PaymentEvent> responseEventConsumerFactory) {
    ConcurrentKafkaListenerContainerFactory<String, PaymentEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(responseEventConsumerFactory);
    factory.setBatchListener(false);
    return factory;
  }
}