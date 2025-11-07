package com.rapidalert.notification.webhook;

import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * REST surface for the outbound webhook registry. Every lifecycle event from
 * {@link com.rapidalert.notification.service.NotificationService} is dispatched
 * to subscribed URLs by {@link WebhookService}.
 */
@RestController
@RequestMapping("/api/v1/webhooks")
public class WebhookController {

    private final WebhookService webhookService;

    public WebhookController(WebhookService webhookService) {
        this.webhookService = webhookService;
    }

    public record RegisterRequest(String url, String secret, Set<String> eventTypes) {
    }

    @PostMapping
    @Operation(summary = "Register a new webhook endpoint.")
    public ResponseEntity<Map<String, String>> register(@RequestBody RegisterRequest req) {
        String id = webhookService.register(
                req.url(),
                req.secret(),
                req.eventTypes() != null && !req.eventTypes().isEmpty() ? req.eventTypes() : Set.of("*")
        );
        return ResponseEntity.ok(Map.of("id", id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove a webhook registration.")
    public ResponseEntity<Map<String, Boolean>> unregister(@PathVariable String id) {
        return ResponseEntity.ok(Map.of("removed", webhookService.unregister(id)));
    }

    @GetMapping("/deliveries")
    @Operation(summary = "List recent webhook delivery attempts.")
    public ResponseEntity<List<WebhookService.WebhookDeliveryRecord>> deliveries(
            @RequestParam(defaultValue = "100") int limit
    ) {
        return ResponseEntity.ok(webhookService.getDeliveryLog(limit));
    }
}
