package com.rapidalert.recipient.service;

import com.rapidalert.recipient.dto.request.RecipientRequest;
import com.rapidalert.recipient.dto.response.RecipientResponse;
import com.rapidalert.recipient.entity.Recipient;
import com.rapidalert.recipient.entity.TemplateId;
import com.rapidalert.recipient.exception.recipient.RecipientNotFoundException;
import com.rapidalert.recipient.exception.recipient.RecipientRegistrationException;
import com.rapidalert.recipient.mapper.RecipientMapper;
import com.rapidalert.recipient.repository.RecipientRepository;
import com.rapidalert.recipient.repository.TemplateIdRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RecipientService {

    private final RecipientRepository recipientRepository;
    private final TemplateIdRepository templateIdRepository;
    private final MessageSourceService message;
    private final RecipientMapper mapper;

    public RecipientResponse register(Long clientId, RecipientRequest request) {
        Optional<Recipient> existing = recipientRepository.findByEmailAndClientId(request.email(), clientId);
        if (existing.isPresent()) {
            return update(clientId, existing.get().getId(), request);
        }

        try {
            return Optional.of(request)
                    .map(mapper::mapToEntity)
                    .map(recipient -> recipient.addClient(clientId))
                    .map(recipientRepository::save)
                    .map(mapper::mapToResponse)
                    .orElseThrow(() -> new RecipientRegistrationException(
                            message.getProperty("recipient.registration", request.email())
                    ));
        } catch (DataIntegrityViolationException e) {
            throw new RecipientRegistrationException(e.getMessage());
        }
    }

    public RecipientResponse receive(Long clientId, Long recipientId) {
        return recipientRepository.findByIdAndClientId(recipientId, clientId)
                .map(mapper::mapToResponse)
                .orElseThrow(() -> new RecipientNotFoundException(
                        message.getProperty("recipient.not_found", recipientId)
                ));
    }

    public Boolean delete(Long clientId, Long recipientId) {
        return recipientRepository.findByIdAndClientId(recipientId, clientId)
                .map(recipient -> {
                    recipientRepository.delete(recipient);
                    return recipient;
                })
                .isPresent();
    }

    public RecipientResponse update(Long clientId, Long recipientId, RecipientRequest request) {
        try {
            return recipientRepository.findByIdAndClientId(recipientId, clientId)
                    .map(recipient -> mapper.update(request, recipient))
                    .map(recipientRepository::saveAndFlush)
                    .map(mapper::mapToResponse)
                    .orElseThrow(() -> new RecipientNotFoundException(
                            message.getProperty("recipient.not_found", recipientId)
                    ));
        } catch (DataIntegrityViolationException e) {
            throw new RecipientRegistrationException(e.getMessage());
        }
    }

    public List<RecipientResponse> receiveByTemplate(Long clientId, Long templateId) {
        return templateIdRepository.findAllByRecipient_clientIdAndTemplateId(clientId, templateId)
                .stream()
                .map(TemplateId::getRecipient)
                .map(mapper::mapToResponse)
                .toList();
    }

    public List<RecipientResponse> receiveByClient(Long clientId) {
        return recipientRepository.findAllByClientId(clientId)
                .stream()
                .map(mapper::mapToResponse)
                .toList();
    }

    public List<RecipientResponse> receiveByPolygon(Long clientId, List<com.rapidalert.recipient.entity.Geolocation> polygon) {
        return recipientRepository.findAllByClientId(clientId)
                .stream()
                .filter(recipient -> recipient.getGeolocation() != null)
                .filter(recipient -> com.rapidalert.recipient.utils.PolygonUtils.isPointInPolygon(recipient.getGeolocation(), polygon))
                .map(mapper::mapToResponse)
                .toList();
    }
}
