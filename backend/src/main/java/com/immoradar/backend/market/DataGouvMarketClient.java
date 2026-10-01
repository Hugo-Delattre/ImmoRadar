package com.immoradar.backend.market;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Client des API publiques : geo.api.gouv.fr pour les communes et les fichiers DVF géolocalisés d'Etalab
 * (files.data.gouv.fr/geo-dvf), publiés par commune et par année.
 */
@Component
class DataGouvMarketClient implements MarketDataClient {

    private static final Logger log = LoggerFactory.getLogger(DataGouvMarketClient.class);
    private static final Set<String> CITIES_WITH_ARRONDISSEMENTS = Set.of("75056", "13055", "69123");

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String geoApiUrl;
    private final String dvfFilesUrl;

    DataGouvMarketClient(
            @Value("${immoradar.market.geo-api-url:https://geo.api.gouv.fr}") String geoApiUrl,
            @Value("${immoradar.market.dvf-files-url:https://files.data.gouv.fr/geo-dvf/latest/csv}") String dvfFilesUrl) {
        this.geoApiUrl = geoApiUrl;
        this.dvfFilesUrl = dvfFilesUrl;
    }

    @Override
    public Optional<Commune> resolveCommune(String name, String postalOrDepartmentCode) {
        var query = new StringBuilder("/communes?fields=code,nom,codeDepartement&boost=population&limit=5")
                .append("&type=arrondissement-municipal,commune-actuelle");
        if (!name.isBlank()) query.append("&nom=").append(encode(name));
        if (postalOrDepartmentCode.length() == 5) {
            query.append("&codePostal=").append(postalOrDepartmentCode);
        } else if (!postalOrDepartmentCode.isBlank()) {
            query.append("&codeDepartement=").append(postalOrDepartmentCode);
        }
        return get(geoApiUrl + query).flatMap(body -> {
            try {
                JsonNode communes = objectMapper.readTree(body);
                if (!communes.isArray() || communes.isEmpty()) return Optional.empty();
                // Paris, Lyon et Marseille n'ont de fichiers DVF que par arrondissement : on préfère
                // l'arrondissement quand la recherche par code postal en renvoie un.
                JsonNode best = communes.get(0);
                for (JsonNode commune : communes) {
                    if (!CITIES_WITH_ARRONDISSEMENTS.contains(commune.path("code").asString())) {
                        best = commune;
                        break;
                    }
                }
                return Optional.of(new Commune(
                        best.path("code").asString(), best.path("nom").asString(), best.path("codeDepartement").asString()));
            } catch (RuntimeException ex) {
                log.info("Unreadable geo API response for {}: {}", name, ex.getMessage());
                return Optional.empty();
            }
        });
    }

    @Override
    public List<DvfSale> sales(Commune commune, int year) {
        var url = "%s/%d/communes/%s/%s.csv".formatted(dvfFilesUrl, year, commune.departmentCode(), commune.inseeCode());
        try {
            var request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(10)).GET().build();
            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() != 200) {
                response.body().close();
                return List.of();
            }
            try (var reader = new BufferedReader(new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
                return parseCsv(reader);
            }
        } catch (IOException | IllegalArgumentException ex) {
            log.info("DVF file unavailable {}: {}", url, ex.getMessage());
            return List.of();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return List.of();
        }
    }

    /**
     * Ne garde que les ventes d'un seul logement (appartement ou maison) : une mutation qui regroupe plusieurs
     * locaux répète le prix total sur chaque ligne et fausserait le prix au m².
     */
    static List<DvfSale> parseCsv(BufferedReader reader) throws IOException {
        var header = reader.readLine();
        if (header == null) return List.of();
        var columns = indexColumns(splitCsvLine(header));
        Map<String, List<String[]>> rowsByMutation = new HashMap<>();
        String line;
        while ((line = reader.readLine()) != null) {
            var fields = splitCsvLine(line).toArray(String[]::new);
            if (fields.length < columns.size()) continue;
            rowsByMutation.computeIfAbsent(fields[columns.get("id_mutation")], key -> new ArrayList<>()).add(fields);
        }

        var sales = new ArrayList<DvfSale>();
        for (var entry : rowsByMutation.entrySet()) {
            var dwellings = entry.getValue().stream()
                    .filter(row -> isDwelling(row[columns.get("type_local")]))
                    .toList();
            var first = entry.getValue().getFirst();
            if (dwellings.size() != 1 || !"Vente".equals(first[columns.get("nature_mutation")])) continue;
            var row = dwellings.getFirst();
            try {
                var price = new BigDecimal(row[columns.get("valeur_fonciere")]);
                var surface = new BigDecimal(row[columns.get("surface_reelle_bati")]);
                if (price.signum() <= 0 || surface.signum() <= 0) continue;
                sales.add(new DvfSale(entry.getKey(), LocalDate.parse(row[columns.get("date_mutation")]),
                        price, surface, row[columns.get("type_local")]));
            } catch (RuntimeException ignored) {
                // ligne incomplète
            }
        }
        return sales;
    }

    private static boolean isDwelling(String localType) {
        return "Appartement".equals(localType) || "Maison".equals(localType);
    }

    private static Map<String, Integer> indexColumns(List<String> header) {
        var columns = new HashMap<String, Integer>();
        for (int index = 0; index < header.size(); index++) columns.put(header.get(index), index);
        for (var required : List.of("id_mutation", "date_mutation", "nature_mutation", "valeur_fonciere",
                "type_local", "surface_reelle_bati")) {
            if (!columns.containsKey(required)) throw new IllegalArgumentException("Colonne DVF manquante : " + required);
        }
        return columns;
    }

    static List<String> splitCsvLine(String line) {
        var fields = new ArrayList<String>();
        var current = new StringBuilder();
        var quoted = false;
        for (int index = 0; index < line.length(); index++) {
            var character = line.charAt(index);
            if (character == '"') {
                if (quoted && index + 1 < line.length() && line.charAt(index + 1) == '"') {
                    current.append('"');
                    index++;
                } else {
                    quoted = !quoted;
                }
            } else if (character == ',' && !quoted) {
                fields.add(current.toString());
                current.setLength(0);
            } else {
                current.append(character);
            }
        }
        fields.add(current.toString());
        return fields;
    }

    private Optional<String> get(String url) {
        try {
            var request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(5)).GET().build();
            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200 ? Optional.of(response.body()) : Optional.empty();
        } catch (IOException | IllegalArgumentException ex) {
            log.info("Geo API unavailable: {}", ex.getMessage());
            return Optional.empty();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
