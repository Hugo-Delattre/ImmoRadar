package com.immoradar.backend.simulation;

import java.math.BigDecimal;
import java.util.List;

public record SimulationResponse(
        BigDecimal totalProjectCost,
        BigDecimal loanAmount,
        BigDecimal monthlyMortgage,
        BigDecimal monthlyCashFlow,
        BigDecimal grossYield,
        BigDecimal netYield,
        BigDecimal taxAnnual,
        BigDecimal annualOperatingExpenses,
        BigDecimal breakEvenRent,
        String cashFlowStatus,
        List<ProjectionPoint> projection
) {
    public SimulationResponse {
        projection = List.copyOf(projection);
    }
}
