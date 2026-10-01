package com.immoradar.backend.market;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.Duration;
import java.time.Year;
import java.util.Optional;
import java.util.regex.Pattern;

/** Fixed upstream hosts: user supplied text only becomes a query parameter, never a target URL. */
@Component
public class MarketDataClient {
    private static final Pattern PARENTHESIZED_CODE = Pattern.compile("\\s*\\((?:\\d{2,3}|\\d{5})\\)\\s*$");
    private static final String METHODOLOGY = "https://foncierdata.fr/methodologie";
    private static final int MIN_COMPARABLES = 20;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3)).followRedirects(HttpClient.Redirect.NEVER).build();
    private final JsonMapper mapper;

    public MarketDataClient(JsonMapper mapper) {
        this.mapper = mapper;
    }

    public Optional<ComparableMarket> findComparable(String location, String category, BigDecimal surface) {
        try {
            String commune = PARENTHESIZED_CODE.matcher(location.trim()).replaceFirst("").trim();
            if (commune.isBlank() || commune.length() > 80) return Optional.empty();
            String query = URLEncoder.encode(commune, StandardCharsets.UTF_8);
            JsonNode candidates = readJson("https://geo.api.gouv.fr/communes?nom=" + query
                    + "&fields=code,nom,codeDepartement&boost=population&limit=10");
            if (!candidates.isArray()) return Optional.empty();
            String expectedDepartment = departmentFrom(location);
            for (JsonNode candidate : candidates) {
                if (!normalize(candidate.path("nom").asText()).equals(normalize(commune))) continue;
                if (expectedDepartment != null
                        && !candidate.path("codeDepartement").asText().equals(expectedDepartment)) continue;
                String code = candidate.path("code").asText();
                if (!code.matches("[0-9AB]{5}")) continue;
                JsonNode market = readJson("https://foncierdata.fr/api/v1/commune/" + code + ".json");
                return parseMarket(market, code, category, surface);
            }
        } catch (Exception ignored) {
            // An unavailable upstream must not turn into an invented valuation.
        }
        return Optional.empty();
    }

    Optional<ComparableMarket> parseMarket(JsonNode market, String code, String category, BigDecimal surface) {
        if (!code.equals(market.path("code_insee").asText())) return Optional.empty();
        JsonNode bands = market.path("tranches_surface").path(category);
        if (!bands.isArray()) return Optional.empty();
        for (JsonNode band : bands) {
            if (!matchesBand(band.path("band").asText(), surface)) continue;
            int count = band.path("n").asInt();
            BigDecimal median = band.path("median").decimalValue();
            if (count < MIN_COMPARABLES || median.signum() <= 0) return Optional.empty();
            int year = market.path("annee_reference").asInt();
            if (year < Year.now().getValue() - 3 || year > Year.now().getValue()) return Optional.empty();
            return Optional.of(new ComparableMarket(market.path("nom").asText(), code, median, count,
                    year, market.path("fiabilite").asText(),
                    "https://foncierdata.fr/api/v1/commune/" + code + ".json", METHODOLOGY));
        }
        return Optional.empty();
    }

    private JsonNode readJson(String url) throws Exception {
        var request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(6))
                .header("Accept", "application/json").GET().build();
        var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200 || response.body().length() > 1_000_000) {
            throw new IllegalStateException("Source indisponible");
        }
        return mapper.readTree(response.body());
    }

    private static boolean matchesBand(String label, BigDecimal surface) {
        if (label.startsWith("< 30")) return surface.compareTo(BigDecimal.valueOf(30)) < 0;
        if (label.startsWith("30-60")) return between(surface, 30, 60);
        if (label.startsWith("60-90")) return between(surface, 60, 90);
        if (label.startsWith("90-120")) return between(surface, 90, 120);
        if (label.startsWith("120 ")) return surface.compareTo(BigDecimal.valueOf(120)) >= 0;
        return false;
    }

    private static boolean between(BigDecimal value, int lower, int upper) {
        return value.compareTo(BigDecimal.valueOf(lower)) >= 0 && value.compareTo(BigDecimal.valueOf(upper)) < 0;
    }

    private static String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .toLowerCase().replace('-', ' ').replaceAll("\\s+", " ").trim();
    }

    private static String departmentFrom(String location) {
        var matcher = PARENTHESIZED_CODE.matcher(location);
        if (!matcher.find()) return null;
        String digits = matcher.group().replaceAll("\\D", "");
        return digits.length() == 5 ? digits.substring(0, 2) : digits;
    }

    public record ComparableMarket(String location, String codeInsee, BigDecimal medianPricePerSquareMeter,
                                   int comparableCount, int referenceYear, String reliability,
                                   String sourceUrl, String methodologyUrl) {}
}
