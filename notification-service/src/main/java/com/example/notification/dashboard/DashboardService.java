package com.rapidalert.notification.dashboard;

import com.rapidalert.notification.audit.AuditEvent;
import com.rapidalert.notification.audit.AuditService;
import com.rapidalert.notification.metrics.NotificationMetricsService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Dashboard service providing aggregated analytics for the notification
 * monitoring dashboard. Combines data from metrics, audit, and delivery
 * services into dashboard-ready summaries.
 */
@Service
public class DashboardService {

    private final NotificationMetricsService metricsService;
    private final AuditService auditService;

    public DashboardService(NotificationMetricsService metricsService,
                            AuditService auditService) {
        this.metricsService = metricsService;
        this.auditService = auditService;
    }

    /**
     * Returns a summary of notification statistics for the last N hours.
     */
    public Map<String, Object> getSummary(int hours) {
        Map<String, Object> summary = new LinkedHashMap<>();
        Instant since = Instant.now().minus(hours, ChronoUnit.HOURS);

        Map<AuditEvent.EventType, Long> counts = auditService.getEventCounts();
        long totalSent = counts.getOrDefault(AuditEvent.EventType.SENT, 0L);
        long totalDelivered = counts.getOrDefault(AuditEvent.EventType.DELIVERED, 0L);
        long totalFailed = counts.getOrDefault(AuditEvent.EventType.FAILED, 0L);

        summary.put("period_hours", hours);
        summary.put("total_sent", totalSent);
        summary.put("total_delivered", totalDelivered);
        summary.put("total_failed", totalFailed);
        summary.put("success_rate", totalSent > 0
                ? Math.round((double) totalDelivered / totalSent * 10000) / 100.0 : 0);

        // Channel breakdown
        Map<String, Object> channels = new LinkedHashMap<>();
        for (String ch : List.of("telegram", "sms", "push", "email", "mesh")) {
            Map<String, Object> chStats = new LinkedHashMap<>();
            chStats.put("avg_latency_ms", metricsService.getAverageLatency(ch));
            channels.put(ch, chStats);
        }
        summary.put("channels", channels);
        summary.put("generated_at", Instant.now().toString());

        return summary;
    }
}
