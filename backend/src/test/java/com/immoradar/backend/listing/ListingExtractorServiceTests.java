package com.immoradar.backend.listing;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class ListingExtractorServiceTests {

    private final ListingExtractorService service = new ListingExtractorService();

    @Test
    void extractsLeboncoinListingCorrectly() {
        var result = service.extract("https://www.leboncoin.fr/ad/ventes_immobilieres/3271114816");

        assertThat(result.platform()).isEqualTo("Leboncoin");
        assertThat(result.title()).isEqualTo("Maison 3 pièces 74 m²");
        assertThat(result.location()).contains("Le Havre");
        assertThat(result.propertyType()).isEqualTo("House");
        assertThat(result.price()).isEqualByComparingTo(new BigDecimal("180000"));
        assertThat(result.surface()).isEqualByComparingTo(new BigDecimal("74"));
        assertThat(result.monthlyRent()).isEqualByComparingTo(new BigDecimal("950"));
        assertThat(result.renovationCost()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void extractsSeLogerListingCorrectly() {
        var result = service.extract("https://www.seloger.com/annonces/achat/appartement/paris-11eme-75/2149503.htm");

        assertThat(result.platform()).isEqualTo("SeLoger");
        assertThat(result.propertyType()).isEqualTo("Studio");
        assertThat(result.location()).contains("Paris");
        assertThat(result.price()).isEqualByComparingTo(new BigDecimal("195000"));
    }

    @Test
    void extractsPapListingCorrectly() {
        var result = service.extract("https://www.pap.fr/annonces/appartement-bordeaux-33000-r439201");

        assertThat(result.platform()).isEqualTo("PAP");
        assertThat(result.location()).contains("Bordeaux");
        assertThat(result.price()).isEqualByComparingTo(new BigDecimal("178000"));
    }

    @Test
    void handlesUnknownUrlWithResilientFallback() {
        var result = service.extract("https://mon-agence-locale.fr/annonce-12345");

        assertThat(result).isNotNull();
        assertThat(result.title()).isNotEmpty();
        assertThat(result.price()).isGreaterThan(BigDecimal.ZERO);
        assertThat(result.surface()).isGreaterThan(BigDecimal.ZERO);
    }
}
