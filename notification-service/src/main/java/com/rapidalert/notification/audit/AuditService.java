package com.rapidalert.notification.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Service for recording and querying notification lifecycle audit events.
 * Provides a complete audit trail for compliance, debugging, and analytics.
 */
@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);
    private final List<AuditEvent> auditLog = new CopyOnWriteArrayList<>();

    /**
     * Records a new audit event for a notification lifecycle transition.
     */
    public void record(AuditEvent event) {
        auditLog.add(event);
        log.info("Audit: notification={} event={} channel={} service={}",
                event.getNotificationId(), event.getEventType(),
                event.getChannel(), event.getActorService());
    }

    /**
     * Queries audit events with optional filters.
     */
    public List<AuditEvent> query(String notificationId, AuditEvent.EventType eventType,
                                   Instant from, Instant to, int limit) {
        Stream<AuditEvent> stream = auditLog.stream();

        if (notificationId != null) {
            stream = stream.filter(e -> notificationId.equals(e.getNotificationId()));
        }
        if (eventType != null) {
            stream = stream.filter(e -> eventType.equals(e.getEventType()));
        }
        if (from != null) {
            stream = stream.filter(e -> e.getTimestamp().isAfter(from));
        }
        if (to != null) {
            stream = stream.filter(e -> e.getTimestamp().isBefore(to));
        }

        return stream
                .sorted(Comparator.comparing(AuditEvent::getTimestamp).reversed())
                .limit(limit > 0 ? limit : 100)
                .collect(Collectors.toList());
    }

    /**
     * Returns the total count of events by type for dashboard display.
     */
    public java.util.Map<AuditEvent.EventType, Long> getEventCounts() {
        return auditLog.stream()
                .collect(Collectors.groupingBy(AuditEvent::getEventType, Collectors.counting()));
    }
}
