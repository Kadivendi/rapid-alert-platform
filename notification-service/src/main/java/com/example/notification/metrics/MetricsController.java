package com.rapidalert.notification.metrics;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * REST controller exposing Prometheus-compatible metrics endpoint
 * for notification delivery monitoring and alerting dashboards.
 */
@RestController
@RequestMapping("/api/v1")
public class MetricsController {

    private final NotificationMetricsService metricsService;

    public MetricsController(NotificationMetricsService metricsService) {
        this.metricsService = metricsService;
    }

    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> getMetrics() {
        return ResponseEntity.ok(metricsService.getMetricsSnapshot());
    }
}
