package com.immoradar.backend.simulation;

import org.jspecify.annotations.Nullable;

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
        List<ProjectionPoint> projection,
        List<TaxComparisonItem> taxComparison,
        @Nullable BigDecimal debtEffortRatio,
        BigDecimal internalRateOfReturn,
        BigDecimal netPresentValue,
        BigDecimal monthlyLoanInsurance
) {
    public SimulationResponse {
        projection = List.copyOf(projection);
        taxComparison = taxComparison != null ? List.copyOf(taxComparison) : List.of();
    }

    public SimulationResponse(
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
            List<ProjectionPoint> projection) {
        this(totalProjectCost, loanAmount, monthlyMortgage, monthlyCashFlow, grossYield, netYield,
                taxAnnual, annualOperatingExpenses, breakEvenRent, cashFlowStatus, projection,
                List.of(), null, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
    }
}
