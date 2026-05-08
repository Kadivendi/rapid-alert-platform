package com.rapidalert.notification.service;

import com.rapidalert.notification.client.ShortenerClient;
import com.rapidalert.notification.client.TemplateClient;
import com.rapidalert.notification.dto.kafka.NotificationKafka;
import com.rapidalert.notification.dto.kafka.RecipientListKafka;
import com.rapidalert.notification.dto.request.NotificationRequest;
import com.rapidalert.notification.dto.response.*;
import com.rapidalert.notification.entity.Notification;
import com.rapidalert.notification.exception.notification.NotificationNotFoundException;
import com.rapidalert.notification.exception.template.TemplateRecipientsNotFound;
import com.rapidalert.notification.mapper.NotificationMapper;
import com.rapidalert.notification.model.NotificationStatus;
import com.rapidalert.notification.repository.NotificationHistoryRepository;
import com.rapidalert.notification.repository.NotificationRepository;
import com.rapidalert.notification.util.CollectionUtils;
import com.rapidalert.notification.util.NodeChecker;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.example.notification.model.NotificationStatus.*;
import static java.time.temporal.ChronoUnit.SECONDS;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final KafkaTemplate<String, RecipientListKafka> kafkaTemplate;
    private final NotificationHistoryRepository notificationHistoryRepository;
    private final NotificationRepository notificationRepository;
    private final TemplateClient templateClient;
    private final ShortenerClient shortenerClient;
    private final MessageSourceService message;
    private final NotificationMapper mapper;
    private final NodeChecker nodeChecker;

    @Value("${spring.application.name}")
    private String applicationName;

    @Value("${spring.kafka.topics.splitter}")
    private String recipientListDistributionTopic;

    public String distributeNotifications(Long clientId, Long templateId) { // TODO: separate service
        TemplateResponse templateResponse = templateClient.getTemplateByClientIdAndTemplateId(clientId, templateId) // TODO: exception handling
                .getBody(); // TODO: retrieve list of IDS at once, not TemplateResponse

        if (templateResponse == null) {
            return "TODO"; // TODO
        }

        List<Long> recipientIds = templateResponse.recipientIds()
                .stream()
                .map(RecipientResponse::id)
                .toList();
        if (recipientIds.size() == 0) {
            throw new TemplateRecipientsNotFound(
                    message.getProperty("template.recipients.not_found", templateId, clientId)
            );
        }

        TemplateHistoryResponse templateHistoryResponse = templateClient.createTemplateHistory(clientId, templateId)
                .getBody();

        for (List<Long> recipients : splitRecipientIds(recipientIds)) {
            RecipientListKafka listKafka = new RecipientListKafka(recipients, templateHistoryResponse, clientId);
            kafkaTemplate.send(recipientListDistributionTopic, listKafka);
        }

        return "Notification's been successfully sent!";
    }

    public NotificationResponse createNotification(NotificationRequest request) {
        return Optional.of(request)
                .map(mapper::mapToEntity)
                .map(notification -> notification.addTemplateHistory(request.template().id()))
                .map(notificationRepository::saveAndFlush)
                .map(notification -> mapper.mapToResponse(notification, templateClient))
                .orElseThrow(); // TODO
    }

    public List<NotificationKafka> getNotificationsForRebalancing(Long pendingSec, Long newSec, Integer size) {
        LocalDateTime now = LocalDateTime.now();
        return notificationRepository.findNotificationsByStatusAndCreatedAt(
                        now.minus(pendingSec, SECONDS), now.minus(newSec, SECONDS), Pageable.ofSize(size)
                ).stream()
                .map(notification -> notification.setNotificationStatus(PENDING))
                .map(Notification::updateCreatedAt)
                .map(notificationRepository::saveAndFlush)
                .map(notification -> mapper.mapToKafka(notification, templateClient, shortenerClient))
                .toList();
    }

    public NotificationHistoryResponse setNotificationAsSent(Long clientId, Long notificationId) {
        return setNotificationAsExecutedWithGivenStatus(clientId, notificationId, SENT);
    }

    public NotificationHistoryResponse setNotificationAsError(Long clientId, Long notificationId) {
        return setNotificationAsExecutedWithGivenStatus(clientId, notificationId, ERROR);
    }

    public NotificationHistoryResponse setNotificationAsCorrupt(Long clientId, Long notificationId) {
        return setNotificationAsExecutedWithGivenStatus(clientId, notificationId, CORRUPT);
    }

    public NotificationResponse setNotificationAsPending(Long clientId, Long notificationId) {
        return notificationRepository.findByIdAndClientId(notificationId, clientId)
                .map(notification -> notification.setNotificationStatus(PENDING))
                .map(notificationRepository::saveAndFlush)
                .map(notification -> mapper.mapToResponse(notification, templateClient))
                .orElseThrow(() -> new NotificationNotFoundException(
                        message.getProperty("notification.not_found", notificationId, clientId)
                ));
    }

    public NotificationResponse setNotificationAsResending(Long clientId, Long notificationId) {
        return notificationRepository.findByIdAndClientId(notificationId, clientId)
                .map(Notification::incrementRetryAttempts)
                .map(notification -> notification.setNotificationStatus(RESENDING))
                .map(notificationRepository::saveAndFlush)
                .map(notification -> mapper.mapToResponse(notification, templateClient))
                .orElseThrow(() -> new NotificationNotFoundException(
                        message.getProperty("notification.not_found", notificationId, clientId)
                ));
    }

    private NotificationHistoryResponse setNotificationAsExecutedWithGivenStatus(
            Long clientId, Long notificationId,
            NotificationStatus status
    ) {
        return notificationRepository.findByIdAndClientId(notificationId, clientId)
                .map(notification -> {
                    notificationRepository.delete(notification);
                    return notification;
                })
                .map(mapper::mapToHistory)
                .map(notificationHistory -> notificationHistory.setNotificationStatus(status))
                .map(notificationHistoryRepository::saveAndFlush)
                .map(mapper::mapToResponse)
                .orElseThrow(() -> new NotificationNotFoundException(
                        message.getProperty("notification.not_found", notificationId, clientId)
                ));
    }

    private List<List<Long>> splitRecipientIds(List<Long> list) {
        return CollectionUtils.splitList(list, nodeChecker.getAmountOfRunningNodes(applicationName));
    }
}
