package com.rapidalert.notification.metrics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Prometheus-compatible metrics service for tracking notification delivery
 * performance across all channels. Exposes counters, histograms, and gauges
 * for real-time monitoring and alerting.
 *
 * Metrics are organized by channel (telegram, sms, push, email) and status
 * (sent, delivered, failed, retried) for granular observability.
 */
@Service
public class NotificationMetricsService {

    private static final Logger log = LoggerFactory.getLogger(NotificationMetricsService.class);

    private final Map<String, AtomicLong> counters = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> latencySum = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> latencyCount = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> gauges = new ConcurrentHashMap<>();

    /**
     * Records a notification send attempt for a specific channel.
     */
    public void recordSent(String channel) {
        increment("notifications_sent_total", channel);
        log.trace("Recorded sent metric for channel={}", channel);
    }

    /**
     * Records a successful delivery confirmation.
     */
    public void recordDelivered(String channel) {
        increment("notifications_delivered_total", channel);
    }

    /**
     * Records a delivery failure with the error category.
     */
    public void recordFailed(String channel, String errorType) {
        increment("notifications_failed_total", channel);
        increment("notifications_errors_by_type", errorType);
    }

    /**
     * Records a retry attempt for a failed notification.
     */
    public void recordRetry(String channel, int attemptNumber) {
        increment("notifications_retried_total", channel);
        log.debug("Retry attempt {} for channel={}", attemptNumber, channel);
    }

    /**
     * Records delivery latency in milliseconds for histogram computation.
     */
    public void recordLatency(String channel, long latencyMs) {
        String sumKey = "delivery_latency_ms_sum:" + channel;
        String countKey = "delivery_latency_ms_count:" + channel;
        latencySum.computeIfAbsent(sumKey, k -> new AtomicLong(0)).addAndGet(latencyMs);
        latencyCount.computeIfAbsent(countKey, k -> new AtomicLong(0)).incrementAndGet();
    }

    /**
     * Sets the current gauge value for active Kafka consumers.
     */
    public void setActiveConsumers(int count) {
        gauges.computeIfAbsent("kafka_active_consumers", k -> new AtomicLong(0)).set(count);
    }

    /**
     * Sets the current queue depth for a specific channel.
     */
    public void setQueueDepth(String channel, long depth) {
        gauges.computeIfAbsent("queue_depth:" + channel, k -> new AtomicLong(0)).set(depth);
    }

    /**
     * Returns a snapshot of all metrics for the /metrics endpoint.
     */
    public Map<String, Object> getMetricsSnapshot() {
        Map<String, Object> snapshot = new ConcurrentHashMap<>();
        counters.forEach((k, v) -> snapshot.put(k, v.get()));
        latencySum.forEach((k, v) -> snapshot.put(k, v.get()));
        latencyCount.forEach((k, v) -> snapshot.put(k, v.get()));
        gauges.forEach((k, v) -> snapshot.put(k, v.get()));
        return snapshot;
    }

    /**
     * Returns the average delivery latency for a channel in milliseconds.
     */
    public double getAverageLatency(String channel) {
        long sum = latencySum.getOrDefault("delivery_latency_ms_sum:" + channel,
                new AtomicLong(0)).get();
        long count = latencyCount.getOrDefault("delivery_latency_ms_count:" + channel,
                new AtomicLong(0)).get();
        return count > 0 ? (double) sum / count : 0.0;
    }

    private void increment(String metric, String label) {
        String key = metric + ":" + label;
        counters.computeIfAbsent(key, k -> new AtomicLong(0)).incrementAndGet();
    }
}
