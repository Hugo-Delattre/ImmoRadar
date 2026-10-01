package com.immoradar.backend.listing;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class ListingExtractorService {
    private static final int MAX_HTML_BYTES = 1_000_000;
    private static final List<String> ALLOWED_HOSTS = List.of(
            "leboncoin.fr", "seloger.com", "pap.fr", "bienici.com", "logic-immo.com");
    private static final Pattern META = Pattern.compile("(?is)<meta\\b[^>]*>");
    private static final Pattern PRICE = Pattern.compile("(?i)(\\d[\\d \\u00a0.,]{3,12})\\s*€");
    private static final Pattern SURFACE = Pattern.compile("(?i)(\\d{1,4}(?:[.,]\\d{1,2})?)\\s*m(?:²|2|&sup2;)");
    private static final Pattern JSON_PRICE = Pattern.compile("\"price\"\\s*:\\s*\"?(\\d{4,9}(?:[.]\\d{1,2})?)\"?");

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3)).followRedirects(HttpClient.Redirect.NEVER).build();

    public ListingExtractDto extract(String url) {
        URI uri = validateUrl(url);
        try {
            var request = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(8))
                    .header("Accept", "text/html").header("User-Agent", "ImmoRadar/1.0 (+manual-review)")
                    .GET().build();
            var response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() != 200
                    || !response.headers().firstValue("Content-Type").orElse("").toLowerCase().contains("text/html")) {
                throw new ListingExtractionException("Le portail ne permet pas de lire cette annonce. Saisis les données manuellement.");
            }
            try (InputStream body = response.body()) {
                byte[] bytes = body.readNBytes(MAX_HTML_BYTES + 1);
                if (bytes.length > MAX_HTML_BYTES) {
                    throw new ListingExtractionException("La page est trop volumineuse pour une extraction fiable.");
                }
                return parsePage(uri, new String(bytes, StandardCharsets.UTF_8));
            }
        } catch (ListingExtractionException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ListingExtractionException("L'annonce n'est pas accessible automatiquement. Saisis les données manuellement.");
        }
    }

    static URI validateUrl(String url) {
        try {
            URI uri = URI.create(url.trim());
            String host = uri.getHost();
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getUserInfo() != null
                    || uri.getPort() != -1 || host == null || url.length() > 2048
                    || ALLOWED_HOSTS.stream().noneMatch(allowed -> host.equals(allowed) || host.endsWith("." + allowed))) {
                throw new IllegalArgumentException("Utilise une URL HTTPS d'un portail immobilier pris en charge.");
            }
            return uri;
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Utilise une URL HTTPS d'un portail immobilier pris en charge.");
        }
    }

    static ListingExtractDto parsePage(URI uri, String html) {
        String title = meta(html, "og:title");
        String description = meta(html, "og:description");
        String image = meta(html, "og:image");
        String text = ((title == null ? "" : title) + " " + (description == null ? "" : description));
        BigDecimal price = number(PRICE, text);
        if (price == null) price = number(JSON_PRICE, html);
        BigDecimal surface = number(SURFACE, text);
        if (price == null || price.compareTo(BigDecimal.valueOf(10_000)) < 0 || surface == null
                || surface.compareTo(BigDecimal.valueOf(8)) < 0) {
            throw new ListingExtractionException("Prix ou surface introuvable dans l'annonce : aucun chiffre n'a été inventé.");
        }
        String propertyType = text.toLowerCase().contains("maison") ? "House"
                : text.toLowerCase().contains("studio") ? "Studio" : "Apartment";
        String host = uri.getHost();
        return new ListingExtractDto(title == null || title.isBlank() ? "Annonce à vérifier" : title.trim(),
                price, null, surface, null, propertyType, null, null, null,
                image, description, uri.toString(), host);
    }

    private static @Nullable String meta(String html, String property) {
        var tags = META.matcher(html);
        while (tags.find()) {
            String tag = tags.group();
            if (!tag.matches("(?is).*\\b(?:property|name)\\s*=\\s*['\"]" + Pattern.quote(property) + "['\"].*")) continue;
            var value = Pattern.compile("(?is)\\bcontent\\s*=\\s*(['\"])(.*?)\\1").matcher(tag);
            if (value.find()) return value.group(2).replace("&amp;", "&").replace("&nbsp;", " ");
        }
        return null;
    }

    private static @Nullable BigDecimal number(Pattern pattern, String text) {
        var matcher = pattern.matcher(text);
        if (!matcher.find()) return null;
        try {
            return new BigDecimal(matcher.group(1).replaceAll("[\\s\\u00a0]", "").replace(',', '.'));
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
