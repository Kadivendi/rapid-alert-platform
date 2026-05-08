package com.rapidalert.template.service;

import com.rapidalert.template.dto.response.TemplateHistoryResponse;
import com.rapidalert.template.entity.Template;
import com.rapidalert.template.entity.TemplateHistory;
import com.rapidalert.template.exception.history.HistoryNotFoundException;
import com.rapidalert.template.exception.template.TemplateNotFoundException;
import com.rapidalert.template.mapper.TemplateMapper;
import com.rapidalert.template.repository.TemplateHistoryRepository;
import com.rapidalert.template.repository.TemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TemplateHistoryService {

    private final TemplateHistoryRepository templateHistoryRepository;
    private final TemplateRepository templateRepository;
    private final MessageSourceService message;
    private final TemplateMapper mapper;

    public TemplateHistoryResponse create(Long clientId, Long templateId) {
        Template template = templateRepository.findByIdAndClientId(templateId, clientId)
                .orElseThrow(() -> new TemplateNotFoundException(
                        message.getProperty("template.not_found", templateId, clientId)
                ));

        Optional<TemplateHistory> optTemplateHistory = templateHistoryRepository.findByClientIdAndResponseIdAndTitleAndContent(
                clientId,
                template.getResponseId(),
                template.getTitle(),
                template.getContent()
        );
        if (optTemplateHistory.isPresent()) {
            return mapper.mapToTemplateHistoryResponse(optTemplateHistory.get());
        }

        return Optional.of(template)
                .map(mapper::mapToTemplateHistory)
                .map(templateHistory -> templateHistory.addClient(clientId))
                .map(templateHistoryRepository::saveAndFlush)
                .map(mapper::mapToTemplateHistoryResponse)
                .orElseThrow(() -> new HistoryNotFoundException(
                        message.getProperty("history.creation", templateId)
                ));
    }

    public TemplateHistoryResponse get(Long clientId, Long historyId) {
        return templateHistoryRepository.findByIdAndClientId(historyId, clientId)
                .map(mapper::mapToTemplateHistoryResponse)
                .orElseThrow(() -> new HistoryNotFoundException(
                        message.getProperty("history.not_found", historyId, clientId)
                ));
    }
}
