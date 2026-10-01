package com.immoradar.backend.deal;

import org.springframework.data.domain.Sort;

/** Tris proposés dans le radar ; chacun s'appuie sur une colonne stockée pour rester en SQL. */
public enum DealSort {
    SCORE(order(Sort.Direction.DESC, "opportunityScore")),
    YIELD(order(Sort.Direction.DESC, "grossYield")),
    MARKET_DISCOUNT(order(Sort.Direction.ASC, "marketDeltaPercent")),
    PRICE_DROP(order(Sort.Direction.DESC, "priceDropPercent")),
    PRICE_ASC(order(Sort.Direction.ASC, "price")),
    PRICE_M2_ASC(order(Sort.Direction.ASC, "pricePerSquareMeter")),
    NEWEST(order(Sort.Direction.DESC, "createdOn"));

    private final Sort sort;

    DealSort(Sort sort) {
        this.sort = sort;
    }

    private static Sort order(Sort.Direction direction, String property) {
        return Sort.by(new Sort.Order(direction, property, Sort.NullHandling.NULLS_LAST));
    }

    public Sort toSort() {
        // Ordre stable à valeur égale
        return sort.and(Sort.by(Sort.Direction.DESC, "opportunityScore")).and(Sort.by("id"));
    }
}
