package com.immoradar.backend.deal;

import org.jspecify.annotations.Nullable;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Radar score sur 10 : il résume ce qu'un investisseur regarde pour repérer une bonne affaire et explique
 * chaque point attribué.
 *
 * <ul>
 *   <li>Prix par rapport au marché (3 pts) : décote du prix au m² face à la médiane des ventes du secteur.</li>
 *   <li>Rendement brut (3 pts) : loyer annuel rapporté au coût d'achat (prix, notaire 7,5 %, travaux).</li>
 *   <li>Cash-flow (2 pts) : cash-flow mensuel avec un financement type (100 % sur 20 ans à 3,5 %).</li>
 *   <li>Levier de négociation (1 pt) : baisse de prix déjà constatée ou annonce en ligne depuis longtemps.</li>
 *   <li>Énergie (1 pt) : classe DPE ; une passoire F ou G retire des points car sa location est ou sera interdite.</li>
 * </ul>
 *
 * <p>Un critère sans donnée (pas de référence DVF, DPE inconnu) rapporte 0 point au lieu d'une valeur neutre
 * inventée : le score ne monte qu'avec des faits vérifiables.
 */
public final class DealScoring {

    static final double REFERENCE_RATE = 3.5;
    static final int REFERENCE_YEARS = 20;
    static final double NOTARY_FEES = 0.075;
    static final double VACANCY = 0.05;

    private DealScoring() {}

    public static List<ScoreFactor> breakdown(Deal deal, LocalDate today) {
        var factors = new ArrayList<ScoreFactor>();
        factors.add(marketFactor(deal.getMarketDeltaPercent()));
        factors.add(yieldFactor(deal));
        factors.add(cashFlowFactor(deal));
        factors.add(negotiationFactor(deal, today));
        factors.add(energyFactor(deal.getEnergyClass()));
        return factors;
    }

    public static double score(List<ScoreFactor> factors) {
        var total = factors.stream().mapToDouble(ScoreFactor::points).sum();
        return Math.round(clamp(total, 0, 10) * 10) / 10.0;
    }

    /** Cash-flow mensuel avec le financement de référence, avant impôt. */
    public static double referenceMonthlyCashFlow(Deal deal) {
        var price = deal.getPrice().doubleValue();
        var loan = price * (1 + NOTARY_FEES) + deal.getRenovationCost().doubleValue();
        var monthlyRate = REFERENCE_RATE / 1200;
        var months = REFERENCE_YEARS * 12;
        var mortgage = loan * monthlyRate / (1 - Math.pow(1 + monthlyRate, -months));
        return deal.getMonthlyRent().doubleValue() * (1 - VACANCY)
                - deal.getMonthlyCharges().doubleValue()
                - deal.getPropertyTax().doubleValue() / 12
                - mortgage;
    }

    /** Rendement brut sur le coût total d'acquisition, en %. */
    public static double grossYieldOnCost(Deal deal) {
        var cost = deal.getPrice().doubleValue() * (1 + NOTARY_FEES) + deal.getRenovationCost().doubleValue();
        return deal.getMonthlyRent().doubleValue() * 1200 / cost;
    }

    public static @Nullable Long daysOnMarket(Deal deal, LocalDate today) {
        return deal.getListedOn() == null ? null : ChronoUnit.DAYS.between(deal.getListedOn(), today);
    }

    /** Alertes bloquantes ou à vérifier avant de faire une offre. */
    public static List<String> alerts(Deal deal, LocalDate today) {
        var alerts = new ArrayList<String>();
        var energy = deal.getEnergyClass();
        if (energy != null && energy.rentalBanYear() > 0) {
            var year = energy.rentalBanYear();
            alerts.add(year <= today.getYear()
                    ? "DPE " + energy + " : location interdite pour tout nouveau bail depuis " + year
                            + ". Prévois une rénovation énergétique avant de louer."
                    : "DPE " + energy + " : location interdite à partir de " + year
                            + (energy == EnergyClass.F ? ", loyers déjà gelés." : "."));
        }
        if (energy == null) {
            alerts.add("DPE non renseigné : vérifie-le, une classe F ou G change tout le calcul.");
        }
        if (grossYieldOnCost(deal) < 4) {
            alerts.add("Rendement brut sous 4 % : le crédit sera difficile à couvrir par le loyer.");
        }
        return alerts;
    }

    private static ScoreFactor marketFactor(@Nullable Double delta) {
        if (delta == null) {
            return new ScoreFactor("market", "Prix vs marché", 0, 3, "Critère non noté : aucune référence DVF fiable pour ce secteur.");
        }
        // +10 % au-dessus du marché : 0 pt ; 20 % en dessous : 3 pts
        var points = round(clamp((10 - delta) / 30 * 3, 0, 3));
        var detail = delta <= 0
                ? String.format(Locale.FRENCH, "%.1f %% sous le prix médian du secteur.", -delta)
                : String.format(Locale.FRENCH, "%.1f %% au-dessus du prix médian du secteur.", delta);
        return new ScoreFactor("market", "Prix vs marché", points, 3, detail);
    }

    private static ScoreFactor yieldFactor(Deal deal) {
        var yield = grossYieldOnCost(deal);
        // 4 % : 0 pt ; 10 % : 3 pts
        var points = round(clamp((yield - 4) / 6 * 3, 0, 3));
        return new ScoreFactor("yield", "Rendement brut", points, 3, String.format(Locale.FRENCH,
                "%.1f %% sur le coût total (prix, notaire et travaux).", yield));
    }

    private static ScoreFactor cashFlowFactor(Deal deal) {
        var cashFlow = referenceMonthlyCashFlow(deal);
        // −200 €/mois : 0 pt ; +200 €/mois : 2 pts
        var points = round(clamp((cashFlow + 200) / 400 * 2, 0, 2));
        return new ScoreFactor("cashflow", "Cash-flow", points, 2, String.format(Locale.FRENCH,
                "%+.0f €/mois avant impôt, financé à 100 %% sur %d ans à %.1f %%.",
                cashFlow, REFERENCE_YEARS, REFERENCE_RATE));
    }

    private static ScoreFactor negotiationFactor(Deal deal, LocalDate today) {
        var drop = deal.getPriceDropPercent() == null ? 0 : deal.getPriceDropPercent();
        var days = daysOnMarket(deal, today);
        // 10 % de baisse : 1 pt ; une annonce de plus de 90 jours vaut 0,5 pt
        var points = clamp(drop / 10, 0, 1);
        if (days != null && days > 90) {
            points = Math.max(points, 0.5);
        }
        var parts = new ArrayList<String>();
        if (drop > 0) parts.add(String.format(Locale.FRENCH, "prix déjà baissé de %.1f %%", drop));
        if (days != null) parts.add("en ligne depuis " + days + " jours");
        var detail = parts.isEmpty()
                ? "Aucun signal : renseigne la date de mise en ligne et les changements de prix."
                : capitalize(String.join(", ", parts)) + ".";
        return new ScoreFactor("negotiation", "Levier de négociation", round(points), 1, detail);
    }

    private static ScoreFactor energyFactor(@Nullable EnergyClass energy) {
        if (energy == null) {
            return new ScoreFactor("energy", "DPE", 0, 1, "Critère non noté : DPE non renseigné.");
        }
        return switch (energy) {
            case A, B, C -> new ScoreFactor("energy", "DPE", 1, 1, "Classe " + energy + " : aucune contrainte de location.");
            case D -> new ScoreFactor("energy", "DPE", 0.75, 1, "Classe D : aucune échéance d'interdiction.");
            case E -> new ScoreFactor("energy", "DPE", 0.25, 1, "Classe E : location interdite à partir de 2034.");
            case F -> new ScoreFactor("energy", "DPE", -1, 1, "Classe F : loyers gelés, location interdite en 2028.");
            case G -> new ScoreFactor("energy", "DPE", -2, 1, "Classe G : location interdite depuis 2025 sans rénovation.");
        };
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double round(double value) {
        return Math.round(value * 100) / 100.0;
    }

    private static String capitalize(String text) {
        return text.isEmpty() ? text : Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }
}
