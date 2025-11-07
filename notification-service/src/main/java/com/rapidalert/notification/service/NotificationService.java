package com.rapidalert.notification.service;

import com.rapidalert.notification.audit.AuditEvent;
import com.rapidalert.notification.audit.AuditService;
import com.rapidalert.notification.client.ShortenerClient;
import com.rapidalert.notification.client.TemplateClient;
import com.rapidalert.notification.dedup.DeduplicationService;
import com.rapidalert.notification.dto.kafka.NotificationKafka;
import com.rapidalert.notification.dto.kafka.RecipientListKafka;
import com.rapidalert.notification.dto.request.NotificationRequest;
import com.rapidalert.notification.dto.response.*;
import com.rapidalert.notification.entity.Notification;
import com.rapidalert.notification.exception.notification.NotificationNotFoundException;
import com.rapidalert.notification.exception.template.TemplateRecipientsNotFound;
import com.rapidalert.notification.mapper.NotificationMapper;
import com.rapidalert.notification.metrics.NotificationMetricsService;
import com.rapidalert.notification.model.NotificationStatus;
import com.rapidalert.notification.repository.NotificationHistoryRepository;
import com.rapidalert.notification.repository.NotificationRepository;
import com.rapidalert.notification.util.CollectionUtils;
import com.rapidalert.notification.util.NodeChecker;
import com.rapidalert.notification.webhook.WebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.rapidalert.notification.model.NotificationStatus.*;
import static java.time.temporal.ChronoUnit.SECONDS;

/**
 * Orchestrates the full notification lifecycle.
 *
 * Every state transition is mirrored to:
 *  - {@link AuditService} (full trail for /api/v1/audit)
 *  - {@link NotificationMetricsService} (Prometheus snapshot at /api/v1/metrics)
 *  - {@link WebhookService} (third-party integration listeners)
 *
 * The dispatch path is guarded by {@link DeduplicationService} so the same
 * payload received from multiple sources within the dedup window only fans
 * out once.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final String ACTOR = "notification-service";

    private final KafkaTemplate<String, RecipientListKafka> kafkaTemplate;
    private final NotificationHistoryRepository notificationHistoryRepository;
    private final NotificationRepository notificationRepository;
    private final TemplateClient templateClient;
    private final ShortenerClient shortenerClient;
    private final MessageSourceService message;
    private final NotificationMapper mapper;
    private final NodeChecker nodeChecker;
    private final AuditService auditService;
    private final DeduplicationService deduplicationService;
    private final NotificationMetricsService metrics;
    private final WebhookService webhookService;

    @Value("${spring.application.name}")
    private String applicationName;

    @Value("${spring.kafka.topics.splitter}")
    private String recipientListDistributionTopic;

    public String distributeNotifications(Long clientId, Long templateId) {
        TemplateResponse templateResponse = Optional.ofNullable(
                templateClient.getTemplateByClientIdAndTemplateId(clientId, templateId).getBody()
        ).orElseThrow(() -> new TemplateRecipientsNotFound(
                message.getProperty("template.recipients.not_found", templateId, clientId)
        ));

        String dedupKey = clientId + ":" + templateId + ":" + templateResponse.title();
        if (deduplicationService.isDuplicate(dedupKey)) {
            audit(AuditEvent.EventType.DEDUPLICATED, String.valueOf(templateId), null,
                    Map.of("clientId", String.valueOf(clientId)));
            log.info("Notification suppressed by deduplication: client={} template={}", clientId, templateId);
            return "Notification suppressed: duplicate of a recent dispatch.";
        }

        List<Long> recipientIds = templateResponse.recipientIds()
                .stream()
                .map(RecipientResponse::id)
                .toList();

        if (recipientIds.isEmpty()) {
            throw new TemplateRecipientsNotFound(
                    message.getProperty("template.recipients.not_found", templateId, clientId)
            );
        }

        TemplateHistoryResponse templateHistoryResponse =
                templateClient.createTemplateHistory(clientId, templateId).getBody();

        int batches = 0;
        for (List<Long> recipients : splitRecipientIds(recipientIds)) {
            RecipientListKafka listKafka =
                    new RecipientListKafka(recipients, templateHistoryResponse, clientId);
            kafkaTemplate.send(recipientListDistributionTopic, listKafka);
            batches++;
        }

        audit(AuditEvent.EventType.QUEUED, String.valueOf(templateId), null,
                Map.of(
                        "clientId", String.valueOf(clientId),
                        "recipients", String.valueOf(recipientIds.size()),
                        "batches", String.valueOf(batches)
                ));
        metrics.setQueueDepth("distribution", batches);
        webhookService.dispatch("notification.queued", Map.of(
                "clientId", clientId,
                "templateId", templateId,
                "recipients", recipientIds.size(),
                "batches", batches
        ));

        return String.format(
                "Notification queued: %d recipients across %d batches.",
                recipientIds.size(), batches);
    }

    public NotificationResponse createNotification(NotificationRequest request) {
        NotificationResponse response = Optional.of(request)
                .map(mapper::mapToEntity)
                .map(notification -> notification.addTemplateHistory(request.template().id()))
                .map(notificationRepository::saveAndFlush)
                .map(notification -> mapper.mapToResponse(notification, templateClient))
                .orElseThrow(() -> new NotificationNotFoundException(
                        message.getProperty("notification.not_found", "-", "-")
                ));

        audit(AuditEvent.EventType.CREATED, String.valueOf(response.id()), null, Map.of());
        webhookService.dispatch("notification.created", Map.of("id", response.id()));
        return response;
    }

    public List<NotificationKafka> getNotificationsForRebalancing(Long pendingSec, Long newSec, Integer size) {
        LocalDateTime now = LocalDateTime.now();
        List<NotificationKafka> stuck = notificationRepository.findNotificationsByStatusAndCreatedAt(
                        now.minus(pendingSec, SECONDS), now.minus(newSec, SECONDS), Pageable.ofSize(size)
                ).stream()
                .map(notification -> notification.setNotificationStatus(PENDING))
                .map(Notification::updateCreatedAt)
                .map(notificationRepository::saveAndFlush)
                .map(notification -> mapper.mapToKafka(notification, templateClient, shortenerClient))
                .toList();

        if (!stuck.isEmpty()) {
            metrics.setQueueDepth("rebalancer", stuck.size());
            audit(AuditEvent.EventType.RETRIED, "rebalancer-batch", null,
                    Map.of("count", String.valueOf(stuck.size())));
        }
        return stuck;
    }

    public NotificationHistoryResponse setNotificationAsSent(Long clientId, Long notificationId) {
        NotificationHistoryResponse response =
                setNotificationAsExecutedWithGivenStatus(clientId, notificationId, SENT);
        audit(AuditEvent.EventType.SENT, String.valueOf(notificationId), channelOf(response),
                Map.of("clientId", String.valueOf(clientId)));
        metrics.recordSent(safe(channelOf(response)));
        webhookService.dispatch("notification.sent",
                Map.of("notificationId", notificationId, "clientId", clientId,
                        "channel", safe(channelOf(response))));
        return response;
    }

    public NotificationHistoryResponse setNotificationAsError(Long clientId, Long notificationId) {
        NotificationHistoryResponse response =
                setNotificationAsExecutedWithGivenStatus(clientId, notificationId, ERROR);
        audit(AuditEvent.EventType.FAILED, String.valueOf(notificationId), channelOf(response),
                Map.of("clientId", String.valueOf(clientId)));
        metrics.recordFailed(safe(channelOf(response)), "delivery_error");
        webhookService.dispatch("notification.failed",
                Map.of("notificationId", notificationId, "clientId", clientId,
                        "channel", safe(channelOf(response))));
        return response;
    }

    public NotificationHistoryResponse setNotificationAsCorrupt(Long clientId, Long notificationId) {
        NotificationHistoryResponse response =
                setNotificationAsExecutedWithGivenStatus(clientId, notificationId, CORRUPT);
        audit(AuditEvent.EventType.FAILED, String.valueOf(notificationId), channelOf(response),
                Map.of("clientId", String.valueOf(clientId), "reason", "corrupt"));
        metrics.recordFailed(safe(channelOf(response)), "corrupt_payload");
        return response;
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
        NotificationResponse response = notificationRepository.findByIdAndClientId(notificationId, clientId)
                .map(Notification::incrementRetryAttempts)
                .map(notification -> notification.setNotificationStatus(RESENDING))
                .map(notificationRepository::saveAndFlush)
                .map(notification -> mapper.mapToResponse(notification, templateClient))
                .orElseThrow(() -> new NotificationNotFoundException(
                        message.getProperty("notification.not_found", notificationId, clientId)
                ));
        audit(AuditEvent.EventType.RETRIED, String.valueOf(notificationId), null,
                Map.of("clientId", String.valueOf(clientId)));
        metrics.recordRetry("unknown", 1);
        return response;
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

    private void audit(AuditEvent.EventType type, String notificationId, String channel,
                       Map<String, String> metadata) {
        auditService.record(AuditEvent.builder()
                .notificationId(notificationId)
                .eventType(type)
                .channel(channel)
                .actorService(ACTOR)
                .metadata(metadata)
                .build());
    }

    private static String safe(String value) {
        return value != null ? value : "unknown";
    }

    private static String channelOf(NotificationHistoryResponse response) {
        return response != null && response.type() != null
                ? response.type().name().toLowerCase()
                : "unknown";
    }
}
