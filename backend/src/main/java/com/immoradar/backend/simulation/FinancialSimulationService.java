package com.immoradar.backend.simulation;

import com.immoradar.backend.deal.Deal;
import com.immoradar.backend.deal.DealService;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
public class FinancialSimulationService {

    private static final BigDecimal TWELVE = BigDecimal.valueOf(12);
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal SOCIAL_CONTRIBUTIONS = new BigDecimal("17.2");
    private static final BigDecimal BANK_RENT_RETENTION = new BigDecimal("0.70");

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
        var monthlyLoanInsurance = monthlyLoanInsurance(loanAmount, request);
        var monthlyDebtService = monthlyMortgage.add(monthlyLoanInsurance);
        var annualRent = effectiveAnnualRent(deal, request.vacancyRate());
        var operatingExpenses = annualOperatingExpenses(deal, annualRent, request);
        var firstYearInterest = firstYearInterest(loanAmount, request.interestRate(), monthlyMortgage);
        var firstYearFinancingCosts = firstYearInterest.add(monthlyLoanInsurance.multiply(TWELVE));
        var taxAnnual = annualTax(deal, request, annualRent, operatingExpenses, firstYearFinancingCosts);
        var annualCashFlow = annualRent
                .subtract(operatingExpenses)
                .subtract(monthlyDebtService.multiply(TWELVE))
                .subtract(taxAnnual);
        var monthlyCashFlow = annualCashFlow.divide(TWELVE, 2, RoundingMode.HALF_UP);
        var grossYield = percentage(deal.getMonthlyRent().multiply(TWELVE), totalProjectCost);
        var netYield = percentage(annualRent.subtract(operatingExpenses), totalProjectCost);
        var breakEvenRent = breakEvenRent(deal, request, monthlyDebtService, firstYearFinancingCosts);
        var taxComparison = buildTaxComparison(deal, request, annualRent, operatingExpenses, firstYearFinancingCosts, monthlyDebtService, totalProjectCost);
        var debtEffortRatio = debtEffortRatio(deal, request, monthlyDebtService);

        var projection = buildProjection(deal, request, loanAmount, monthlyMortgage, monthlyLoanInsurance);
        var irr = calculateIrr(request, projection);
        var npv = calculateNpv(request, projection);

        return new SimulationResponse(
                money(totalProjectCost), money(loanAmount), money(monthlyMortgage), money(monthlyCashFlow),
                percent(grossYield), percent(netYield), money(taxAnnual), money(operatingExpenses),
                money(breakEvenRent), cashFlowStatus(monthlyCashFlow),
                projection,
                taxComparison, debtEffortRatio,
                irr == null ? null : percent(irr), money(npv), money(monthlyLoanInsurance));
    }

    /**
     * Taux d'effort au sens du HCSF : mensualités (assurance comprise) et crédits en cours rapportés aux
     * revenus nets du foyer, augmentés de 70 % du loyer attendu comme le retiennent la plupart des banques.
     * Sans revenu renseigné, le ratio n'est pas calculé.
     */
    private static @Nullable BigDecimal debtEffortRatio(Deal deal, SimulationRequest request, BigDecimal monthlyDebtService) {
        var income = request.monthlyNetIncome();
        if (income == null) {
            return null;
        }
        var existingDebt = request.existingMonthlyDebt() == null ? BigDecimal.ZERO : request.existingMonthlyDebt();
        var retainedIncome = income.add(deal.getMonthlyRent().multiply(BANK_RENT_RETENTION));
        if (retainedIncome.signum() <= 0) {
            return null;
        }
        return monthlyDebtService.add(existingDebt).multiply(HUNDRED)
                .divide(retainedIncome, 1, RoundingMode.HALF_UP);
    }

    /**
     * Loyer mensuel brut (avant vacance) qui ramène le cash-flow après impôt de la première année à zéro.
     * L'impôt dépend du loyer : la recherche se fait par dichotomie plutôt qu'en réutilisant l'impôt du loyer actuel.
     */
    private BigDecimal breakEvenRent(
            Deal deal, SimulationRequest request, BigDecimal monthlyDebtService, BigDecimal financingCosts) {
        double low = 0;
        double high = Math.max(deal.getMonthlyRent().doubleValue() * 4, 1000);
        for (int iteration = 0; iteration < 60; iteration++) {
            double candidate = (low + high) / 2;
            if (cashFlowForRent(deal, request, BigDecimal.valueOf(candidate), monthlyDebtService, financingCosts).signum() >= 0) {
                high = candidate;
            } else {
                low = candidate;
            }
        }
        return BigDecimal.valueOf(high);
    }

    private BigDecimal cashFlowForRent(
            Deal deal, SimulationRequest request, BigDecimal monthlyRent,
            BigDecimal monthlyDebtService, BigDecimal financingCosts) {
        var annualRent = monthlyRent.multiply(TWELVE).multiply(BigDecimal.ONE.subtract(rate(request.vacancyRate())));
        var expenses = annualOperatingExpenses(deal, annualRent, request);
        var tax = annualTax(deal, request, annualRent, expenses, financingCosts);
        return annualRent.subtract(expenses).subtract(monthlyDebtService.multiply(TWELVE)).subtract(tax);
    }

    private static BigDecimal monthlyLoanInsurance(BigDecimal loanAmount, SimulationRequest request) {
        var insuranceRate = request.loanInsuranceRate();
        if (insuranceRate == null || loanAmount.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return loanAmount.multiply(rate(insuranceRate)).divide(TWELVE, 8, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateIrr(SimulationRequest request, List<ProjectionPoint> projection) {
        if (projection.isEmpty() || request.downpayment().signum() == 0) return null;
        int n = projection.size();
        double initialEquity = request.downpayment().doubleValue();

        double[] cf = new double[n + 1];
        for (int t = 1; t <= n; t++) {
            var pt = projection.get(t - 1);
            if (t < n) {
                cf[t] = pt.annualCashFlow().doubleValue();
            } else {
                // Exit year: annual cash flow + terminal equity (property value - remaining debt)
                double terminalEquity = pt.estimatedPropertyValue().doubleValue() - pt.remainingLoan().doubleValue();
                cf[t] = pt.annualCashFlow().doubleValue() + Math.max(0.0, terminalEquity);
            }
        }

        // Newton-Raphson iteration for IRR
        double r = 0.08;
        for (int iter = 0; iter < 50; iter++) {
            double f = -initialEquity;
            double df = 0.0;
            for (int t = 1; t <= n; t++) {
                double denom = Math.pow(1.0 + r, t);
                f += cf[t] / denom;
                df += (-t * cf[t]) / (denom * (1.0 + r));
            }
            if (Math.abs(f) < 1e-4) {
                break;
            }
            if (Math.abs(df) < 1e-8) {
                break;
            }
            double nextR = r - (f / df);
            if (Double.isNaN(nextR) || Double.isInfinite(nextR)) {
                break;
            }
            nextR = Math.max(-0.5, Math.min(3.0, nextR));
            if (Math.abs(nextR - r) < 1e-5) {
                r = nextR;
                break;
            }
            r = nextR;
        }
        double residual = -initialEquity;
        for (int t = 1; t <= n; t++) residual += cf[t] / Math.pow(1.0 + r, t);
        return Math.abs(residual) > 1.0 ? null : BigDecimal.valueOf(r * 100.0);
    }

    private BigDecimal calculateNpv(SimulationRequest request, List<ProjectionPoint> projection) {
        if (projection == null || projection.isEmpty()) {
            return BigDecimal.ZERO;
        }
        int n = projection.size();
        double initialEquity = request.downpayment().doubleValue();

        double discountRate = 0.04; // 4% hurdle rate benchmark
        double npv = -initialEquity;
        for (int t = 1; t <= n; t++) {
            var pt = projection.get(t - 1);
            double cf = (t < n)
                    ? pt.annualCashFlow().doubleValue()
                    : pt.annualCashFlow().doubleValue() + Math.max(0.0, pt.estimatedPropertyValue().doubleValue() - pt.remainingLoan().doubleValue());
            npv += cf / Math.pow(1.0 + discountRate, t);
        }
        return BigDecimal.valueOf(npv);
    }

    private List<TaxComparisonItem> buildTaxComparison(
            Deal deal,
            SimulationRequest request,
            BigDecimal annualRent,
            BigDecimal operatingExpenses,
            BigDecimal firstYearInterest,
            BigDecimal monthlyDebtService,
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
            var tempRequest = request.withTaxRegime(regime);
            var tax = annualTax(deal, tempRequest, annualRent, operatingExpenses, firstYearInterest);
            var cashFlow = annualRent.subtract(operatingExpenses)
                    .subtract(monthlyDebtService.multiply(TWELVE))
                    .subtract(tax)
                    .divide(TWELVE, 2, RoundingMode.HALF_UP);
            if (cashFlow.compareTo(bestCashFlow) > 0) {
                bestCashFlow = cashFlow;
                bestRegime = regime;
            }
        }

        var list = new ArrayList<TaxComparisonItem>();
        for (var regime : regimes) {
            var tempRequest = request.withTaxRegime(regime);
            var tax = annualTax(deal, tempRequest, annualRent, operatingExpenses, firstYearInterest);
            var cashFlow = annualRent.subtract(operatingExpenses)
                    .subtract(monthlyDebtService.multiply(TWELVE))
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
                case REEL_LMNP -> "Hypothèse simplifiée d'amortissement et de déduction : à confirmer selon votre situation";
                case MICRO_BIC -> "Hypothèse forfaitaire sur les loyers : vérifier votre éligibilité et le taux applicable";
                case NU -> "Hypothèse micro-foncier, sans calcul de déficit foncier";
                case SCI_IS -> "Hypothèse d'impôt société, hors fiscalité de distribution et de revente";
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
            BigDecimal monthlyMortgage,
            BigDecimal monthlyLoanInsurance) {
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

            var annualLoanInsurance = monthlyLoanInsurance.multiply(TWELVE);
            var tax = annualTax(deal, request, annualRent, operatingExpenses, annualInterest.add(annualLoanInsurance));
            var annualCashFlow = annualRent.subtract(operatingExpenses)
                    .subtract(monthlyMortgage.multiply(TWELVE)).subtract(annualLoanInsurance).subtract(tax);
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
