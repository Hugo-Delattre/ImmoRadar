package com.immoradar.backend.listing;

import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ListingExtractorServiceTests {
    @Test
    void extractsOnlyValuesPresentInThePage() {
        var html = """
                <meta property="og:title" content="Maison 74 m² à vendre 180 000 €">
                <meta property="og:description" content="Jardin et garage">
                <meta property="og:image" content="https://images.example/photo.jpg">
                """;
        var result = ListingExtractorService.parsePage(URI.create("https://www.leboncoin.fr/ad/123"), html);
        assertThat(result.price()).isEqualByComparingTo("180000");
        assertThat(result.surface()).isEqualByComparingTo("74");
        assertThat(result.monthlyRent()).isNull();
        assertThat(result.location()).isNull();
        assertThat(result.sourceUrl()).isEqualTo("https://www.leboncoin.fr/ad/123");
    }

    @Test
    void refusesToInventMissingPricesOrSurfaces() {
        assertThatThrownBy(() -> ListingExtractorService.parsePage(
                URI.create("https://www.pap.fr/annonce"), "<meta property='og:title' content='Bel appartement'>"))
                .isInstanceOf(ListingExtractionException.class);
    }

    @Test
    void rejectsUnsafeOrUnrelatedHosts() {
        for (String url : new String[]{"http://www.leboncoin.fr/ad/1", "https://leboncoin.fr.evil.test/a",
                "https://127.0.0.1/a", "https://www.pap.fr:444/a", "https://user@pap.fr/a"}) {
            assertThatThrownBy(() -> ListingExtractorService.validateUrl(url))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
