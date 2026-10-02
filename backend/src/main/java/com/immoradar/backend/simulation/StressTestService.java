package com.immoradar.backend.simulation;

import com.immoradar.backend.deal.Deal;
import com.immoradar.backend.deal.DealService;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class StressTestService {
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private final DealService deals;
    private final FinancialSimulationService simulations;

    public StressTestService(DealService deals, FinancialSimulationService simulations) {
        this.deals = deals;
        this.simulations = simulations;
    }

    public StressTestResponse compare(StressTestRequest request) {
        Deal deal = deals.getEntity(request.base().dealId());
        var central = simulate(deal, request, 0, "CENTRAL", "Central", BigDecimal.ZERO);
        var prudent = simulate(deal, request, 1, "PRUDENT", "Prudent", central.monthlyCashFlow());
        var adverse = simulate(deal, request, 2, "ADVERSE", "Dégradé", central.monthlyCashFlow());
        return new StressTestResponse(deal.getId(), List.of(central, prudent, adverse),
                "Tests d'hypothèses, pas prévisions ni probabilités. Le scénario dégradé double les chocs du prudent. "
                + "Charges, taxe et assurance sont majorées ; gestion et fiscalité sont recalculées. "
                + "Le surcoût des travaux augmente le financement à apport constant. Fiscalité simplifiée, "
                + "hors assurance de prêt et frais de sortie. Un cash-flow positif ne certifie pas une opportunité.");
    }

    private StressTestResponse.Scenario simulate(Deal original, StressTestRequest stress, int severity,
                                                  String key, String label, BigDecimal centralCashFlow) {
        BigDecimal multiplier = BigDecimal.valueOf(severity);
        BigDecimal rentFactor = BigDecimal.ONE.subtract(stress.rentDropPercent().multiply(multiplier).divide(HUNDRED));
        BigDecimal costFactor = BigDecimal.ONE.add(stress.operatingCostIncreasePercent().multiply(multiplier).divide(HUNDRED));
        BigDecimal workFactor = BigDecimal.ONE.add(stress.renovationIncreasePercent().multiply(multiplier).divide(HUNDRED));
        var deal = new Deal(original.getId(), original.getTitle(), original.getPrice(),
                money(original.getMonthlyRent().multiply(rentFactor)), money(original.getMonthlyCharges().multiply(costFactor)),
                money(original.getPropertyTax().multiply(costFactor)), money(original.getRenovationCost().multiply(workFactor)),
                original.getLocation(), original.getSurface(), original.getPropertyType(), original.getDescription(),
                original.getOpportunityScore(), original.getImageUrl(), original.isFavorite(), original.getSourceUrl());
        SimulationRequest base = stress.base();
        BigDecimal vacancy = base.vacancyRate().add(stress.vacancyIncreasePoints().multiply(multiplier));
        var scenarioRequest = new SimulationRequest(base.dealId(), base.downpayment(), base.interestRate(),
                base.loanTermYears(), base.taxRegime(), base.marginalTaxRate(), vacancy, base.managementRate(),
                money(base.insuranceAnnual().multiply(costFactor)), base.rentGrowthRate(), base.propertyGrowthRate());
        SimulationResponse result = simulations.simulateForDeal(deal, scenarioRequest);
        return new StressTestResponse.Scenario(key, label, deal.getMonthlyRent(), deal.getMonthlyCharges(),
                deal.getPropertyTax(), deal.getRenovationCost(), scenarioRequest.insuranceAnnual(), vacancy,
                result.totalProjectCost(), result.monthlyMortgage(), result.monthlyCashFlow(),
                severity == 0 ? BigDecimal.ZERO : result.monthlyCashFlow().subtract(centralCashFlow),
                result.monthlyCashFlow().negate().max(BigDecimal.ZERO).multiply(BigDecimal.valueOf(12)), result.breakEvenRent());
    }

    private static BigDecimal money(BigDecimal value) { return value.setScale(2, RoundingMode.HALF_UP); }
}
