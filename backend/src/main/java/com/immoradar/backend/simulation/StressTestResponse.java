package com.immoradar.backend.simulation;

import java.math.BigDecimal;
import java.util.List;

public record StressTestResponse(String dealId, List<Scenario> scenarios, String notice) {
    public StressTestResponse { scenarios = List.copyOf(scenarios); }

    public record Scenario(String key, String label, BigDecimal monthlyRent, BigDecimal monthlyCharges,
                           BigDecimal propertyTax, BigDecimal renovationCost, BigDecimal insuranceAnnual,
                           BigDecimal vacancyRate, BigDecimal totalProjectCost, BigDecimal monthlyMortgage,
                           BigDecimal monthlyCashFlow, BigDecimal deltaFromCentral,
                           BigDecimal annualCashShortfall, BigDecimal breakEvenRent) {}
}
