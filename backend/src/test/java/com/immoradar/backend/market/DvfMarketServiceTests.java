package com.immoradar.backend.market;

import com.immoradar.backend.deal.DealService;
import com.immoradar.backend.deal.PropertyType;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DvfMarketServiceTests {
    private final DealService dealService = mock(DealService.class);
    private final MarketDataClient client = mock(MarketDataClient.class);
    private final DvfMarketService service = new DvfMarketService(dealService, client);

    @Test
    void computesDifferenceOnlyFromAttributedComparableSales() {
        when(client.findComparable(eq("Saint-Étienne (42)"), eq("Appartement"), any()))
                .thenReturn(Optional.of(new MarketDataClient.ComparableMarket("Saint-Étienne", "42218",
                        new BigDecimal("1200"), 1551, 2025, "Forte",
                        "https://foncierdata.fr/api/v1/commune/42218.json",
                        "https://foncierdata.fr/methodologie")));
        when(client.findRecentSales(eq("42218"), eq("Appartement"), any(), eq(2025)))
                .thenReturn(List.of(new RecentSale(LocalDate.of(2025, 12, 30), "Appartement",
                        new BigDecimal("50"), new BigDecimal("75000"), new BigDecimal("1500"))));

        var result = service.analyze("Saint-Étienne (42)", new BigDecimal("1500"),
                PropertyType.APARTMENT, new BigDecimal("50"));

        assertThat(result.available()).isTrue();
        assertThat(result.deltaPercentage()).isEqualByComparingTo("25.0");
        assertThat(result.comparableCount()).isEqualTo(1551);
        assertThat(result.sourceUrl()).contains("42218");
        assertThat(result.recentSales()).hasSize(1);
        assertThat(result.recentSalesSourceUrl()).endsWith("/42218/transactions.json");
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

    @Test
    void acceptsOnlyRecentSalesMatchingTheActualCommuneTypeAndSurface() {
        var realClient = new MarketDataClient(JsonMapper.builder().build());
        var response = JsonMapper.builder().build().readTree("""
                {"code_insee":"87085","annee":2025,"transactions":[
                  {"date":"30/12/2025","type_local":"Appartement","surface_m2":77,"prix_eur":159600,"prix_m2_eur":2073},
                  {"date":"30/12/2025","type_local":"Maison","surface_m2":80,"prix_eur":140000,"prix_m2_eur":1750},
                  {"date":"30/12/2025","type_local":"Appartement","surface_m2":20,"prix_eur":50000,"prix_m2_eur":2500},
                  {"date":"31/02/2025","type_local":"Appartement","surface_m2":78,"prix_eur":100000,"prix_m2_eur":1282}]}
                """);

        var sales = realClient.parseRecentSales(response, "87085", "Appartement", new BigDecimal("78"), 2025);

        assertThat(sales).hasSize(1);
        assertThat(sales.getFirst().price()).isEqualByComparingTo("159600");
        assertThat(realClient.parseRecentSales(response, "99999", "Appartement", new BigDecimal("78"), 2025)).isEmpty();
    }
}
