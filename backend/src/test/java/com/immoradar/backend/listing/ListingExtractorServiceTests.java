package com.immoradar.backend.listing;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.URI;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ListingExtractorServiceTests {

    private static final ListingPageFetcher UNREACHABLE = uri -> Optional.empty();

    @Test
    void returnsTheDemoListingOnlyForTheExactDemoUrl() {
        var result = new ListingExtractorService(UNREACHABLE).extract(ListingExtractorService.DEMO_LEBONCOIN);

        assertThat(result.demo()).isTrue();
        assertThat(result.platform()).isEqualTo("Leboncoin");
        assertThat(result.location()).contains("Le Havre");
        assertThat(result.price()).isEqualByComparingTo(new BigDecimal("180000"));
        assertThat(result.warnings()).anyMatch(warning -> warning.contains("démonstration"));
    }

    @Test
    void neverInventsFiguresWhenThePortalCannotBeRead() {
        var result = new ListingExtractorService(UNREACHABLE)
                .extract("https://www.leboncoin.fr/ad/ventes_immobilieres/9999999999");

        assertThat(result.demo()).isFalse();
        assertThat(result.platform()).isEqualTo("Leboncoin");
        assertThat(result.price()).isNull();
        assertThat(result.surface()).isNull();
        assertThat(result.monthlyRent()).isNull();
        assertThat(result.location()).isNull();
        assertThat(result.extractedFields()).isEmpty();
        assertThat(result.warnings()).anyMatch(warning -> warning.contains("n’a pas pu être lue"));
    }

    @Test
    void readsOpenGraphMetadataAndJsonLdPrice() {
        var html = """
                <html><head>
                <meta property="og:title" content="Appartement T3 65 m² - Angers (49000)" />
                <meta content="Bel appartement lumineux, 3 pièces, proche gare &amp; commerces" property="og:description">
                <meta property="og:image" content="https://cdn.example.fr/photo.jpg" />
                <script type="application/ld+json">{"@type":"Offer","price":"189000","priceCurrency":"EUR"}</script>
                </head></html>
                """;
        var result = new ListingExtractorService(uri -> Optional.of(html))
                .extract("https://www.exemple-agence.fr/annonce/123");

        assertThat(result.title()).isEqualTo("Appartement T3 65 m² - Angers (49000)");
        assertThat(result.description()).contains("proche gare & commerces");
        assertThat(result.price()).isEqualByComparingTo("189000");
        assertThat(result.surface()).isEqualByComparingTo("65");
        assertThat(result.location()).isEqualTo("Angers (49000)");
        assertThat(result.propertyType()).isEqualTo("Apartment");
        assertThat(result.imageUrl()).isEqualTo("https://cdn.example.fr/photo.jpg");
        assertThat(result.monthlyRent()).isNull();
        assertThat(result.extractedFields()).contains("price", "surface", "location").doesNotContain("monthlyRent");
        assertThat(result.platform()).isEqualTo("exemple-agence.fr");
    }

    @Test
    void readsPriceWithThousandsSeparatorsFromTheTitle() {
        var html = "<meta property=\"og:title\" content=\"Maison 4 pièces 92,5 m² 215 000 €\">";
        var result = new ListingExtractorService(uri -> Optional.of(html)).extract("https://www.pap.fr/annonces/x");

        assertThat(result.price()).isEqualByComparingTo("215000");
        assertThat(result.surface()).isEqualByComparingTo("92.5");
        assertThat(result.propertyType()).isEqualTo("House");
    }

    @Test
    void readsTheCityAndPostcodeFromTheUrlWhenThePageIsBlocked() {
        var result = new ListingExtractorService(UNREACHABLE)
                .extract("https://www.pap.fr/annonces/appartement-saint-etienne-42000-r439201");

        assertThat(result.location()).isEqualTo("Saint-Etienne (42000)");
        assertThat(result.propertyType()).isEqualTo("Apartment");
        assertThat(result.price()).isNull();
    }

    @Test
    void rejectsNonHttpUrls() {
        var service = new ListingExtractorService(UNREACHABLE);

        assertThatThrownBy(() -> service.extract("file:///etc/passwd")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.extract("pas une url")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void fetcherRefusesInternalNetworkTargets() throws Exception {
        assertThat(SafeHttpPageFetcher.isPublicHttpTarget(URI.create("http://localhost:8080/actuator"))).isFalse();
        assertThat(SafeHttpPageFetcher.isPublicHttpTarget(URI.create("http://169.254.169.254/latest/meta-data"))).isFalse();
        assertThat(SafeHttpPageFetcher.isPublicHttpTarget(URI.create("http://10.0.0.12/"))).isFalse();
        assertThat(SafeHttpPageFetcher.isPublicHttpTarget(URI.create("http://192.168.1.1/"))).isFalse();
        assertThat(SafeHttpPageFetcher.isPublicHttpTarget(URI.create("ftp://example.com/"))).isFalse();
        assertThat(SafeHttpPageFetcher.isPublicAddress(InetAddress.getByName("100.64.1.1"))).isFalse();
        assertThat(SafeHttpPageFetcher.isPublicAddress(InetAddress.getByName("fd00::1"))).isFalse();
        assertThat(SafeHttpPageFetcher.isPublicAddress(InetAddress.getByName("93.184.216.34"))).isTrue();
    }
}
