package com.immoradar.backend.deal;

import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;

/**
 * Critères du radar. Sans statut précisé, les biens écartés sont masqués.
 * {@code excludeEnergySieves} retire les passoires énergétiques (F et G).
 */
public record DealSearchCriteria(
        @Nullable BigDecimal priceMax,
        @Nullable BigDecimal yieldMin,
        @Nullable BigDecimal cashflowMin,
        @Nullable String location,
        boolean favoritesOnly,
        @Nullable PropertyType propertyType,
        boolean excludeEnergySieves,
        @Nullable DealStatus status,
        DealSort sort
) {
    public DealSearchCriteria(
            @Nullable BigDecimal priceMax, @Nullable BigDecimal yieldMin, @Nullable BigDecimal cashflowMin,
            @Nullable String location, boolean favoritesOnly) {
        this(priceMax, yieldMin, cashflowMin, location, favoritesOnly, null, false, null, DealSort.SCORE);
    }
}
