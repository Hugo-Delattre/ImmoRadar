package com.immoradar.backend.simulation;

import java.math.BigDecimal;

public record TaxComparisonItem(
        TaxRegime regime,
        String label,
        BigDecimal annualTax,
        BigDecimal monthlyCashFlow,
        BigDecimal netYield,
        boolean isRecommended,
        String advantage
) {
}
