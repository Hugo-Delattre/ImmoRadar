package com.immoradar.backend.simulation;

import com.immoradar.backend.deal.Deal;
import com.immoradar.backend.deal.DealService;
import com.immoradar.backend.deal.PropertyType;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class StressTestServiceTests {
    private final DealService deals = mock(DealService.class);
    private final FinancialSimulationService simulations = new FinancialSimulationService(deals);
    private final StressTestService service = new StressTestService(deals, simulations);

    @Test
    void centralMatchesTheSimulatorAndAdverseShocksNeverMutateTheDeal() {
        Deal deal = sampleDeal();
        when(deals.getEntity("test")).thenReturn(deal);
        var result = service.compare(stress(base(), "10", "10", "15", "5"));
        var central = result.scenarios().getFirst();
        var prudent = result.scenarios().get(1);
        var adverse = result.scenarios().getLast();
        assertThat(central.monthlyCashFlow()).isEqualByComparingTo(simulations.simulate(base()).monthlyCashFlow());
        assertThat(prudent.monthlyRent()).isEqualByComparingTo("900");
        assertThat(adverse.monthlyRent()).isEqualByComparingTo("800");
        assertThat(prudent.vacancyRate()).isEqualByComparingTo("9");
        assertThat(adverse.vacancyRate()).isEqualByComparingTo("14");
        assertThat(prudent.renovationCost()).isEqualByComparingTo("11500");
        assertThat(adverse.monthlyMortgage()).isGreaterThan(prudent.monthlyMortgage());
        assertThat(adverse.monthlyCashFlow()).isLessThan(prudent.monthlyCashFlow());
        assertThat(prudent.monthlyCashFlow()).isLessThan(central.monthlyCashFlow());
        assertThat(deal.getMonthlyRent()).isEqualByComparingTo("1000");
        assertThat(deal.getRenovationCost()).isEqualByComparingTo("10000");
        assertThat(deal.getMonthlyCharges()).isEqualByComparingTo("100");
    }

    @Test
    void zeroShocksProduceIdenticalScenariosAndShortfallOnlyCountsNegativeCashFlow() {
        when(deals.getEntity("test")).thenReturn(sampleDeal());
        var result = service.compare(stress(base(), "0", "0", "0", "0"));
        var central = result.scenarios().getFirst();
        assertThat(result.scenarios()).allSatisfy(scenario -> {
            assertThat(scenario.monthlyCashFlow()).isEqualByComparingTo(central.monthlyCashFlow());
            assertThat(scenario.deltaFromCentral()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(scenario.annualCashShortfall()).isEqualByComparingTo(
                    scenario.monthlyCashFlow().negate().max(BigDecimal.ZERO).multiply(amount("12")));
        });
    }

    @Test
    void largestAllowedShocksRemainFiniteWithPositiveRentAndLessThanFullVacancy() {
        when(deals.getEntity("test")).thenReturn(sampleDeal());
        var adverse = service.compare(stress(base(), "40", "100", "100", "30")).scenarios().getLast();
        assertThat(adverse.monthlyRent()).isEqualByComparingTo("200");
        assertThat(adverse.vacancyRate()).isEqualByComparingTo("64");
        assertThat(adverse.annualCashShortfall()).isPositive();
        assertThat(adverse.breakEvenRent()).isPositive();
    }

    static SimulationRequest base() {
        return new SimulationRequest("test", amount("30000"), amount("3.5"), 20, TaxRegime.REEL_LMNP,
                amount("30"), amount("4"), amount("5"), amount("180"), amount("1.5"), amount("1.2"));
    }
    static StressTestRequest stress(SimulationRequest base, String rent, String costs, String works, String vacancy) {
        return new StressTestRequest(base, amount(rent), amount(costs), amount(works), amount(vacancy));
    }
    private static Deal sampleDeal() {
        return new Deal("test", "T3", amount("150000"), amount("1000"), amount("100"), amount("800"),
                amount("10000"), "Limoges", amount("60"), PropertyType.APARTMENT, "", 0, "", false);
    }
    private static BigDecimal amount(String value) { return new BigDecimal(value); }
}
