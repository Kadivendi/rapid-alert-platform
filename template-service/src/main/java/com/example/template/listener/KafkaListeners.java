package com.rapidalert.template.listener;

import com.rapidalert.template.dto.kafka.TemplateRecipientKafka;
import com.rapidalert.template.mapper.RecipientIdMapper;
import com.rapidalert.template.repository.RecipientIdRepository;
import com.rapidalert.template.repository.TemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
public class KafkaListeners {

    private final RecipientIdRepository recipientIdRepository;
    private final TemplateRepository templateRepository;
    private final RecipientIdMapper mapper;

    @Transactional
    @KafkaListener(topics = "${spring.kafka.topics.template-update}")
    public CompletableFuture<Void> listener(TemplateRecipientKafka kafka) {
        switch (kafka.operation()) {
            case REMOVE -> {
                templateRepository.findById(kafka.templateId())
                        .map(template -> template.removeRecipient(kafka.recipientId()))
                        .ifPresent(templateRepository::saveAndFlush);
            }
            case PERSISTS -> {
                if (!recipientIdRepository.existsByTemplateIdAndRecipientId(
                        kafka.templateId(),
                        kafka.recipientId()
                )) {
                    recipientIdRepository.save(mapper.mapToEntity(kafka));
                }
            }
        }
        return CompletableFuture.completedFuture(null);
    }
}
