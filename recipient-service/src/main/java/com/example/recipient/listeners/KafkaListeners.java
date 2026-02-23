package com.rapidalert.recipient.listeners;

import com.rapidalert.recipient.dto.kafka.TemplateRecipientKafka;
import com.rapidalert.recipient.entity.Recipient;
import com.rapidalert.recipient.entity.TemplateId;
import com.rapidalert.recipient.repository.RecipientRepository;
import com.rapidalert.recipient.repository.TemplateIdRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class KafkaListeners {

    private final TemplateIdRepository templateIdRepository;
    private final RecipientRepository recipientRepository;

    @Transactional
    @KafkaListener(
            topics = "#{ '${spring.kafka.topics.recipient-update}' }",
            groupId = "rapidalert",
            containerFactory = "listenerContainerFactory"
    )
    public void listener(TemplateRecipientKafka kafka) {
        switch (kafka.operation()) {
            case REMOVE -> {
                recipientRepository.findById(kafka.recipientId())
                        .map(recipient -> recipient.removeTemplate(kafka.templateId()))
                        .ifPresent(recipientRepository::saveAndFlush);
            }
            case PERSISTS -> {
                if (!templateIdRepository.existsByTemplateIdAndRecipientId(
                        kafka.templateId(),
                        kafka.recipientId()
                )) {
                    templateIdRepository.save(
                            TemplateId.builder() // TODO: mapper
                                    .recipient(
                                            Recipient.builder()
                                                    .id(kafka.recipientId())
                                                    .build()
                                    )
                                    .templateId(kafka.templateId())
                                    .build()
                    );
                }
            }
        }
    }
}
