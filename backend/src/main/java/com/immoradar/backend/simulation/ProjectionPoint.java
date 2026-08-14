package com.immoradar.backend.simulation;

import java.math.BigDecimal;

public record ProjectionPoint(
        int year,
        BigDecimal annualCashFlow,
        BigDecimal cumulativeCashFlow,
        BigDecimal remainingLoan,
        BigDecimal estimatedPropertyValue,
        BigDecimal netWorth
) {}
