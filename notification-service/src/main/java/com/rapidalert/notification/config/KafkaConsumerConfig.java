package com.rapidalert.notification.config;

import com.rapidalert.notification.dto.kafka.RecipientListKafka;
import com.rapidalert.notification.triage.TriageEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    // ---- RecipientListKafka (existing internal pipeline) ----
    //
    // Class-based config — Kafka instantiates the deserializer itself from the
    // property map. The ErrorHandlingDeserializer reads its inner type out of
    // the same property map.

    public Map<String, Object> classBasedConsumerConfig() {
        Map<String, Object> properties = new HashMap<>();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        properties.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);
        properties.put(JsonDeserializer.TRUSTED_PACKAGES,
                "com.rapidalert.notification.dto.kafka,com.rapidalert.notification.triage");
        return properties;
    }

    @Bean
    public ConsumerFactory<String, RecipientListKafka> consumerFactory() {
        return new DefaultKafkaConsumerFactory<>(classBasedConsumerConfig());
    }

    @Bean
    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, RecipientListKafka>>
    listenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, RecipientListKafka> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        return factory;
    }

    // ---- TriageEvent (cross-project: disaster-triage-engine -> here) ----
    //
    // Instance-based config — we hand the deserializer instance to the
    // ConsumerFactory directly. To avoid Spring's
    //   "JsonDeserializer must be configured with property setters,
    //    or via configuration properties; not both"
    // assertion we DO NOT also list deserializer classes / trusted-packages in
    // the property map. The properties below are bootstrap + group only.

    @Bean
    public ConsumerFactory<String, TriageEvent> triageConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "rapid-alert-notification-triage");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        JsonDeserializer<TriageEvent> json = new JsonDeserializer<>(TriageEvent.class);
        json.addTrustedPackages("com.rapidalert.notification.triage");
        json.setUseTypeHeaders(false);

        return new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                new ErrorHandlingDeserializer<>(json)
        );
    }

    @Bean
    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, TriageEvent>>
    triageKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, TriageEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(triageConsumerFactory());
        return factory;
    }
}
