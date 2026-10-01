package com.immoradar.backend.listing;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pré-remplit une fiche à partir de l'URL d'une annonce.
 *
 * <p>Seules les valeurs effectivement lues dans la page (balises Open Graph, JSON-LD) ou dans l'URL sont
 * renvoyées. Les grands portails bloquent souvent la lecture automatique : dans ce cas la réponse le dit
 * et laisse les champs vides plutôt que de proposer des chiffres fictifs.
 */
@Service
public class ListingExtractorService {

    static final String DEMO_LEBONCOIN = "https://www.leboncoin.fr/ad/ventes_immobilieres/3271114816";
    static final String DEMO_SELOGER = "https://www.seloger.com/annonces/achat/appartement/paris-11eme-75/studio-renove";
    static final String DEMO_PAP = "https://www.pap.fr/annonces/appartement-bordeaux-centre-t2";

    private static final Pattern PRICE_IN_TEXT = Pattern.compile(
            "(\\d{1,3}(?:[\\s  .]\\d{3})+|\\d{4,7})\\s*(?:€|euros?\\b)", Pattern.CASE_INSENSITIVE);
    private static final Pattern PRICE_IN_JSON_LD = Pattern.compile("\"price\"\\s*:\\s*\"?(\\d+(?:\\.\\d+)?)");
    private static final Pattern SURFACE_IN_TEXT = Pattern.compile(
            "(\\d{1,4}(?:[.,]\\d{1,2})?)\\s*(?:m²|m2|m\\s?carrés?)", Pattern.CASE_INSENSITIVE);
    private static final Pattern PLACE_WITH_CODE = Pattern.compile(
            "(\\p{Lu}[\\p{L}'’\\-]+(?:[ \\-]\\p{L}[\\p{L}'’\\-]*){0,3})\\s*\\(?(\\d{5}|\\d{2}|2[AB])\\)");
    private static final Pattern SLUG_WITH_POSTCODE = Pattern.compile("([a-z][a-z\\-]{1,40}?)-(\\d{5})(?:\\D|$)");
    private static final Pattern RENT_IN_TEXT = Pattern.compile(
            "loy(?:er|é)[^\\d€]{0,30}(\\d{1,3}(?:[\\s  .]\\d{3})*|\\d{3,5})\\s*(?:€|euros?)",
            Pattern.CASE_INSENSITIVE);

    private final ListingPageFetcher pageFetcher;

    public ListingExtractorService(ListingPageFetcher pageFetcher) {
        this.pageFetcher = pageFetcher;
    }

    public ListingExtractDto extract(String url) {
        var cleanUrl = url.trim();
        var uri = parseHttpUri(cleanUrl);
        var platform = detectPlatform(uri);

        var demo = DEMO_LISTINGS.get(cleanUrl);
        if (demo != null) {
            return demo;
        }

        var fields = new ExtractedFields();
        var warnings = new ArrayList<String>();

        var page = pageFetcher.fetch(uri);
        if (page.isPresent()) {
            readPage(page.get(), fields);
        } else {
            warnings.add("La page " + platform + " n’a pas pu être lue automatiquement (protection anti-robots ou "
                    + "annonce retirée). Recopie le prix et la surface depuis l’annonce.");
        }
        readUrlSlug(uri, fields);

        if (fields.price == null) warnings.add("Prix non trouvé : saisis-le depuis l’annonce.");
        if (fields.surface == null) warnings.add("Surface non trouvée : saisis-la depuis l’annonce.");
        if (fields.location == null) warnings.add("Ville non trouvée : indique la commune et son code postal.");
        if (fields.monthlyRent == null) {
            warnings.add("Loyer non indiqué dans l’annonce : estime-le à partir des loyers du secteur.");
        }
        warnings.add("Charges, taxe foncière et travaux ne sont pas lus : vérifie-les avant de simuler.");

        return new ListingExtractDto(
                fields.title, fields.price, fields.monthlyRent, fields.surface, fields.location,
                fields.propertyType, null, null, null, fields.imageUrl, fields.description,
                cleanUrl, platform, fields.found, warnings, false);
    }

    private static void readPage(String html, ExtractedFields fields) {
        var title = metaContent(html, "og:title");
        var description = metaContent(html, "og:description");
        fields.set("title", title, value -> fields.title = value);
        fields.set("description", description, value -> fields.description = value);
        fields.set("imageUrl", metaContent(html, "og:image"), value -> fields.imageUrl = value);

        var summary = (title == null ? "" : title) + " · " + (description == null ? "" : description);
        fields.set("price", firstAmount(PRICE_IN_JSON_LD.matcher(html)), value -> fields.price = value);
        fields.set("price", firstAmount(PRICE_IN_TEXT.matcher(summary)), value -> fields.price = value);
        fields.set("surface", firstAmount(SURFACE_IN_TEXT.matcher(summary)), value -> fields.surface = value);
        fields.set("monthlyRent", firstAmount(RENT_IN_TEXT.matcher(summary)), value -> fields.monthlyRent = value);
        fields.set("propertyType", propertyType(summary), value -> fields.propertyType = value);

        var place = PLACE_WITH_CODE.matcher(summary);
        if (place.find()) {
            fields.set("location", place.group(1).trim() + " (" + place.group(2) + ")", value -> fields.location = value);
        }
    }

    /** Les URL de PAP, SeLoger ou Bien'ici contiennent souvent la commune, le code postal et le type de bien. */
    private static void readUrlSlug(URI uri, ExtractedFields fields) {
        var path = URLDecoder.decode(uri.getPath() == null ? "" : uri.getPath(), StandardCharsets.UTF_8)
                .toLowerCase(Locale.ROOT);
        var slug = SLUG_WITH_POSTCODE.matcher(path);
        if (slug.find()) {
            var words = slug.group(1).replaceFirst("^(?:appartement|maison|studio|immeuble|vente|achat)-", "");
            fields.set("location", capitalize(words) + " (" + slug.group(2) + ")", value -> fields.location = value);
        }
        fields.set("propertyType", propertyType(path.replace('-', ' ')), value -> fields.propertyType = value);
    }

    private static @Nullable String propertyType(String text) {
        var lower = text.toLowerCase(Locale.ROOT);
        if (lower.contains("immeuble")) return "Building";
        if (lower.contains("maison") || lower.contains("villa") || lower.contains("pavillon")) return "House";
        if (lower.contains("studio")) return "Studio";
        if (lower.contains("appartement") || Pattern.compile("\\b[tf][1-6]\\b").matcher(lower).find()) {
            return "Apartment";
        }
        return null;
    }

    private static @Nullable String metaContent(String html, String property) {
        var name = "(?:property|name)=[\"']" + Pattern.quote(property) + "[\"']";
        var content = "content=(?:\"([^\"]*)\"|'([^']*)')";
        var propertyFirst = Pattern.compile("(?i)<meta[^>]*?" + name + "[^>]*?" + content);
        var contentFirst = Pattern.compile("(?i)<meta[^>]*?" + content + "[^>]*?" + name);
        for (var pattern : List.of(propertyFirst, contentFirst)) {
            var matcher = pattern.matcher(html);
            if (matcher.find()) {
                var value = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
                if (!value.isBlank()) {
                    return decodeEntities(value.trim());
                }
            }
        }
        return null;
    }

    private static @Nullable BigDecimal firstAmount(Matcher matcher) {
        while (matcher.find()) {
            try {
                var raw = matcher.group(1).replaceAll("[\\s  ]", "");
                // "180.000" est un séparateur de milliers ; "45,5" ou "45.5" une décimale
                raw = raw.matches("\\d{1,3}(\\.\\d{3})+") ? raw.replace(".", "") : raw.replace(",", ".");
                var value = new BigDecimal(raw);
                if (value.signum() > 0) {
                    return value;
                }
            } catch (NumberFormatException ignored) {
                // valeur suivante
            }
        }
        return null;
    }

    private static String decodeEntities(String value) {
        return value.replace("&amp;", "&").replace("&quot;", "\"").replace("&#39;", "'")
                .replace("&#x27;", "'").replace("&apos;", "'").replace("&lt;", "<").replace("&gt;", ">")
                .replace("&nbsp;", " ").replace("&euro;", "€").replace("&#8364;", "€");
    }

    private static String capitalize(String slugWords) {
        var small = List.of("sur", "en", "le", "la", "les", "de", "du", "des", "et");
        var parts = new ArrayList<String>();
        for (var part : slugWords.split("-")) {
            if (part.isEmpty()) continue;
            parts.add(parts.isEmpty() || !small.contains(part)
                    ? Character.toUpperCase(part.charAt(0)) + part.substring(1)
                    : part);
        }
        return String.join("-", parts);
    }

    private static URI parseHttpUri(String url) {
        try {
            var uri = new URI(url);
            var scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
            if ((!scheme.equals("http") && !scheme.equals("https")) || uri.getHost() == null) {
                throw new IllegalArgumentException("Colle l’adresse complète de l’annonce (https://…).");
            }
            return uri;
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException("Cette adresse d’annonce n’est pas valide.");
        }
    }

    private static String detectPlatform(URI uri) {
        var host = uri.getHost().toLowerCase(Locale.ROOT);
        if (host.endsWith("leboncoin.fr")) return "Leboncoin";
        if (host.endsWith("seloger.com")) return "SeLoger";
        if (host.endsWith("pap.fr")) return "PAP";
        if (host.endsWith("bienici.com")) return "Bien'ici";
        if (host.endsWith("logic-immo.com")) return "Logic-Immo";
        if (host.endsWith("figaro.fr")) return "Propriétés Le Figaro";
        if (host.endsWith("paruvendu.fr")) return "ParuVendu";
        return host.replaceFirst("^www\\.", "");
    }

    private static final class ExtractedFields {
        private final List<String> found = new ArrayList<>();
        private @Nullable String title;
        private @Nullable String description;
        private @Nullable String imageUrl;
        private @Nullable String location;
        private @Nullable String propertyType;
        private @Nullable BigDecimal price;
        private @Nullable BigDecimal surface;
        private @Nullable BigDecimal monthlyRent;

        /** Garde la première valeur trouvée pour chaque champ. */
        <T> void set(String name, @Nullable T value, java.util.function.Consumer<T> setter) {
            if (value != null && !found.contains(name)) {
                setter.accept(value);
                found.add(name);
            }
        }
    }

    private static final Map<String, ListingExtractDto> DEMO_LISTINGS = Map.of(
            DEMO_LEBONCOIN, demo(
                    "Maison 3 pièces 74 m²", "180000", "950", "74", "Le Havre (76600)", "House", "0", "40", "890",
                    "https://images.unsplash.com/photo-1568605117036-5fe5e7bab0b7?w=800&auto=format&fit=crop&q=80",
                    "Maison 3 pièces 74 m² avec 2 chambres, terrasse de 40 m² et garage. Aucuns travaux à prévoir.",
                    DEMO_LEBONCOIN, "Leboncoin"),
            DEMO_SELOGER, demo(
                    "Studio rénové 24 m² proche métro", "195000", "890", "24", "Paris (75011)", "Studio", "5000", "60", "510",
                    "https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?w=800&auto=format&fit=crop&q=80",
                    "Studio refait à neuf au pied des commerces et du métro.",
                    DEMO_SELOGER, "SeLoger"),
            DEMO_PAP, demo(
                    "T2 rénové avec balcon 48 m²", "178000", "920", "48", "Bordeaux (33000)", "Apartment", "8000", "75", "740",
                    "https://images.unsplash.com/photo-1560448204-e02f11c3d0e2?w=800&auto=format&fit=crop&q=80",
                    "Particulier vend appartement 2 pièces lumineux, proche tramway et universités.",
                    DEMO_PAP, "PAP"));

    private static ListingExtractDto demo(
            String title, String price, String rent, String surface, String location, String type,
            String works, String charges, String propertyTax, String image, String description,
            String url, String platform) {
        return new ListingExtractDto(
                title, new BigDecimal(price), new BigDecimal(rent), new BigDecimal(surface), location, type,
                new BigDecimal(works), new BigDecimal(charges), new BigDecimal(propertyTax), image, description,
                url, platform,
                List.of("title", "price", "monthlyRent", "surface", "location", "propertyType", "renovationCost",
                        "monthlyCharges", "propertyTax", "imageUrl", "description"),
                List.of("Annonce de démonstration : les chiffres sont fictifs."),
                true);
    }
}
