package com.rapidalert.security.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.function.Function;

import static io.jsonwebtoken.SignatureAlgorithm.HS256;

/**
 * Issues and validates the JWT used across the platform.
 *
 * The signing key is sourced from the JWT_SIGNING_KEY environment variable (base64-encoded,
 * at least 32 bytes decoded). If the variable is unset — only acceptable for local
 * development — a per-boot random key is generated and a WARN is logged. Tokens issued
 * with a per-boot key invalidate on restart by design.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JwtService {

    @Value("${security.jwt.key:}")
    private String configuredKey;

    @Value("${security.jwt.prefix}")
    public String prefix;

    private Key signingKey;

    @PostConstruct
    void initializeSigningKey() {
        if (configuredKey != null && !configuredKey.isBlank()) {
            byte[] keyBytes = Decoders.BASE64.decode(configuredKey);
            this.signingKey = Keys.hmacShaKeyFor(keyBytes);
            log.info("JWT signing key loaded from configuration ({} bytes).", keyBytes.length);
        } else {
            byte[] random = new byte[64];
            new SecureRandom().nextBytes(random);
            this.signingKey = Keys.hmacShaKeyFor(random);
            log.warn("JWT_SIGNING_KEY is not set; generated a 64-byte random key for this JVM. "
                    + "Tokens will not survive a restart. Set JWT_SIGNING_KEY in production.");
            log.warn("Suggested value (base64): {}", Base64.getEncoder().encodeToString(random));
        }
    }

    public String generateJwt(UserDetails userDetails) {
        return Jwts.builder()
                .setClaims(new HashMap<>())
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + 1000L * 60 * 60 * 24))
                .signWith(signingKey, HS256)
                .compact();
    }

    public String extractJwt(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith(prefix)) {
            return authHeader.substring(prefix.length());
        }
        return null;
    }

    public boolean isJwtValid(String jwt, UserDetails userDetails) {
        String username = extractEmail(jwt);
        return username.equals(userDetails.getUsername()) && !isJwtExpired(jwt);
    }

    public String extractEmail(String jwt) {
        return extractClaim(jwt, Claims::getSubject);
    }

    private boolean isJwtExpired(String jwt) {
        return extractClaim(jwt, Claims::getExpiration).before(new Date());
    }

    private <T> T extractClaim(String jwt, Function<Claims, T> claimsResolver) {
        Claims claims = extractAllClaims(jwt);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String jwt) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(jwt)
                .getBody();
    }
}
