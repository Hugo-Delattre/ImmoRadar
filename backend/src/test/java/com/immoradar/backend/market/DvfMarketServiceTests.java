package com.immoradar.backend.market;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class DvfMarketServiceTests {

    private DvfMarketService dvfMarketService;

    @BeforeEach
    void setUp() {
        dvfMarketService = new DvfMarketService();
    }

    @Test
    @DisplayName("Identifie correctement un bien sous-évalué par rapport aux données réelles DVF")
    void shouldIdentifyUndervaluedDeal() {
        // Saint-Étienne (médiane DVF : 1450 €/m²)
        // Bien à 100 000 € pour 100 m² = 1000 €/m² (-31%)
        DvfMarketAnalysis analysis = dvfMarketService.analyze(
                "Saint-Étienne (42)", new BigDecimal("100000"), new BigDecimal("100"));

        assertThat(analysis.marketStatus()).isEqualTo("SOUS_EVALUE");
        assertThat(analysis.dvfMedianPricePerSquareMeter()).isEqualByComparingTo("1450");
        assertThat(analysis.dealPricePerSquareMeter()).isEqualByComparingTo("1000");
        assertThat(analysis.deltaPercentage()).isLessThan(BigDecimal.ZERO);
        assertThat(analysis.suggestedOfferPrice()).isEqualByComparingTo("100000");
        assertThat(analysis.advice()).contains("Excellente opportunité");
    }

    @Test
    @DisplayName("Identifie un bien surévalué et calcule la marge de négociation recommandée")
    void shouldIdentifyOvervaluedDealAndCalculateNegotiationMargin() {
        // Limoges (médiane DVF : 1650 €/m²)
        // Bien à 200 000 € pour 80 m² = 2500 €/m² (+51.5%)
        DvfMarketAnalysis analysis = dvfMarketService.analyze(
                "Limoges (87)", new BigDecimal("2500"), new BigDecimal("200000"), new BigDecimal("80")
        );

        assertThat(analysis.marketStatus()).isEqualTo("SUREVALUE");
        assertThat(analysis.deltaPercentage()).isGreaterThan(new BigDecimal("4.0"));
        assertThat(analysis.negotiationMargin()).isGreaterThan(BigDecimal.ZERO);
        assertThat(analysis.suggestedOfferPrice()).isLessThan(new BigDecimal("200000"));
        assertThat(analysis.advice()).contains("Marge de négociation");
    }

    @Test
    @DisplayName("Gère correctement les biens au prix du marché (alignés)")
    void shouldIdentifyAlignedMarketDeal() {
        // Mulhouse (médiane DVF : 1380 €/m²)
        // Bien à 1390 €/m²
        DvfMarketAnalysis analysis = dvfMarketService.analyze(
                "Mulhouse (68)", new BigDecimal("1390"), new BigDecimal("69500"), new BigDecimal("50")
        );

        assertThat(analysis.marketStatus()).isEqualTo("ALIGNE");
        assertThat(analysis.advice()).contains("aligné avec la médiane DVF");
    }
}
