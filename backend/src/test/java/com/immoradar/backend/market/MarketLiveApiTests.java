package com.immoradar.backend.market;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.Year;

import static org.assertj.core.api.Assertions.assertThat;

/** Opt-in smoke test: it makes real HTTPS calls and is intentionally not part of offline CI. */
class MarketLiveApiTests {

    @Test
    @EnabledIfEnvironmentVariable(named = "IMMORADAR_LIVE_API_TEST", matches = "true")
    void readsActualGeoAggregateAndTransactionsEndpoints() {
        var client = new MarketDataClient(JsonMapper.builder().build());
        var market = client.findComparable("Limoges (87)", "Appartement", new BigDecimal("78"));

        assertThat(market).isPresent();
        var comparable = market.orElseThrow();
        assertThat(comparable.codeInsee()).isEqualTo("87085");
        assertThat(comparable.referenceYear()).isBetween(Year.now().getValue() - 3, Year.now().getValue());
        assertThat(comparable.comparableCount()).isGreaterThanOrEqualTo(20);
        assertThat(comparable.medianPricePerSquareMeter()).isPositive();

        var sales = client.findRecentSales("87085", "Appartement", new BigDecimal("78"), comparable.referenceYear());
        assertThat(sales).isNotEmpty();
        assertThat(sales).allSatisfy(sale -> {
            assertThat(sale.propertyCategory()).isEqualTo("Appartement");
            assertThat(sale.surface()).isBetween(new BigDecimal("62.40"), new BigDecimal("93.60"));
            assertThat(sale.price()).isPositive();
        });
    }
}
