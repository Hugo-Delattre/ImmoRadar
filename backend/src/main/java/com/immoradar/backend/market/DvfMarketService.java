package com.immoradar.backend.market;

import com.immoradar.backend.deal.PropertyType;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Map;

/**
 * Compare le prix d'un bien au marché.
 *
 * <p>La référence vient en priorité des ventes réelles DVF de la commune ({@link DvfComparables}). Quand elles
 * ne sont pas disponibles (réseau, commune inconnue, trop peu de ventes), un barème départemental indicatif
 * prend le relais et la réponse l'indique ({@code source = ESTIMATION}).
 */
@Service
public class DvfMarketService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private record Reference(
            BigDecimal median, BigDecimal low, BigDecimal high, int transactions, String liquidityScore,
            @Nullable Integer averageSaleDelayDays, String source, String scope,
            java.time.@Nullable LocalDate periodStart, java.time.@Nullable LocalDate periodEnd) {}

    // Barème indicatif par département (prix m² médian, bas, haut, ventes sur 5 ans, liquidité, délai de vente).
    private record Benchmark(
            BigDecimal medianPriceM2, BigDecimal lowPriceM2, BigDecimal highPriceM2,
            int transactions5Years, String liquidityScore, int averageSaleDelayDays) {}

    private static final Map<String, Benchmark> DEPARTMENT_BENCHMARKS = Map.of(
            "42", new Benchmark(new BigDecimal("1450"), new BigDecimal("1150"), new BigDecimal("1850"), 3420, "B+", 72),
            "87", new Benchmark(new BigDecimal("1650"), new BigDecimal("1300"), new BigDecimal("2100"), 2850, "A-", 65),
            "68", new Benchmark(new BigDecimal("1380"), new BigDecimal("1050"), new BigDecimal("1780"), 3100, "B", 78),
            "72", new Benchmark(new BigDecimal("1820"), new BigDecimal("1450"), new BigDecimal("2300"), 4150, "A", 58),
            "90", new Benchmark(new BigDecimal("1320"), new BigDecimal("1000"), new BigDecimal("1700"), 1650, "B-", 85),
            "75", new Benchmark(new BigDecimal("9800"), new BigDecimal("8500"), new BigDecimal("12500"), 35200, "A+", 42),
            "69", new Benchmark(new BigDecimal("4600"), new BigDecimal("3800"), new BigDecimal("5900"), 18400, "A+", 48),
            "13", new Benchmark(new BigDecimal("3300"), new BigDecimal("2400"), new BigDecimal("4800"), 15100, "A", 55),
            "33", new Benchmark(new BigDecimal("4200"), new BigDecimal("3400"), new BigDecimal("5300"), 14200, "A+", 49),
            "31", new Benchmark(new BigDecimal("3600"), new BigDecimal("2900"), new BigDecimal("4500"), 16800, "A+", 51)
    );

    private static final Map<String, String> CITY_DEPARTMENTS = Map.of(
            "paris", "75", "lyon", "69", "marseille", "13", "bordeaux", "33", "toulouse", "31",
            "saint-étienne", "42", "saint etienne", "42", "limoges", "87", "mulhouse", "68", "le mans", "72");

    private static final Benchmark NATIONAL_BENCHMARK = new Benchmark(
            new BigDecimal("2100"), new BigDecimal("1600"), new BigDecimal("2800"), 2500, "B", 70);

    private final DvfComparables comparables;

    public DvfMarketService(DvfComparables comparables) {
        this.comparables = comparables;
    }

    public DvfMarketAnalysis analyze(
            String location, BigDecimal totalPrice, BigDecimal surface, @Nullable PropertyType propertyType) {
        var pricePerSquareMeter = totalPrice.divide(surface, 0, RoundingMode.HALF_UP);
        var reference = comparables.forLocation(location, propertyType, surface)
                .map(DvfMarketService::fromSales)
                .orElseGet(() -> fromBenchmark(location));
        return evaluate(location, pricePerSquareMeter, totalPrice, surface, reference);
    }

    private static Reference fromSales(DvfComparables.Stats stats) {
        var months = Math.max(1, ChronoUnit.MONTHS.between(stats.periodStart(), stats.periodEnd()) + 1);
        var salesPerYear = stats.count() * 12.0 / months;
        var liquidity = salesPerYear >= 150 ? "A" : salesPerYear >= 50 ? "B" : salesPerYear >= 15 ? "C" : "D";
        return new Reference(
                BigDecimal.valueOf(stats.median()).setScale(0, RoundingMode.HALF_UP),
                BigDecimal.valueOf(stats.firstQuartile()).setScale(0, RoundingMode.HALF_UP),
                BigDecimal.valueOf(stats.thirdQuartile()).setScale(0, RoundingMode.HALF_UP),
                stats.count(), liquidity, null, "DVF",
                stats.count() + " ventes · " + stats.scope(), stats.periodStart(), stats.periodEnd());
    }

    private static Reference fromBenchmark(String location) {
        var department = department(location);
        var benchmark = department == null ? NATIONAL_BENCHMARK : DEPARTMENT_BENCHMARKS.get(department);
        var scope = department == null
                ? "Moyenne nationale indicative"
                : "Barème indicatif du département " + department;
        return new Reference(benchmark.medianPriceM2(), benchmark.lowPriceM2(), benchmark.highPriceM2(),
                benchmark.transactions5Years(), benchmark.liquidityScore(), benchmark.averageSaleDelayDays(),
                "ESTIMATION", scope, null, null);
    }

    private static DvfMarketAnalysis evaluate(
            String location, BigDecimal pricePerSquareMeter, BigDecimal totalPrice, BigDecimal surface, Reference reference) {
        var median = reference.median();
        var deltaPercentage = pricePerSquareMeter.subtract(median).multiply(HUNDRED)
                .divide(median, 1, RoundingMode.HALF_UP);
        var basis = reference.source().equals("DVF")
                ? "des ventes réelles DVF (" + reference.scope() + ")"
                : "du barème indicatif (pas de ventes DVF disponibles pour ce secteur)";

        String marketStatus;
        BigDecimal negotiationMargin;
        BigDecimal suggestedOfferPrice;
        String advice;

        if (deltaPercentage.compareTo(new BigDecimal("-4.0")) <= 0) {
            marketStatus = "SOUS_EVALUE";
            negotiationMargin = BigDecimal.ZERO;
            suggestedOfferPrice = totalPrice;
            advice = String.format(Locale.FRENCH,
                    "Prix %.1f %% sous la médiane %s, à %s €/m². Vérifie l'état du bien et le DPE : si rien ne "
                            + "l'explique, une offre proche du prix évite de perdre l'affaire.",
                    deltaPercentage.abs().doubleValue(), basis, median.toPlainString());
        } else if (deltaPercentage.compareTo(new BigDecimal("4.0")) >= 0) {
            marketStatus = "SUREVALUE";
            negotiationMargin = pricePerSquareMeter.subtract(median).multiply(surface).setScale(0, RoundingMode.HALF_UP);
            suggestedOfferPrice = totalPrice.subtract(negotiationMargin.multiply(new BigDecimal("0.85")))
                    .setScale(0, RoundingMode.HALF_UP);
            advice = String.format(Locale.FRENCH,
                    "Prix %.1f %% au-dessus de la médiane %s. L'écart représente ~%s € : une offre autour de "
                            + "%s € est défendable en citant ces ventes.",
                    deltaPercentage.doubleValue(), basis, negotiationMargin.toPlainString(),
                    suggestedOfferPrice.toPlainString());
        } else {
            marketStatus = "ALIGNE";
            negotiationMargin = totalPrice.multiply(new BigDecimal("0.04")).setScale(0, RoundingMode.HALF_UP);
            suggestedOfferPrice = totalPrice.subtract(negotiationMargin);
            advice = String.format(Locale.FRENCH,
                    "Prix aligné avec la médiane %s (%s €/m²). Une marge d'environ %s € (4 %%) reste courante à la visite.",
                    basis, median.toPlainString(), negotiationMargin.toPlainString());
        }

        return new DvfMarketAnalysis(location, pricePerSquareMeter, median, reference.low(), reference.high(),
                deltaPercentage, marketStatus, suggestedOfferPrice, negotiationMargin, reference.transactions(),
                reference.liquidityScore(), reference.averageSaleDelayDays(), advice, reference.source(),
                reference.scope(), reference.periodStart(), reference.periodEnd());
    }

    private static @Nullable String department(String location) {
        if (location.isBlank()) return null;
        var lower = location.toLowerCase(Locale.FRENCH);
        var code = java.util.regex.Pattern.compile("\\((\\d{2})\\d{0,3}\\)").matcher(lower);
        if (code.find() && DEPARTMENT_BENCHMARKS.containsKey(code.group(1))) return code.group(1);
        for (var entry : CITY_DEPARTMENTS.entrySet()) {
            if (lower.contains(entry.getKey())) return entry.getValue();
        }
        return null;
    }
}
