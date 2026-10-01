package com.immoradar.backend.deal;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class DealScoringTests {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 1);

    @Test
    void rewardsAPropertyBelowTheMarketWithStrongYield() {
        var cheap = deal("120000", "1100");
        cheap.setMarketDeltaPercent(-18.0);
        cheap.setEnergyClass(EnergyClass.C);
        var expensive = deal("120000", "1100");
        expensive.setMarketDeltaPercent(12.0);
        expensive.setEnergyClass(EnergyClass.C);

        assertThat(score(cheap)).isGreaterThan(score(expensive));
        assertThat(factor(cheap, "market").points()).isGreaterThan(2.5);
        assertThat(factor(expensive, "market").points()).isZero();
    }

    @Test
    void penalisesEnergySievesAndWarnsAboutTheRentalBan() {
        var sieve = deal("120000", "1100");
        sieve.setEnergyClass(EnergyClass.G);
        var efficient = deal("120000", "1100");
        efficient.setEnergyClass(EnergyClass.B);

        assertThat(score(efficient) - score(sieve)).isGreaterThanOrEqualTo(3.0);
        assertThat(DealScoring.alerts(sieve, TODAY)).anyMatch(alert -> alert.contains("interdite pour tout nouveau bail depuis 2025"));
        assertThat(DealScoring.alerts(efficient, TODAY)).noneMatch(alert -> alert.startsWith("DPE"));
    }

    @Test
    void aPriceDropOrAStaleListingIsANegotiationLever() {
        var dropped = deal("200000", "1100");
        dropped.recordPrice(new BigDecimal("180000"), TODAY);
        dropped.refreshDerivedValues();
        var stale = deal("200000", "1100");
        stale.setListedOn(TODAY.minusDays(150));

        assertThat(dropped.getPriceDropPercent()).isEqualTo(10.0);
        assertThat(factor(dropped, "negotiation").points()).isEqualTo(1.0);
        assertThat(factor(stale, "negotiation").points()).isEqualTo(0.5);
        assertThat(factor(stale, "negotiation").detail()).contains("150 jours");
    }

    @Test
    void scoreStaysBetweenZeroAndTen() {
        var terrible = deal("400000", "600");
        terrible.setMarketDeltaPercent(40.0);
        terrible.setEnergyClass(EnergyClass.G);

        assertThat(score(terrible)).isZero();
        assertThat(DealScoring.breakdown(terrible, TODAY)).hasSize(5);
    }

    private static double score(Deal deal) {
        return DealScoring.score(DealScoring.breakdown(deal, TODAY));
    }

    private static ScoreFactor factor(Deal deal, String key) {
        return DealScoring.breakdown(deal, TODAY).stream().filter(f -> f.key().equals(key)).findFirst().orElseThrow();
    }

    private static Deal deal(String price, String rent) {
        var deal = new Deal("d", "Bien", new BigDecimal(price), new BigDecimal(rent), new BigDecimal("80"),
                new BigDecimal("900"), BigDecimal.ZERO, "Limoges (87)", new BigDecimal("60"),
                PropertyType.APARTMENT, "", 0, "", false);
        deal.setCreatedOn(TODAY);
        deal.refreshDerivedValues();
        return deal;
    }
}
