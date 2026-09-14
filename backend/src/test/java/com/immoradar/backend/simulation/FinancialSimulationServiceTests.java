package com.immoradar.backend.simulation;

import com.immoradar.backend.deal.Deal;
import com.immoradar.backend.deal.DealService;
import com.immoradar.backend.deal.PropertyType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FinancialSimulationServiceTests {

    private DealService dealService;
    private FinancialSimulationService simulationService;

    @BeforeEach
    void setUp() {
        dealService = mock(DealService.class);
        simulationService = new FinancialSimulationService(dealService);
        when(dealService.getEntity("deal-1")).thenReturn(sampleDeal());
    }

    @Test
    void shouldBuildACompleteTwentyYearProjection() {
        var result = simulationService.simulate(request(TaxRegime.REEL_LMNP, "3.5"));

        assertThat(result.loanAmount()).isPositive();
        assertThat(result.monthlyMortgage()).isPositive();
        assertThat(result.projection()).hasSize(20);
        assertThat(result.projection().getLast().remainingLoan()).isLessThan(new BigDecimal("1.00"));
        assertThat(result.projection().getLast().netWorth()).isPositive();
    }

    @Test
    void shouldHandleAZeroInterestLoan() {
        var result = simulationService.simulate(request(TaxRegime.REEL_LMNP, "0"));

        assertThat(result.monthlyMortgage()).isEqualByComparingTo("731.25");
        assertThat(result.projection().getLast().remainingLoan()).isEqualByComparingTo("0.00");
    }

    @Test
    void shouldExposeTheFiscalImpactOfMicroBic() {
        var realResult = simulationService.simulate(request(TaxRegime.REEL_LMNP, "3.5"));
        var microResult = simulationService.simulate(request(TaxRegime.MICRO_BIC, "3.5"));

        assertThat(microResult.taxAnnual()).isGreaterThan(realResult.taxAnnual());
        assertThat(microResult.monthlyCashFlow()).isLessThan(realResult.monthlyCashFlow());
    }

    @Test
    void shouldProvideMultiRegimeTaxComparisonAndDebtEffort() {
        var result = simulationService.simulate(request(TaxRegime.REEL_LMNP, "3.5"));

        assertThat(result.taxComparison()).hasSize(4);
        assertThat(result.taxComparison()).anyMatch(TaxComparisonItem::isRecommended);
        assertThat(result.debtEffortRatio()).isPositive();
    }

    @Test
    void shouldCalculateInstitutionalIrrAndNpv() {
        var result = simulationService.simulate(request(TaxRegime.REEL_LMNP, "3.5"));

        assertThat(result.internalRateOfReturn()).isNotNull();
        assertThat(result.internalRateOfReturn()).isGreaterThan(BigDecimal.ZERO);
        assertThat(result.netPresentValue()).isNotNull();
        assertThat(result.netPresentValue()).isPositive();
    }

    private SimulationRequest request(TaxRegime regime, String interestRate) {
        return new SimulationRequest(
                "deal-1", amount("30000"), amount(interestRate), 20, regime,
                amount("30"), amount("4"), BigDecimal.ZERO, amount("180"),
                amount("1.5"), amount("1.2"));
    }

    private Deal sampleDeal() {
        return new Deal(
                "deal-1", "T3 de test", amount("180000"), amount("1350"), amount("110"),
                amount("950"), amount("12000"), "Angers (49)", amount("65"),
                PropertyType.APARTMENT, "Bien de test", 8.7, "https://example.com/property.jpg", false);
    }

    private static BigDecimal amount(String value) {
        return new BigDecimal(value);
    }
}
