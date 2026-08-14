package com.immoradar.backend.deal.dto;

import java.util.List;

public record DealSearchResponse(
        List<DealResponse> content,
        long totalElements,
        int page,
        int size,
        int totalPages
) {
    public DealSearchResponse {
        content = List.copyOf(content);
    }
}
