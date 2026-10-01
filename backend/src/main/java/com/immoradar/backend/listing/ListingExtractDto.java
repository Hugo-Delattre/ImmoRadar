package com.immoradar.backend.listing;

import java.math.BigDecimal;
import org.jspecify.annotations.Nullable;

public record ListingExtractDto(
        String title,
        BigDecimal price,
        @Nullable BigDecimal monthlyRent,
        BigDecimal surface,
        @Nullable String location,
        String propertyType,
        @Nullable BigDecimal renovationCost,
        @Nullable BigDecimal monthlyCharges,
        @Nullable BigDecimal propertyTax,
        @Nullable String imageUrl,
        @Nullable String description,
        String sourceUrl,
        String platform
) {
}
