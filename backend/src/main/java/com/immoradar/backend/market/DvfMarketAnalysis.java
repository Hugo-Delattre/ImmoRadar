package com.immoradar.backend.market;

import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.util.List;

/** A null median means that no market conclusion may be drawn. */
public record DvfMarketAnalysis(
        boolean available,
        String location,
        @Nullable String codeInsee,
        @Nullable String propertyCategory,
        BigDecimal dealPricePerSquareMeter,
        @Nullable BigDecimal medianPricePerSquareMeter,
        @Nullable BigDecimal deltaPercentage,
        int comparableCount,
        @Nullable Integer referenceYear,
        @Nullable String reliability,
        @Nullable String sourceUrl,
        @Nullable String methodologyUrl,
        List<RecentSale> recentSales,
        @Nullable String recentSalesSourceUrl,
        String notice
) {
    public DvfMarketAnalysis {
        recentSales = List.copyOf(recentSales);
    }

    public static DvfMarketAnalysis unavailable(String location, BigDecimal dealPricePerSquareMeter, String reason) {
        return new DvfMarketAnalysis(false, location, null, null, dealPricePerSquareMeter,
                null, null, 0, null, null, null, null, List.of(), null, reason);
    }
}
