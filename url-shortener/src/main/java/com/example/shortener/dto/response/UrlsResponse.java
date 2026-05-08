package com.rapidalert.shortener.dto.response;

import java.util.Map;

public record UrlsResponse(
        Long urlId,
        Map<String, String> urlOptionMap
) {
}
