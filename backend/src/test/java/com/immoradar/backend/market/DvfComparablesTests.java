package com.immoradar.backend.market;

import com.immoradar.backend.deal.PropertyType;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class DvfComparablesTests {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-01T10:00:00Z"), ZoneOffset.UTC);
    private static final Commune LIMOGES = new Commune("87085", "Limoges", "87");

    @Test
    void parsesGeoDvfFilesAndSkipsMultiDwellingSales() throws Exception {
        var csv = """
                id_mutation,date_mutation,numero_disposition,nature_mutation,valeur_fonciere,adresse_numero,adresse_nom_voie,code_postal,code_commune,nom_commune,code_departement,type_local,surface_reelle_bati,nombre_pieces_principales
                2025-1,2025-03-04,000001,Vente,120000,12,"RUE DES LILAS, BAT A",87000,87085,Limoges,87,Appartement,60,3
                2025-1,2025-03-04,000001,Vente,120000,12,"RUE DES LILAS, BAT A",87000,87085,Limoges,87,Dépendance,,0
                2025-2,2025-05-10,000001,Vente,300000,3,AV FOCH,87000,87085,Limoges,87,Appartement,50,2
                2025-2,2025-05-10,000001,Vente,300000,3,AV FOCH,87000,87085,Limoges,87,Appartement,55,2
                2025-3,2025-06-01,000001,Vente en l'état futur d'achèvement,200000,1,RUE NEUVE,87000,87085,Limoges,87,Appartement,70,3
                2025-4,2025-07-01,000001,Vente,180000,8,RUE BASSE,87000,87085,Limoges,87,Maison,90,4
                """;
        var sales = DataGouvMarketClient.parseCsv(new BufferedReader(new StringReader(csv)));

        assertThat(sales).extracting(DvfSale::mutationId).containsExactlyInAnyOrder("2025-1", "2025-4");
        var flat = sales.stream().filter(sale -> sale.mutationId().equals("2025-1")).findFirst().orElseThrow();
        assertThat(flat.pricePerSquareMeter()).isEqualTo(2000.0);
        assertThat(flat.date()).isEqualTo(LocalDate.of(2025, 3, 4));
    }

    @Test
    void computesMedianAndQuartilesOnSameTypeSalesOfTheLastThreeYears() {
        var client = new FakeClient();
        for (int index = 0; index < 20; index++) {
            client.add(2025, new DvfSale("a" + index, LocalDate.of(2025, 1, 10), BigDecimal.valueOf(100_000 + index * 5_000), BigDecimal.valueOf(60), "Appartement"));
            client.add(2024, new DvfSale("m" + index, LocalDate.of(2024, 2, 10), BigDecimal.valueOf(400_000), BigDecimal.valueOf(100), "Maison"));
        }
        client.add(2021, new DvfSale("old", LocalDate.of(2021, 1, 1), BigDecimal.valueOf(10_000_000), BigDecimal.valueOf(60), "Appartement"));

        var stats = new DvfComparables(client, CLOCK, true)
                .forLocation("Limoges (87000)", PropertyType.APARTMENT, BigDecimal.valueOf(55))
                .orElseThrow();

        assertThat(stats.count()).isEqualTo(20);
        assertThat(stats.median()).isEqualTo(2458.333, org.assertj.core.data.Offset.offset(0.01));
        assertThat(stats.firstQuartile()).isLessThan(stats.median());
        assertThat(stats.thirdQuartile()).isGreaterThan(stats.median());
        assertThat(stats.scope()).startsWith("Appartements de 28 à 110 m²").endsWith("Limoges");
        assertThat(client.requestedYears).containsExactly(2025, 2024, 2023);
        assertThat(client.lastCode).isEqualTo("87000");
    }

    @Test
    void fallsBackToTheEstimateWhenTooFewSalesOrOffline() {
        var client = new FakeClient();
        client.add(2025, new DvfSale("a", LocalDate.of(2025, 1, 10), BigDecimal.valueOf(100_000), BigDecimal.valueOf(60), "Appartement"));
        var market = new DvfMarketService(new DvfComparables(client, CLOCK, true));

        var analysis = market.analyze("Limoges (87)", BigDecimal.valueOf(120_000), BigDecimal.valueOf(60), PropertyType.APARTMENT);

        assertThat(analysis.source()).isEqualTo("ESTIMATION");
        assertThat(analysis.scope()).isEqualTo("Barème indicatif du département 87");
        assertThat(analysis.periodStart()).isNull();
    }

    @Test
    void usesRealSalesWhenAvailableAndCachesThem() {
        var client = new FakeClient();
        for (int index = 0; index < 12; index++) {
            client.add(2025, new DvfSale("a" + index, LocalDate.of(2025, 1 + index % 6, 10), BigDecimal.valueOf(120_000), BigDecimal.valueOf(60), "Appartement"));
        }
        var market = new DvfMarketService(new DvfComparables(client, CLOCK, true));

        var analysis = market.analyze("Limoges (87)", BigDecimal.valueOf(96_000), BigDecimal.valueOf(60), PropertyType.APARTMENT);
        market.analyze("Limoges (87)", BigDecimal.valueOf(96_000), BigDecimal.valueOf(60), PropertyType.APARTMENT);

        assertThat(analysis.source()).isEqualTo("DVF");
        assertThat(analysis.dvfMedianPricePerSquareMeter()).isEqualByComparingTo("2000");
        assertThat(analysis.deltaPercentage()).isEqualByComparingTo("-20.0");
        assertThat(analysis.marketStatus()).isEqualTo("SOUS_EVALUE");
        assertThat(analysis.transactionsCount5Years()).isEqualTo(12);
        assertThat(analysis.scope()).startsWith("12 ventes");
        assertThat(analysis.averageSaleDelayDays()).isNull();
        assertThat(client.communeLookups.get()).isEqualTo(1);
    }

    private static final class FakeClient implements MarketDataClient {
        private final List<Integer> requestedYears = new ArrayList<>();
        private final List<DvfSale>[] byYear = newYears();
        private final AtomicInteger communeLookups = new AtomicInteger();
        private String lastCode = "";

        @SuppressWarnings("unchecked")
        private static List<DvfSale>[] newYears() {
            var years = new List[10];
            for (int index = 0; index < years.length; index++) years[index] = new ArrayList<DvfSale>();
            return years;
        }

        void add(int year, DvfSale sale) {
            byYear[year - 2020].add(sale);
        }

        @Override
        public Optional<Commune> resolveCommune(String name, String code) {
            communeLookups.incrementAndGet();
            lastCode = code;
            return name.equals("Limoges") ? Optional.of(LIMOGES) : Optional.empty();
        }

        @Override
        public List<DvfSale> sales(Commune commune, int year) {
            if (!requestedYears.contains(year)) requestedYears.add(year);
            return byYear[year - 2020];
        }
    }
}
