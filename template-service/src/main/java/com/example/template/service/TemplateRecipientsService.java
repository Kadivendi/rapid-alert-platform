package com.rapidalert.template.service;

import com.rapidalert.template.client.RecipientClient;
import com.rapidalert.template.dto.request.RecipientListRequest;
import com.rapidalert.template.dto.response.RecipientResponse;
import com.rapidalert.template.dto.response.TemplateResponse;
import com.rapidalert.template.entity.Template;
import com.rapidalert.template.exception.template.TemplateNotFoundException;
import com.rapidalert.template.mapper.TemplateMapper;
import com.rapidalert.template.repository.RecipientIdRepository;
import com.rapidalert.template.repository.TemplateRepository;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TemplateRecipientsService {

    private final TemplateRepository templateRepository;
    private final RecipientIdRepository recipientIdRepository;
    private final RecipientClient recipientClient;
    private final MessageSourceService message;
    private final TemplateMapper mapper;

    public TemplateResponse addRecipients(Long clientId, Long templateId, RecipientListRequest request) {
        Template template = templateRepository.findByIdAndClientId(templateId, clientId)
                .orElseThrow(() -> new TemplateNotFoundException(
                        message.getProperty("template.not_found", templateId, clientId)
                ));

        for (Long recipientId : request.recipientIds()) {
            if (recipientIdRepository.existsByTemplateIdAndRecipientId(templateId, recipientId)) {
                log.warn("Recipient {} has already been registered for Template {}", recipientId, templateId);
                continue;
            }

            try {
                Optional.of(recipientClient.receiveRecipientById(clientId, recipientId))
                        .map(ResponseEntity::getBody)
                        .ifPresent(recipientResponse -> template.addRecipient(recipientResponse.id()));
                templateRepository.save(template);
            } catch (FeignException.NotFound e) {
                log.warn("Recipient {} not found for Client {}", recipientId, clientId);
            }
        }

        return mapper.mapToResponse(template, recipientClient);
    }

    public TemplateResponse removeRecipients(Long clientId, Long templateId, RecipientListRequest request) {
        Template template = templateRepository.findByIdAndClientId(templateId, clientId)
                .orElseThrow(() -> new TemplateNotFoundException(
                        message.getProperty("template.not_found", templateId, clientId)
                ));

        for (Long recipientId : request.recipientIds()) {
            if (templateRepository.existsByIdAndRecipientIds_recipientId(templateId, recipientId)) {
                Optional.of(recipientClient.receiveRecipientById(clientId, recipientId))
                        .map(ResponseEntity::getBody)
                        .map(RecipientResponse::id)
                        .ifPresent(template::removeRecipient);
            } else {
                log.warn("Recipient {} hasn't been registered for Template {}", recipientId, templateId);
            }
        }

        templateRepository.save(template);
        return mapper.mapToResponse(template, recipientClient);
    }
}
