package com.rapidalert.gateway.filter;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Predicate;

/**
 * Decides whether a request must carry a JWT.
 *
 * Earlier versions used {@code path.contains(prefix)}, which allowed
 * substrings such as {@code /recipients/swagger-ui-foo} to bypass auth. The current
 * implementation matches by path prefix only, against an explicit allowlist.
 */
@Component
public class RouteValidator {

    /** Path prefixes that are publicly reachable without a JWT. */
    public static final List<String> PUBLIC_PATH_PREFIXES = List.of(
            "/api/v1/auth/register",
            "/api/v1/auth/authenticate",
            "/api/v1/auth/login",
            "/api/v1/auth/refresh",
            "/api/v1/health",
            "/actuator/health",
            "/swagger-ui",
            "/v3/api-docs",
            "/webjars",
            "/security-service/api-docs",
            "/notification-service/api-docs",
            "/template-service/api-docs",
            "/recipient-service/api-docs",
            "/file-service/api-docs",
            "/url-shortener/api-docs"
    );

    public Predicate<ServerHttpRequest> isSecured = request -> {
        String path = request.getURI().getPath();
        return PUBLIC_PATH_PREFIXES.stream().noneMatch(path::startsWith);
    };
}
