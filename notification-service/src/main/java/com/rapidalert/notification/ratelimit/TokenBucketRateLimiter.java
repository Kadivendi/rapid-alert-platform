package com.rapidalert.notification.ratelimit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe token bucket rate limiter for per-channel notification throttling.
 *
 * Each channel (telegram, sms, push, email) has its own bucket with configurable
 * capacity and refill rate. This prevents overwhelming downstream providers
 * during mass alert events while ensuring fair access across channels.
 *
 * The token bucket algorithm is chosen over sliding window because:
 * - It naturally handles burst traffic (initial capacity = burst size)
 * - Constant memory regardless of request volume
 * - O(1) per check with no cleanup needed
 */
@Component
public class TokenBucketRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(TokenBucketRateLimiter.class);

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    /** Default channel limits (tokens per second) */
    private static final Map<String, Double> DEFAULT_RATES = Map.of(
            "telegram", 30.0,
            "sms", 100.0,
            "push", 500.0,
            "email", 50.0,
            "mesh", 10.0
    );

    /** Default burst capacity (max tokens stored) */
    private static final Map<String, Integer> DEFAULT_CAPACITY = Map.of(
            "telegram", 50,
            "sms", 200,
            "push", 1000,
            "email", 100,
            "mesh", 20
    );

    /**
     * Attempts to acquire a token for the given channel.
     *
     * @param channel the delivery channel (e.g., "telegram", "sms")
     * @return true if the request is allowed, false if rate limited
     */
    public boolean tryAcquire(String channel) {
        Bucket bucket = buckets.computeIfAbsent(channel, this::createBucket);
        boolean allowed = bucket.tryConsume();
        if (!allowed) {
            log.warn("Rate limited: channel={}, available={}", channel, bucket.getAvailableTokens());
        }
        return allowed;
    }

    /**
     * Returns the current number of available tokens for a channel.
     */
    public double getAvailableTokens(String channel) {
        Bucket bucket = buckets.get(channel);
        return bucket != null ? bucket.getAvailableTokens() : 0;
    }

    /**
     * Resets the bucket for a channel (useful for testing and admin overrides).
     */
    public void resetBucket(String channel) {
        buckets.remove(channel);
        log.info("Rate limiter bucket reset for channel={}", channel);
    }

    private Bucket createBucket(String channel) {
        double rate = DEFAULT_RATES.getOrDefault(channel, 10.0);
        int capacity = DEFAULT_CAPACITY.getOrDefault(channel, 20);
        return new Bucket(capacity, rate);
    }

    /**
     * Internal bucket implementation with atomic token management.
     * Tokens refill continuously based on elapsed time since last access.
     */
    private static class Bucket {
        private final int capacity;
        private final double refillRatePerSecond;
        private double tokens;
        private Instant lastRefill;

        Bucket(int capacity, double refillRatePerSecond) {
            this.capacity = capacity;
            this.refillRatePerSecond = refillRatePerSecond;
            this.tokens = capacity;  // Start full
            this.lastRefill = Instant.now();
        }

        synchronized boolean tryConsume() {
            refill();
            if (tokens >= 1.0) {
                tokens -= 1.0;
                return true;
            }
            return false;
        }

        synchronized double getAvailableTokens() {
            refill();
            return tokens;
        }

        private void refill() {
            Instant now = Instant.now();
            double elapsed = (now.toEpochMilli() - lastRefill.toEpochMilli()) / 1000.0;
            tokens = Math.min(capacity, tokens + elapsed * refillRatePerSecond);
            lastRefill = now;
        }
    }
}
