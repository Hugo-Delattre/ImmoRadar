package com.immoradar.backend.market;

import com.immoradar.backend.deal.PropertyType;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Prix au m² de référence calculé sur les ventes DVF de la commune : logements du même type, des trois
 * dernières années publiées, de surface comparable quand l'échantillon le permet.
 */
@Component
public class DvfComparables {

    static final int MIN_COMPARABLES = 8;
    private static final int SURFACE_BAND_MIN = 15;
    private static final Duration CACHE_TTL = Duration.ofHours(24);
    // Un échec (réseau, fichier absent) est retenté plus tôt qu'un résultat.
    private static final Duration EMPTY_CACHE_TTL = Duration.ofMinutes(30);
    private static final Pattern NAME_AND_CODE = Pattern.compile("^(.*?)\\s*\\((\\d{5}|\\d{2}|2[AB]|97\\d)\\)\\s*$");

    private final MarketDataClient client;
    private final Clock clock;
    private final boolean enabled;
    private final Map<String, Cached<Optional<Commune>>> communes = new ConcurrentHashMap<>();
    private final Map<String, Cached<List<DvfSale>>> sales = new ConcurrentHashMap<>();

    public DvfComparables(
            MarketDataClient client, Clock clock, @Value("${immoradar.market.live-dvf:true}") boolean enabled) {
        this.client = client;
        this.clock = clock;
        this.enabled = enabled;
    }

    public record Stats(
            Commune commune, int count, double median, double firstQuartile, double thirdQuartile,
            LocalDate periodStart, LocalDate periodEnd, String scope) {}

    public Optional<Stats> forLocation(String location, @Nullable PropertyType type, @Nullable BigDecimal surface) {
        if (!enabled || location.isBlank()) return Optional.empty();
        return commune(location).flatMap(commune -> compute(commune, type, surface));
    }

    Optional<Stats> compute(Commune commune, @Nullable PropertyType type, @Nullable BigDecimal surface) {
        var latestYear = LocalDate.now(clock).getYear() - 1;
        var wantedTypes = localTypes(type);
        var candidates = new ArrayList<DvfSale>();
        for (int year = latestYear; year > latestYear - 3; year--) {
            for (var sale : salesFor(commune, year)) {
                var perSquareMeter = sale.pricePerSquareMeter();
                if (wantedTypes.contains(sale.localType()) && perSquareMeter >= 300 && perSquareMeter <= 30000) {
                    candidates.add(sale);
                }
            }
        }

        var scope = typeLabel(type);
        var comparables = candidates;
        if (surface != null && surface.signum() > 0) {
            var low = surface.doubleValue() * 0.5;
            var high = surface.doubleValue() * 2;
            var sameSize = candidates.stream()
                    .filter(sale -> sale.surface().doubleValue() >= low && sale.surface().doubleValue() <= high)
                    .toList();
            if (sameSize.size() >= SURFACE_BAND_MIN) {
                comparables = new ArrayList<>(sameSize);
                scope += String.format(Locale.FRENCH, " de %.0f à %.0f m²", low, high);
            }
        }
        if (comparables.size() < MIN_COMPARABLES) return Optional.empty();

        var prices = comparables.stream().mapToDouble(DvfSale::pricePerSquareMeter).sorted().toArray();
        var dates = comparables.stream().map(DvfSale::date).sorted(Comparator.naturalOrder()).toList();
        return Optional.of(new Stats(commune, prices.length, quantile(prices, 0.5), quantile(prices, 0.25),
                quantile(prices, 0.75), dates.getFirst(), dates.getLast(), scope + ", " + commune.name()));
    }

    private Optional<Commune> commune(String location) {
        var key = location.trim().toLowerCase(Locale.FRENCH);
        return cached(communes, key, () -> {
            var matcher = NAME_AND_CODE.matcher(location.trim());
            return matcher.matches()
                    ? client.resolveCommune(matcher.group(1).trim(), matcher.group(2))
                    : client.resolveCommune(location.trim(), "");
        });
    }

    private List<DvfSale> salesFor(Commune commune, int year) {
        return cached(sales, commune.inseeCode() + "/" + year, () -> client.sales(commune, year));
    }

    private <T> T cached(Map<String, Cached<T>> cache, String key, java.util.function.Supplier<T> loader) {
        var now = Instant.now(clock);
        var hit = cache.get(key);
        if (hit != null && hit.expiresAt().isAfter(now)) return hit.value();
        var value = loader.get();
        var empty = value instanceof Optional<?> optional ? optional.isEmpty() : value instanceof List<?> list && list.isEmpty();
        cache.put(key, new Cached<>(value, now.plus(empty ? EMPTY_CACHE_TTL : CACHE_TTL)));
        return value;
    }

    private record Cached<T>(T value, Instant expiresAt) {}

    private static List<String> localTypes(@Nullable PropertyType type) {
        if (type == null || type == PropertyType.BUILDING) return List.of("Appartement", "Maison");
        return type == PropertyType.HOUSE ? List.of("Maison") : List.of("Appartement");
    }

    private static String typeLabel(@Nullable PropertyType type) {
        if (type == null || type == PropertyType.BUILDING) return "Logements";
        return type == PropertyType.HOUSE ? "Maisons" : "Appartements";
    }

    static double quantile(double[] sorted, double q) {
        var position = (sorted.length - 1) * q;
        var lower = (int) Math.floor(position);
        var upper = (int) Math.ceil(position);
        return sorted[lower] + (sorted[upper] - sorted[lower]) * (position - lower);
    }
}
