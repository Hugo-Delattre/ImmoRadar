package com.immoradar.backend.simulation;

import com.immoradar.backend.deal.Deal;
import com.immoradar.backend.deal.DealService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;

@Service
public class FinancialSimulationService {

    private static final BigDecimal TWELVE = BigDecimal.valueOf(12);
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal SOCIAL_CONTRIBUTIONS = new BigDecimal("17.2");

    private final DealService dealService;

    public FinancialSimulationService(DealService dealService) {
        this.dealService = dealService;
    }

    public SimulationResponse simulate(SimulationRequest request) {
        var deal = dealService.getEntity(request.dealId());
        var notaryFees = deal.getPrice().multiply(new BigDecimal("0.075"));
        var totalProjectCost = deal.getPrice().add(deal.getRenovationCost()).add(notaryFees);
        var loanAmount = totalProjectCost.subtract(request.downpayment()).max(BigDecimal.ZERO);
        var monthlyMortgage = monthlyPayment(loanAmount, request.interestRate(), request.loanTermYears());
        var annualRent = effectiveAnnualRent(deal, request.vacancyRate());
        var operatingExpenses = annualOperatingExpenses(deal, annualRent, request);
        var firstYearInterest = firstYearInterest(loanAmount, request.interestRate(), monthlyMortgage);
        var taxAnnual = annualTax(deal, request, annualRent, operatingExpenses, firstYearInterest);
        var annualCashFlow = annualRent
                .subtract(operatingExpenses)
                .subtract(monthlyMortgage.multiply(TWELVE))
                .subtract(taxAnnual);
        var monthlyCashFlow = annualCashFlow.divide(TWELVE, 2, RoundingMode.HALF_UP);
        var grossYield = percentage(deal.getMonthlyRent().multiply(TWELVE), totalProjectCost);
        var netYield = percentage(annualRent.subtract(operatingExpenses), totalProjectCost);
        var breakEvenRent = operatingExpenses.add(monthlyMortgage.multiply(TWELVE)).add(taxAnnual)
                .divide(TWELVE, 2, RoundingMode.HALF_UP);
        var taxComparison = buildTaxComparison(deal, request, annualRent, operatingExpenses, firstYearInterest, monthlyMortgage, totalProjectCost);
        // Indicative debt ratio based on French average investor household net income (3 500€/month)
        var debtEffortRatio = monthlyMortgage.signum() > 0
                ? monthlyMortgage.divide(new BigDecimal("3500"), 4, RoundingMode.HALF_UP).multiply(HUNDRED).setScale(1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return new SimulationResponse(
                money(totalProjectCost), money(loanAmount), money(monthlyMortgage), money(monthlyCashFlow),
                percent(grossYield), percent(netYield), money(taxAnnual), money(operatingExpenses),
                money(breakEvenRent), cashFlowStatus(monthlyCashFlow),
                buildProjection(deal, request, loanAmount, monthlyMortgage),
                taxComparison, debtEffortRatio);
    }

    private List<TaxComparisonItem> buildTaxComparison(
            Deal deal,
            SimulationRequest request,
            BigDecimal annualRent,
            BigDecimal operatingExpenses,
            BigDecimal firstYearInterest,
            BigDecimal monthlyMortgage,
            BigDecimal totalProjectCost) {
        var regimes = List.of(
                TaxRegime.REEL_LMNP,
                TaxRegime.MICRO_BIC,
                TaxRegime.NU,
                TaxRegime.SCI_IS
        );

        TaxRegime bestRegime = TaxRegime.REEL_LMNP;
        BigDecimal bestCashFlow = new BigDecimal("-999999999");

        for (var regime : regimes) {
            var tempRequest = new SimulationRequest(
                    request.dealId(), request.downpayment(), request.interestRate(),
                    request.loanTermYears(), regime, request.marginalTaxRate(),
                    request.vacancyRate(), request.managementRate(), request.insuranceAnnual(),
                    request.rentGrowthRate(), request.propertyGrowthRate()
            );
            var tax = annualTax(deal, tempRequest, annualRent, operatingExpenses, firstYearInterest);
            var cashFlow = annualRent.subtract(operatingExpenses)
                    .subtract(monthlyMortgage.multiply(TWELVE))
                    .subtract(tax)
                    .divide(TWELVE, 2, RoundingMode.HALF_UP);
            if (cashFlow.compareTo(bestCashFlow) > 0) {
                bestCashFlow = cashFlow;
                bestRegime = regime;
            }
        }

        var list = new ArrayList<TaxComparisonItem>();
        for (var regime : regimes) {
            var tempRequest = new SimulationRequest(
                    request.dealId(), request.downpayment(), request.interestRate(),
                    request.loanTermYears(), regime, request.marginalTaxRate(),
                    request.vacancyRate(), request.managementRate(), request.insuranceAnnual(),
                    request.rentGrowthRate(), request.propertyGrowthRate()
            );
            var tax = annualTax(deal, tempRequest, annualRent, operatingExpenses, firstYearInterest);
            var cashFlow = annualRent.subtract(operatingExpenses)
                    .subtract(monthlyMortgage.multiply(TWELVE))
                    .subtract(tax)
                    .divide(TWELVE, 2, RoundingMode.HALF_UP);
            var netYield = percentage(annualRent.subtract(operatingExpenses).subtract(tax), totalProjectCost);

            var isRecommended = (regime == bestRegime);
            var label = switch (regime) {
                case REEL_LMNP -> "LMNP Réel";
                case MICRO_BIC -> "LMNP Micro-BIC";
                case NU -> "Location Nue";
                case SCI_IS -> "SCI à l'IS";
            };
            var advantage = switch (regime) {
                case REEL_LMNP -> "Amortissement bâti (85%) & déduction intégrale des travaux et intérêts";
                case MICRO_BIC -> "Abattement forfaitaire simple de 50% sur les loyers bruts";
                case NU -> "Régime forfaitaire (abattement 30%) avec imputation déficit foncier";
                case SCI_IS -> "Taux réduit IS à 15% jusqu'à 42 500€ de bénéfice, capitalisation";
            };

            list.add(new TaxComparisonItem(
                    regime, label, money(tax), money(cashFlow), percent(netYield), isRecommended, advantage
            ));
        }

        return list;
    }

    private ArrayList<ProjectionPoint> buildProjection(
            Deal deal,
            SimulationRequest request,
            BigDecimal initialLoan,
            BigDecimal monthlyMortgage) {
        var projection = new ArrayList<ProjectionPoint>();
        var remainingLoan = initialLoan;
        var cumulativeCashFlow = BigDecimal.ZERO;
        var propertyValue = deal.getPrice();
        var rentGrowth = rate(request.rentGrowthRate());
        var propertyGrowth = rate(request.propertyGrowthRate());
        var monthlyRate = rate(request.interestRate()).divide(TWELVE, 12, RoundingMode.HALF_UP);

        for (int year = 1; year <= request.loanTermYears(); year++) {
            var growthMultiplier = BigDecimal.ONE.add(rentGrowth).pow(year - 1);
            var annualRent = effectiveAnnualRent(deal, request.vacancyRate()).multiply(growthMultiplier);
            var operatingExpenses = annualOperatingExpenses(deal, annualRent, request)
                    .multiply(BigDecimal.ONE.add(new BigDecimal("0.02")).pow(year - 1));
            var annualInterest = BigDecimal.ZERO;

            for (int month = 0; month < 12 && remainingLoan.signum() > 0; month++) {
                var interest = remainingLoan.multiply(monthlyRate);
                var principal = monthlyMortgage.subtract(interest).max(BigDecimal.ZERO);
                annualInterest = annualInterest.add(interest);
                remainingLoan = remainingLoan.subtract(principal).max(BigDecimal.ZERO);
            }

            var tax = annualTax(deal, request, annualRent, operatingExpenses, annualInterest);
            var annualCashFlow = annualRent.subtract(operatingExpenses)
                    .subtract(monthlyMortgage.multiply(TWELVE)).subtract(tax);
            cumulativeCashFlow = cumulativeCashFlow.add(annualCashFlow);
            propertyValue = year == 1 ? propertyValue : propertyValue.multiply(BigDecimal.ONE.add(propertyGrowth));
            var netWorth = propertyValue.subtract(remainingLoan).add(cumulativeCashFlow);

            projection.add(new ProjectionPoint(
                    year, money(annualCashFlow), money(cumulativeCashFlow), money(remainingLoan),
                    money(propertyValue), money(netWorth)));
        }
        return projection;
    }

    private BigDecimal annualOperatingExpenses(
            Deal deal, BigDecimal annualRent, SimulationRequest request) {
        var managementFees = annualRent.multiply(rate(request.managementRate()));
        return deal.getMonthlyCharges().multiply(TWELVE)
                .add(deal.getPropertyTax())
                .add(request.insuranceAnnual())
                .add(managementFees);
    }

    private BigDecimal annualTax(
            Deal deal,
            SimulationRequest request,
            BigDecimal annualRent,
            BigDecimal operatingExpenses,
            BigDecimal annualInterest) {
        var householdRate = rate(request.marginalTaxRate().add(SOCIAL_CONTRIBUTIONS));
        return switch (request.taxRegime()) {
            case MICRO_BIC -> annualRent.multiply(new BigDecimal("0.50")).multiply(householdRate);
            case NU -> annualRent.multiply(new BigDecimal("0.70")).multiply(householdRate);
            case REEL_LMNP -> {
                var depreciation = deal.getPrice().multiply(new BigDecimal("0.85"))
                        .divide(BigDecimal.valueOf(30), 8, RoundingMode.HALF_UP)
                        .add(deal.getRenovationCost().divide(BigDecimal.TEN, 8, RoundingMode.HALF_UP));
                var taxable = annualRent.subtract(operatingExpenses).subtract(annualInterest)
                        .subtract(depreciation).max(BigDecimal.ZERO);
                yield taxable.multiply(householdRate);
            }
            case SCI_IS -> {
                var depreciation = deal.getPrice().multiply(new BigDecimal("0.85"))
                        .divide(BigDecimal.valueOf(30), 8, RoundingMode.HALF_UP);
                var taxable = annualRent.subtract(operatingExpenses).subtract(annualInterest)
                        .subtract(depreciation).max(BigDecimal.ZERO);
                var reducedBase = taxable.min(new BigDecimal("42500"));
                var standardBase = taxable.subtract(reducedBase).max(BigDecimal.ZERO);
                yield reducedBase.multiply(new BigDecimal("0.15"))
                        .add(standardBase.multiply(new BigDecimal("0.25")));
            }
        };
    }

    private static BigDecimal effectiveAnnualRent(Deal deal, BigDecimal vacancyRate) {
        return deal.getMonthlyRent().multiply(TWELVE).multiply(BigDecimal.ONE.subtract(rate(vacancyRate)));
    }

    private static BigDecimal monthlyPayment(BigDecimal principal, BigDecimal annualRate, int years) {
        int months = years * 12;
        if (principal.signum() == 0) {
            return BigDecimal.ZERO;
        }
        if (annualRate.signum() == 0) {
            return principal.divide(BigDecimal.valueOf(months), 8, RoundingMode.HALF_UP);
        }
        double rate = annualRate.doubleValue() / 1200.0;
        double factor = Math.pow(1 + rate, months);
        return BigDecimal.valueOf(principal.doubleValue() * rate * factor / (factor - 1));
    }

    private static BigDecimal firstYearInterest(
            BigDecimal principal, BigDecimal annualRate, BigDecimal monthlyPayment) {
        var remaining = principal;
        var interestTotal = BigDecimal.ZERO;
        var monthlyRate = rate(annualRate).divide(TWELVE, 12, RoundingMode.HALF_UP);
        for (int month = 0; month < 12 && remaining.signum() > 0; month++) {
            var interest = remaining.multiply(monthlyRate);
            interestTotal = interestTotal.add(interest);
            remaining = remaining.subtract(monthlyPayment.subtract(interest).max(BigDecimal.ZERO)).max(BigDecimal.ZERO);
        }
        return interestTotal;
    }

    private static BigDecimal rate(BigDecimal percentage) {
        return percentage.divide(HUNDRED, 10, RoundingMode.HALF_UP);
    }

    private static BigDecimal percentage(BigDecimal numerator, BigDecimal denominator) {
        return numerator.multiply(HUNDRED).divide(denominator, 8, RoundingMode.HALF_UP);
    }

    private static BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal percent(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private static String cashFlowStatus(BigDecimal monthlyCashFlow) {
        if (monthlyCashFlow.compareTo(BigDecimal.valueOf(100)) >= 0) {
            return "POSITIF";
        }
        if (monthlyCashFlow.signum() >= 0) {
            return "EQUILIBRE";
        }
        return "A_OPTIMISER";
    }
}
