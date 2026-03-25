package com.rapidalert.rebalancer.dto.response;

public record TemplateHistoryResponse(
        Long id,
        String title,
        String content,
        String imageUrl
) {
}
