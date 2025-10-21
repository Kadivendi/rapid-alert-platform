package com.rapidalert.gateway.filter;

import com.github.benmanes.caffeine.cache.Cache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Validates the JWT on every secured request by calling the security service.
 *
 * Implementation notes:
 *  - Uses a {@link WebClient} so the filter stays non-blocking on the Netty event loop.
 *  - The security-service URI is resolved via Eureka through {@code lb://SECURITY-SERVICE},
 *    so the gateway no longer points at {@code localhost:8080} (which is the gateway itself).
 *  - A short-TTL Caffeine cache absorbs hot-path traffic; cache key is the raw JWT,
 *    value is the resolved client id.
 *  - The injected request header is {@code X-Client-Id} (capitalized), matching the
 *    documented contract in the README.
 */
@Slf4j
@Component
public class AuthenticationGatewayFilterFactory
        extends AbstractGatewayFilterFactory<AuthenticationGatewayFilterFactory.Config> {

    public static final String CLIENT_ID_HEADER = "X-Client-Id";

    private final RouteValidator validator;
    private final WebClient.Builder webClientBuilder;
    private final Cache<String, Long> validationCache;

    @Value("${urls.validate:lb://SECURITY-SERVICE/api/v1/auth/validate}")
    private String validateUrl;

    public AuthenticationGatewayFilterFactory(
            RouteValidator validator,
            WebClient.Builder webClientBuilder,
            Cache<String, Long> validationCache
    ) {
        super(Config.class);
        this.validator = validator;
        this.webClientBuilder = webClientBuilder;
        this.validationCache = validationCache;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            if (!validator.isSecured.test(exchange.getRequest())) {
                return chain.filter(exchange);
            }

            String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (authHeader == null || authHeader.isBlank()) {
                return reject(exchange, HttpStatus.UNAUTHORIZED, "Missing Authorization header");
            }

            Long cachedClientId = validationCache.getIfPresent(authHeader);
            if (cachedClientId != null) {
                return chain.filter(withClientHeader(exchange, cachedClientId));
            }

            return webClientBuilder.build()
                    .get()
                    .uri(validateUrl)
                    .header(HttpHeaders.AUTHORIZATION, authHeader)
                    .retrieve()
                    .bodyToMono(Long.class)
                    .flatMap(clientId -> {
                        validationCache.put(authHeader, clientId);
                        return chain.filter(withClientHeader(exchange, clientId));
                    })
                    .onErrorResume(WebClientResponseException.class, ex -> {
                        log.debug("Token rejected by security service: {}", ex.getStatusCode());
                        return reject(exchange, HttpStatus.UNAUTHORIZED, "Invalid or expired token");
                    })
                    .onErrorResume(ex -> {
                        log.warn("Token validation failed", ex);
                        return reject(exchange, HttpStatus.SERVICE_UNAVAILABLE,
                                "Security service unavailable");
                    });
        };
    }

    public static class Config {
    }

    private ServerWebExchange withClientHeader(ServerWebExchange exchange, Long clientId) {
        ServerHttpRequest mutated = exchange.getRequest().mutate()
                .header(CLIENT_ID_HEADER, String.valueOf(clientId))
                // Backward compatible alias for older downstream services.
                .header("clientId", String.valueOf(clientId))
                .build();
        return exchange.mutate().request(mutated).build();
    }

    private Mono<Void> reject(ServerWebExchange exchange, HttpStatus status, String reason) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().add("X-Auth-Failure-Reason", reason);
        return response.setComplete();
    }
}
