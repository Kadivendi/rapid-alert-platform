package com.rapidalert.notification.triage;

import com.rapidalert.notification.audit.AuditEvent;
import com.rapidalert.notification.audit.AuditService;
import com.rapidalert.notification.metrics.NotificationMetricsService;
import com.rapidalert.notification.webhook.WebhookService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Mesh failover signal.
 *
 * Watches the rolling delivery-success rate; when it drops below
 * {@code mesh.failover.threshold} (default 0.80) it POSTs the affected event to
 * {@code resilient-mesh-gateway} so the alert is rebroadcast over BLE / WiFi
 * Direct / LoRa. Also fires on any triage event that explicitly recommends the
 * {@code mesh_gateway} channel or carries an escalation prediction.
 */
@Slf4j
@Component
public class MeshFailoverDispatcher {

    private final AuditService auditService;
    private final WebhookService webhookService;
    private final NotificationMetricsService metrics;
    private final WebClient meshClient;

    @Value("${mesh.failover.threshold:0.80}")
    private double threshold;

    @Value("${mesh.failover.endpoint:http://resilient-mesh-gateway:8090/api/mesh/broadcast}")
    private String endpoint;

    private final Queue<Boolean> recent = new LinkedList<>();
    private final AtomicLong dispatched = new AtomicLong();
    private volatile Instant lastDispatch;

    public MeshFailoverDispatcher(AuditService auditService,
                                  WebhookService webhookService,
                                  NotificationMetricsService metrics) {
        this.auditService = auditService;
        this.webhookService = webhookService;
        this.metrics = metrics;
        this.meshClient = WebClient.builder()
                .baseUrl("http://localhost") // overridden per-call with absolute endpoint
                .build();
    }

    public synchronized void recordOutcome(boolean delivered) {
        recent.add(delivered);
        while (recent.size() > 50) recent.poll();
    }

    public synchronized double currentDeliveryRate() {
        if (recent.isEmpty()) return 1.0;
        long ok = recent.stream().filter(Boolean::booleanValue).count();
        return ok / (double) recent.size();
    }

    public boolean shouldFailover() {
        return recent.size() >= 10 && currentDeliveryRate() < threshold;
    }

    public void dispatch(TriageEvent event) {
        try {
            meshClient.post()
                    .uri(endpoint)
                    .bodyValue(Map.of(
                            "event_id", event.event_id(),
                            "severity", String.valueOf(event.severity()),
                            "risk_score", event.risk_score() != null ? event.risk_score() : 0.0,
                            "source", "rapid-alert-platform",
                            "reason", shouldFailover()
                                    ? "delivery_rate_below_threshold"
                                    : "triage_escalation"
                    ))
                    .retrieve()
                    .toBodilessEntity()
                    .timeout(Duration.ofSeconds(5))
                    .doOnNext(resp -> onDispatchAck(event, resp.getStatusCode()))
                    .doOnError(err -> log.warn(
                            "Mesh failover dispatch failed for event={}: {}",
                            event.event_id(), err.getMessage()))
                    .subscribe();
        } catch (Exception e) {
            log.warn("Failed to dispatch mesh failover for event={}", event.event_id(), e);
        }
    }

    private void onDispatchAck(TriageEvent event, HttpStatusCode status) {
        dispatched.incrementAndGet();
        lastDispatch = Instant.now();
        metrics.recordSent("mesh");
        auditService.record(AuditEvent.builder()
                .notificationId(event.event_id())
                .eventType(AuditEvent.EventType.SENT)
                .channel("mesh")
                .actorService("notification-service")
                .metadata(Map.of("status_code", String.valueOf(status.value())))
                .build());
        webhookService.dispatch("mesh.dispatched", Map.of(
                "event_id", event.event_id(),
                "status_code", status.value(),
                "delivery_rate", currentDeliveryRate()
        ));
        log.info("Mesh failover dispatched: event={} status={} rate={}",
                event.event_id(), status, currentDeliveryRate());
    }

    public long totalDispatches() {
        return dispatched.get();
    }

    public Instant lastDispatchAt() {
        return lastDispatch;
    }
}
