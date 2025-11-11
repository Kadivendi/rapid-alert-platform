package com.rapidalert.template.listener;

import com.rapidalert.template.dto.kafka.Operation;
import com.rapidalert.template.dto.kafka.TemplateRecipientKafka;
import com.rapidalert.template.entity.RecipientId;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;

import static com.rapidalert.template.dto.kafka.Operation.PERSISTS;
import static com.rapidalert.template.dto.kafka.Operation.REMOVE;

@RequiredArgsConstructor
public class RecipientIdListener {

    private final KafkaTemplate<String, TemplateRecipientKafka> kafkaTemplate;

    @Value("${spring.kafka.topics.recipient-update}")
    private String recipientUpdateTopic;

    @PostRemove
    public void postRemove(RecipientId recipientId) {
        sendKafkaEvent(recipientId, REMOVE);
    }

    @PostPersist
    public void postPersist(RecipientId recipientId) {
        sendKafkaEvent(recipientId, PERSISTS);
    }

    private void sendKafkaEvent(RecipientId recipientId, Operation operation) {
        kafkaTemplate.send(
                recipientUpdateTopic,
                TemplateRecipientKafka.builder()
                        .recipientId(recipientId.getRecipientId())
                        .templateId(recipientId.getTemplate().getId())
                        .operation(operation)
                        .build()
        );
    }
}
