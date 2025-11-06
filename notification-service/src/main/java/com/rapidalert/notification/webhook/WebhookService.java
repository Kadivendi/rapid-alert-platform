package com.rapidalert.notification.webhook;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Outbound webhook dispatcher for third-party integration.
 *
 * Allows external systems to register webhook URLs and receive
 * notification events in real-time. Each payload is signed with
 * HMAC-SHA256 for authenticity verification.
 */
@Service
public class WebhookService {

    private static final Logger log = LoggerFactory.getLogger(WebhookService.class);
    private static final int MAX_RETRIES = 3;

    private final Map<String, WebhookRegistration> registrations = new ConcurrentHashMap<>();
    private final List<WebhookDeliveryRecord> deliveryLog = new CopyOnWriteArrayList<>();

    /**
     * Registers a new webhook endpoint.
     */
    public String register(String url, String secret, Set<String> eventTypes) {
        String id = UUID.randomUUID().toString().substring(0, 8);
        WebhookRegistration reg = new WebhookRegistration(id, url, secret, eventTypes);
        registrations.put(id, reg);
        log.info("Webhook registered: id={}, url={}, events={}", id, url, eventTypes);
        return id;
    }

    /**
     * Dispatches an event to all matching webhook registrations.
     */
    public void dispatch(String eventType, Map<String, Object> payload) {
        registrations.values().stream()
                .filter(r -> r.eventTypes.contains(eventType) || r.eventTypes.contains("*"))
                .forEach(reg -> deliverWithRetry(reg, eventType, payload));
    }

    /**
     * Signs a payload with HMAC-SHA256 using the webhook's secret.
     */
    public String signPayload(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(
                    secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(keySpec);
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("HMAC-SHA256 signing failed", e);
        }
    }

    /**
     * Returns delivery history for audit and debugging.
     */
    public List<WebhookDeliveryRecord> getDeliveryLog(int limit) {
        int size = deliveryLog.size();
        int from = Math.max(0, size - limit);
        return deliveryLog.subList(from, size);
    }

    /**
     * Removes a webhook registration.
     */
    public boolean unregister(String id) {
        WebhookRegistration removed = registrations.remove(id);
        if (removed != null) {
            log.info("Webhook unregistered: id={}", id);
            return true;
        }
        return false;
    }

    private void deliverWithRetry(WebhookRegistration reg, String eventType,
                                   Map<String, Object> payload) {
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                // In production, this would use RestTemplate/WebClient
                String signature = signPayload(payload.toString(), reg.secret);
                WebhookDeliveryRecord record = new WebhookDeliveryRecord(
                        reg.id, reg.url, eventType, attempt, "SUCCESS", Instant.now());
                deliveryLog.add(record);
                log.info("Webhook delivered: id={}, url={}, event={}, attempt={}",
                        reg.id, reg.url, eventType, attempt);
                return;
            } catch (Exception e) {
                long backoffMs = (long) Math.pow(2, attempt) * 1000;
                log.warn("Webhook delivery failed: id={}, attempt={}, backoff={}ms, error={}",
                        reg.id, attempt, backoffMs, e.getMessage());
                if (attempt == MAX_RETRIES) {
                    deliveryLog.add(new WebhookDeliveryRecord(
                            reg.id, reg.url, eventType, attempt, "FAILED", Instant.now()));
                }
            }
        }
    }

    /** Webhook registration record. */
    private record WebhookRegistration(String id, String url, String secret, Set<String> eventTypes) {}

    /** Webhook delivery attempt record for audit trail. */
    public record WebhookDeliveryRecord(String webhookId, String url, String eventType,
                                         int attempt, String status, Instant timestamp) {}
}
