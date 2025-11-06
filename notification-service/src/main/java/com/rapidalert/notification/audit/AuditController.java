package com.rapidalert.notification.audit;

import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

/**
 * Read-only view of the notification audit trail.
 *
 * Routed by the API gateway at {@code /api/v1/audit/**}; every event written
 * by {@link AuditService} is queryable here for compliance and debugging.
 */
@RestController
@RequestMapping("/api/v1/audit")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping("/events")
    @Operation(summary = "Query audit events with optional filters.")
    public ResponseEntity<List<AuditEvent>> queryEvents(
            @RequestParam(required = false) String notificationId,
            @RequestParam(required = false) AuditEvent.EventType eventType,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "100") int limit
    ) {
        Instant fromInstant = from != null && !from.isBlank() ? Instant.parse(from) : null;
        Instant toInstant = to != null && !to.isBlank() ? Instant.parse(to) : null;
        return ResponseEntity.ok(
                auditService.query(notificationId, eventType, fromInstant, toInstant, limit)
        );
    }

    @GetMapping("/counts")
    @Operation(summary = "Return total counts by event type.")
    public ResponseEntity<?> counts() {
        return ResponseEntity.ok(auditService.getEventCounts());
    }
}
