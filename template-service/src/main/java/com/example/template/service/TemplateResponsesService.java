package com.rapidalert.template.service;

import com.rapidalert.template.client.RecipientClient;
import com.rapidalert.template.client.ShortenerClient;
import com.rapidalert.template.dto.request.NotificationOptionsRequest;
import com.rapidalert.template.dto.response.TemplateResponse;
import com.rapidalert.template.exception.template.TemplateNotFoundException;
import com.rapidalert.template.mapper.TemplateMapper;
import com.rapidalert.template.repository.TemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TemplateResponsesService {

    private final TemplateRepository templateRepository;
    private final RecipientClient recipientClient;
    private final ShortenerClient shortenerClient;
    private final MessageSourceService message;
    private final TemplateMapper mapper;

    public TemplateResponse addResponseOptions(Long clientId, Long templateId, NotificationOptionsRequest request) {
        return templateRepository.findByIdAndClientId(templateId, clientId)
                .map(template -> template.addResponse(shortenerClient.create(request).getBody()))
                .map(templateRepository::saveAndFlush)
                .map(template -> mapper.mapToResponse(template, recipientClient))
                .orElseThrow(() -> new TemplateNotFoundException(
                        message.getProperty("template.not_found", templateId, clientId)
                ));
    }
}
