package com.immoradar.backend.market;

import com.immoradar.backend.deal.PropertyType;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.Year;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DvfMarketServiceTests {
    private final MarketDataClient client = mock(MarketDataClient.class);
    private final DvfMarketService service = new DvfMarketService(client);

    @Test
    void computesDifferenceOnlyFromAttributedComparableSales() {
        when(client.findComparable(eq("Saint-Étienne (42)"), eq("Appartement"), any()))
                .thenReturn(Optional.of(new MarketDataClient.ComparableMarket("Saint-Étienne", "42218",
                        new BigDecimal("1200"), 1551, 2025, "Forte",
                        "https://foncierdata.fr/api/v1/commune/42218.json",
                        "https://foncierdata.fr/methodologie")));

        var result = service.analyze("Saint-Étienne (42)", new BigDecimal("1500"),
                PropertyType.APARTMENT, new BigDecimal("50"));

        assertThat(result.available()).isTrue();
        assertThat(result.deltaPercentage()).isEqualByComparingTo("25.0");
        assertThat(result.comparableCount()).isEqualTo(1551);
        assertThat(result.sourceUrl()).contains("42218");
    }

    @Test
    void failsClosedForWholeBuildingsAndUnavailableData() {
        var building = service.analyze("Saint-Étienne (42)", new BigDecimal("1500"),
                PropertyType.BUILDING, new BigDecimal("140"));
        assertThat(building.available()).isFalse();
        assertThat(building.medianPricePerSquareMeter()).isNull();

        when(client.findComparable(any(), any(), any())).thenReturn(Optional.empty());
        var unavailable = service.analyze("Commune inconnue", new BigDecimal("1500"),
                PropertyType.HOUSE, new BigDecimal("80"));
        assertThat(unavailable.available()).isFalse();
        assertThat(unavailable.deltaPercentage()).isNull();
    }

    @Test
    void rejectsThinOrMismatchedUpstreamSamples() {
        var realClient = new MarketDataClient(JsonMapper.builder().build());
        var mapper = JsonMapper.builder().build();
        var thin = mapper.readTree("""
                {"code_insee":"42218","nom":"Saint-Étienne","annee_reference":2025,
                 "tranches_surface":{"Appartement":[{"band":"30-60 m²","median":1202,"n":3}]}}
                """);
        assertThat(realClient.parseMarket(thin, "42218", "Appartement", new BigDecimal("45"))).isEmpty();
        assertThat(realClient.parseMarket(thin, "99999", "Appartement", new BigDecimal("45"))).isEmpty();

        var stale = mapper.readTree("""
                {"code_insee":"42218","nom":"Saint-Étienne","annee_reference":%d,
                 "tranches_surface":{"Appartement":[{"band":"30-60 m²","median":1202,"n":50}]}}
                """.formatted(Year.now().getValue() - 4));
        assertThat(realClient.parseMarket(stale, "42218", "Appartement", new BigDecimal("45"))).isEmpty();
    }
}
