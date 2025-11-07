package com.rapidalert.notification.audit;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Immutable audit event recording a single lifecycle event for a notification.
 * Every state transition (created, queued, sent, delivered, failed, retried)
 * generates an AuditEvent for full traceability.
 */
public class AuditEvent {

    public enum EventType {
        CREATED, QUEUED, SENT, DELIVERED, FAILED, RETRIED, EXPIRED, DEDUPLICATED
    }

    private final String id;
    private final String notificationId;
    private final EventType eventType;
    private final String channel;
    private final Instant timestamp;
    private final String actorService;
    private final Map<String, String> metadata;
    private final String errorMessage;

    private AuditEvent(Builder builder) {
        this.id = UUID.randomUUID().toString();
        this.notificationId = builder.notificationId;
        this.eventType = builder.eventType;
        this.channel = builder.channel;
        this.timestamp = Instant.now();
        this.actorService = builder.actorService;
        this.metadata = builder.metadata != null ? Map.copyOf(builder.metadata) : Map.of();
        this.errorMessage = builder.errorMessage;
    }

    public String getId() { return id; }
    public String getNotificationId() { return notificationId; }
    public EventType getEventType() { return eventType; }
    public String getChannel() { return channel; }
    public Instant getTimestamp() { return timestamp; }
    public String getActorService() { return actorService; }
    public Map<String, String> getMetadata() { return metadata; }
    public String getErrorMessage() { return errorMessage; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String notificationId;
        private EventType eventType;
        private String channel;
        private String actorService;
        private Map<String, String> metadata;
        private String errorMessage;

        public Builder notificationId(String id) { this.notificationId = id; return this; }
        public Builder eventType(EventType type) { this.eventType = type; return this; }
        public Builder channel(String channel) { this.channel = channel; return this; }
        public Builder actorService(String service) { this.actorService = service; return this; }
        public Builder metadata(Map<String, String> meta) { this.metadata = meta; return this; }
        public Builder errorMessage(String error) { this.errorMessage = error; return this; }
        public AuditEvent build() { return new AuditEvent(this); }
    }
}
