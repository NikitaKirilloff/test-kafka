package kirilloff.orderservice.config;

import java.util.Map;
import kirilloff.common.event.PaymentEvent;
import kirilloff.common.event.ResponseEvent;
import kirilloff.common.event.RestEvent;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

@Configuration
public class KafkaConfiguration {

  @Bean
  DefaultKafkaProducerFactory<String, PaymentEvent> paymentProducerFactory(
      KafkaProperties properties) {
    Map<String, Object> producerProperties = properties.buildProducerProperties(null);
    producerProperties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    producerProperties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
    return new DefaultKafkaProducerFactory<>(producerProperties);
  }

  @Bean
  KafkaTemplate<String, PaymentEvent> paymentKafkaTemplate(
      DefaultKafkaProducerFactory<String, PaymentEvent> paymentProducerFactory) {
    return new KafkaTemplate<>(paymentProducerFactory);
  }

  @Bean
  DefaultKafkaProducerFactory<String, RestEvent> restProducerFactory(
      KafkaProperties properties) {
    Map<String, Object> producerProperties = properties.buildProducerProperties(null);
    producerProperties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    producerProperties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
    return new DefaultKafkaProducerFactory<>(producerProperties);
  }

  @Bean
  KafkaTemplate<String, RestEvent> restKafkaTemplate(
      DefaultKafkaProducerFactory<String, RestEvent> restProducerFactory) {
    return new KafkaTemplate<>(restProducerFactory);
  }

  @Bean
  public ConsumerFactory<String, ResponseEvent> responseConsumerFactory(
      KafkaProperties kafkaProperties) {
    Map<String, Object> props = kafkaProperties.buildConsumerProperties(null);
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
    props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
    return new DefaultKafkaConsumerFactory<>(props);
  }

  @Bean
  public KafkaListenerContainerFactory<?> kafkaListenerContainerFactory(
      ConsumerFactory<String, ResponseEvent> paymentEventConsumerFactory) {
    ConcurrentKafkaListenerContainerFactory<String, ResponseEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(paymentEventConsumerFactory);
    factory.setBatchListener(false);
    return factory;
  }

  @Bean
  public NewTopic restTopic() {
    return TopicBuilder.name("rest")
        .partitions(3)
        .replicas(2)
        .compact()
        .build();
  }

  @Bean
  public NewTopic paymentTopic() {
    return TopicBuilder.name("payment")
        .partitions(3)
        .replicas(2)
        .compact()
        .build();
  }
}