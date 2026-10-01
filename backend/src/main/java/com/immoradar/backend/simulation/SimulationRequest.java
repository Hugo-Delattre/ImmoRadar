package com.immoradar.backend.simulation;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;

/**
 * Hypothèses d'une simulation.
 *
 * <p>{@code monthlyNetIncome} et {@code existingMonthlyDebt} décrivent le foyer emprunteur : sans revenu
 * renseigné, le taux d'effort HCSF n'est pas calculé plutôt que d'être estimé sur un revenu fictif.
 * {@code loanInsuranceRate} est le taux annuel de l'assurance emprunteur, appliqué au capital initial.
 */
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
        @NotNull @DecimalMin("-10.0") @DecimalMax("10.0") BigDecimal propertyGrowthRate,
        @Nullable @PositiveOrZero BigDecimal monthlyNetIncome,
        @Nullable @PositiveOrZero BigDecimal existingMonthlyDebt,
        @Nullable @DecimalMin("0.0") @DecimalMax("2.0") BigDecimal loanInsuranceRate
) {

    public SimulationRequest(
            String dealId,
            BigDecimal downpayment,
            BigDecimal interestRate,
            int loanTermYears,
            TaxRegime taxRegime,
            BigDecimal marginalTaxRate,
            BigDecimal vacancyRate,
            BigDecimal managementRate,
            BigDecimal insuranceAnnual,
            BigDecimal rentGrowthRate,
            BigDecimal propertyGrowthRate) {
        this(dealId, downpayment, interestRate, loanTermYears, taxRegime, marginalTaxRate, vacancyRate,
                managementRate, insuranceAnnual, rentGrowthRate, propertyGrowthRate, null, null, null);
    }

    public SimulationRequest withTaxRegime(TaxRegime regime) {
        return new SimulationRequest(dealId, downpayment, interestRate, loanTermYears, regime, marginalTaxRate,
                vacancyRate, managementRate, insuranceAnnual, rentGrowthRate, propertyGrowthRate,
                monthlyNetIncome, existingMonthlyDebt, loanInsuranceRate);
    }
}
