package com.immoradar.backend.listing;

import java.math.BigDecimal;

public record ListingExtractDto(
        String title,
        BigDecimal price,
        BigDecimal monthlyRent,
        BigDecimal surface,
        String location,
        String propertyType,
        BigDecimal renovationCost,
        BigDecimal monthlyCharges,
        BigDecimal propertyTax,
        String imageUrl,
        String description,
        String sourceUrl,
        String platform
) {
}
