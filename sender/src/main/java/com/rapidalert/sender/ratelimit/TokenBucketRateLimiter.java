package com.rapidalert.sender.ratelimit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Token bucket throttle protecting downstream providers (Telegram, Twilio, SMTP)
 * from being overwhelmed during mass alert dispatch.
 *
 * One bucket per channel, configured via {@code DEFAULT_RATES} / {@code DEFAULT_CAPACITY}
 * below. {@link #acquire(String)} blocks the calling Kafka listener thread until a token
 * is available — this is what we want: it slows down the consumer to the channel rate.
 */
@Component
public class TokenBucketRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(TokenBucketRateLimiter.class);

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    private static final Map<String, Double> DEFAULT_RATES = Map.of(
            "telegram", 30.0,
            "sms", 100.0,
            "push", 500.0,
            "email", 50.0,
            "mesh", 10.0
    );

    private static final Map<String, Integer> DEFAULT_CAPACITY = Map.of(
            "telegram", 50,
            "sms", 200,
            "push", 1000,
            "email", 100,
            "mesh", 20
    );

    public boolean tryAcquire(String channel) {
        return bucket(channel).tryConsume();
    }

    public void acquire(String channel) throws InterruptedException {
        Bucket b = bucket(channel);
        while (!b.tryConsume()) {
            long waitMs = b.estimatedMillisUntilNextToken();
            log.debug("Rate limit hit for channel={}, sleeping {}ms", channel, waitMs);
            Thread.sleep(Math.max(5, Math.min(waitMs, 1000)));
        }
    }

    public double getAvailableTokens(String channel) {
        return bucket(channel).getAvailableTokens();
    }

    public void resetBucket(String channel) {
        buckets.remove(channel);
    }

    private Bucket bucket(String channel) {
        return buckets.computeIfAbsent(channel, this::createBucket);
    }

    private Bucket createBucket(String channel) {
        double rate = DEFAULT_RATES.getOrDefault(channel, 10.0);
        int capacity = DEFAULT_CAPACITY.getOrDefault(channel, 20);
        return new Bucket(capacity, rate);
    }

    private static class Bucket {
        private final int capacity;
        private final double refillRatePerSecond;
        private double tokens;
        private Instant lastRefill;

        Bucket(int capacity, double refillRatePerSecond) {
            this.capacity = capacity;
            this.refillRatePerSecond = refillRatePerSecond;
            this.tokens = capacity;
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

        synchronized long estimatedMillisUntilNextToken() {
            refill();
            if (tokens >= 1.0) return 0;
            double needed = 1.0 - tokens;
            return (long) Math.ceil(needed / refillRatePerSecond * 1000.0);
        }

        private void refill() {
            Instant now = Instant.now();
            double elapsed = (now.toEpochMilli() - lastRefill.toEpochMilli()) / 1000.0;
            tokens = Math.min(capacity, tokens + elapsed * refillRatePerSecond);
            lastRefill = now;
        }
    }
}
