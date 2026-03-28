package com.rapidalert.notification.dedup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Content-hash deduplication service that prevents duplicate alert delivery.
 *
 * During disaster events, the same alert may be received from multiple sources
 * (NOAA, NWS, IPAWS) or retried by upstream systems. This service ensures each
 * unique alert content is delivered exactly once within a configurable window.
 *
 * Hash computation uses SHA-256 over normalized content to handle minor
 * formatting differences between sources while catching true duplicates.
 */
@Service
public class DeduplicationService {

    private static final Logger log = LoggerFactory.getLogger(DeduplicationService.class);
    private static final Duration DEFAULT_TTL = Duration.ofHours(1);

    private final Map<String, Instant> seenHashes = new ConcurrentHashMap<>();
    private final Duration ttl;

    public DeduplicationService() {
        this(DEFAULT_TTL);
    }

    public DeduplicationService(Duration ttl) {
        this.ttl = ttl;
    }

    /**
     * Checks if the given content has been seen within the dedup window.
     *
     * @param content the alert content to check
     * @return true if this is a duplicate (already seen), false if new
     */
    public boolean isDuplicate(String content) {
        cleanup();
        String hash = computeHash(normalizeContent(content));
        Instant existing = seenHashes.putIfAbsent(hash, Instant.now());
        if (existing != null) {
            log.info("Duplicate alert detected: hash={}", hash.substring(0, 12));
            return true;
        }
        log.debug("New alert registered: hash={}", hash.substring(0, 12));
        return false;
    }

    /**
     * Returns the number of unique alerts tracked in the current window.
     */
    public int getTrackedCount() {
        cleanup();
        return seenHashes.size();
    }

    /**
     * Normalizes content by removing whitespace variations and lowering case
     * to ensure semantically identical alerts from different sources match.
     */
    private String normalizeContent(String content) {
        if (content == null) return "";
        return content.trim()
                .replaceAll("\\s+", " ")
                .toLowerCase();
    }

    /**
     * Computes SHA-256 hash of the normalized content.
     */
    private String computeHash(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    /**
     * Removes expired hashes that have exceeded the TTL window.
     */
    private void cleanup() {
        Instant cutoff = Instant.now().minus(ttl);
        Iterator<Map.Entry<String, Instant>> it = seenHashes.entrySet().iterator();
        while (it.hasNext()) {
            if (it.next().getValue().isBefore(cutoff)) {
                it.remove();
            }
        }
    }
}
