package com.rapidalert.notification.triage;

import com.rapidalert.notification.audit.AuditEvent;
import com.rapidalert.notification.audit.AuditService;
import com.rapidalert.notification.metrics.NotificationMetricsService;
import com.rapidalert.notification.webhook.WebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Consumes triage events published by {@code disaster-triage-engine}.
 *
 * Each event records an audit entry, fires the {@code triage.received} webhook
 * for any external listeners, and — when the triage flags
 * {@code escalation_likely} or recommends mesh delivery — triggers the
 * mesh-failover signal handled by {@link MeshFailoverDispatcher}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TriageEventConsumer {

    private final AuditService auditService;
    private final NotificationMetricsService metrics;
    private final WebhookService webhookService;
    private final MeshFailoverDispatcher meshFailoverDispatcher;

    @KafkaListener(
            topics = "${spring.kafka.topics.triage-events}",
            groupId = "rapid-alert-notification",
            containerFactory = "triageKafkaListenerContainerFactory"
    )
    public void onTriage(TriageEvent event) {
        if (event == null || event.event_id() == null) {
            log.warn("Skipping malformed triage event");
            return;
        }

        log.info("Triage event received: id={} severity={} confidence={} escalation={}",
                event.event_id(), event.severity(), event.confidence(), event.escalation_likely());

        auditService.record(AuditEvent.builder()
                .notificationId(event.event_id())
                .eventType(AuditEvent.EventType.CREATED)
                .actorService("disaster-triage-engine")
                .metadata(Map.of(
                        "severity", String.valueOf(event.severity()),
                        "confidence", String.valueOf(event.confidence()),
                        "risk_score", String.valueOf(event.risk_score())
                ))
                .build());

        metrics.recordSent("triage_intake");

        webhookService.dispatch("triage.received", Map.of(
                "event_id", event.event_id(),
                "severity", String.valueOf(event.severity()),
                "channels", event.recommended_channels() != null
                        ? event.recommended_channels() : java.util.List.of()
        ));

        if (event.shouldTriggerMeshFallback()) {
            meshFailoverDispatcher.dispatch(event);
        }
    }
}
