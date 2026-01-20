package com.rapidalert.notification.health;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * REST controller exposing deep health check endpoints for
 * infrastructure monitoring and Kubernetes readiness probes.
 */
@RestController
@RequestMapping("/api/v1/health")
public class HealthCheckController {

    private final DeepHealthCheckService healthCheckService;

    public HealthCheckController(DeepHealthCheckService healthCheckService) {
        this.healthCheckService = healthCheckService;
    }

    @GetMapping("/deep")
    public ResponseEntity<Map<String, Object>> deepHealthCheck() {
        Map<String, Object> report = healthCheckService.performDeepCheck();
        String status = (String) report.get("status");
        if ("UP".equals(status)) {
            return ResponseEntity.ok(report);
        }
        return ResponseEntity.status(503).body(report);
    }

    @GetMapping("/liveness")
    public ResponseEntity<Map<String, String>> liveness() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }
}
