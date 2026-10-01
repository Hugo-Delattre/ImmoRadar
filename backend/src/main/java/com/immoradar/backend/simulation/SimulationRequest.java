package com.immoradar.backend.simulation;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record SimulationRequest(
        @NotBlank String dealId,
        @NotNull @PositiveOrZero BigDecimal downpayment,
        @NotNull @DecimalMin("0.0") @DecimalMax("20.0") BigDecimal interestRate,
        @Min(5) @Max(30) int loanTermYears,
        @NotNull TaxRegime taxRegime,
        @NotNull @DecimalMin("0.0") @DecimalMax("45.0") BigDecimal marginalTaxRate,
        @NotNull @DecimalMin("0.0") @DecimalMax("30.0") BigDecimal vacancyRate,
        @NotNull @DecimalMin("0.0") @DecimalMax("20.0") BigDecimal managementRate,
        @NotNull @PositiveOrZero BigDecimal insuranceAnnual,
        @NotNull @DecimalMin("0.0") @DecimalMax("10.0") BigDecimal rentGrowthRate,
        @NotNull @DecimalMin("-10.0") @DecimalMax("10.0") BigDecimal propertyGrowthRate
) {}
