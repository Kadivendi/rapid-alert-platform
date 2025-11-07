package com.rapidalert.notification.triage;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Map;

/**
 * Inbound payload published by {@code disaster-triage-engine} on the
 * {@code rapid-alert.triage-events} Kafka topic. The schema mirrors the
 * {@code TriageResponse} pydantic model in that service so the two ends
 * stay in sync.
 *
 * <p>Unknown fields are tolerated so additions on the producer side don't
 * break this consumer.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TriageEvent(
        String event_id,
        String severity,
        Double confidence,
        Boolean escalation_likely,
        Double escalation_probability_3h,
        List<String> recommended_channels,
        Integer affected_population_estimate,
        Double risk_score,
        Map<String, Object> meta
) {
    public boolean shouldTriggerMeshFallback() {
        return Boolean.TRUE.equals(escalation_likely)
                || (recommended_channels != null && recommended_channels.contains("mesh_gateway"));
    }
}
