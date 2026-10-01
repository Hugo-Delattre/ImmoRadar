package com.immoradar.backend.listing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.Optional;

/**
 * Client HTTP de l'import d'annonces, protégé contre les requêtes vers le réseau interne (SSRF) :
 * seuls http et https sont acceptés, chaque hôte (redirections comprises) doit se résoudre vers une adresse
 * publique, les redirections sont suivies à la main et la taille lue est plafonnée.
 */
@Component
class SafeHttpPageFetcher implements ListingPageFetcher {

    private static final Logger log = LoggerFactory.getLogger(SafeHttpPageFetcher.class);
    private static final int MAX_REDIRECTS = 3;
    private static final int MAX_BYTES = 2 * 1024 * 1024;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    @Override
    public Optional<String> fetch(URI uri) {
        var current = uri;
        try {
            for (int hop = 0; hop <= MAX_REDIRECTS; hop++) {
                if (!isPublicHttpTarget(current)) {
                    log.warn("Refused to fetch non-public listing URL {}", current);
                    return Optional.empty();
                }
                var request = HttpRequest.newBuilder(current)
                        .timeout(Duration.ofSeconds(5))
                        .header("User-Agent", "Mozilla/5.0 (compatible; ImmoRadar/1.0)")
                        .header("Accept-Language", "fr-FR,fr;q=0.9")
                        .GET()
                        .build();
                var response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
                var status = response.statusCode();
                if (status >= 300 && status < 400) {
                    response.body().close();
                    var location = response.headers().firstValue("Location");
                    if (location.isEmpty()) {
                        return Optional.empty();
                    }
                    current = current.resolve(location.get());
                    continue;
                }
                try (var body = response.body()) {
                    return status == 200 ? Optional.of(readCapped(body)) : Optional.empty();
                }
            }
        } catch (IOException | IllegalArgumentException ex) {
            log.info("Could not fetch listing URL {}: {}", current, ex.getMessage());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
        return Optional.empty();
    }

    static boolean isPublicHttpTarget(URI uri) {
        var scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        if (!scheme.equals("http") && !scheme.equals("https")) {
            return false;
        }
        var host = uri.getHost();
        if (host == null || host.isBlank()) {
            return false;
        }
        try {
            for (var address : InetAddress.getAllByName(host)) {
                if (!isPublicAddress(address)) {
                    return false;
                }
            }
            return true;
        } catch (UnknownHostException ex) {
            return false;
        }
    }

    static boolean isPublicAddress(InetAddress address) {
        if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
                || address.isSiteLocalAddress() || address.isMulticastAddress()) {
            return false;
        }
        var bytes = address.getAddress();
        if (address instanceof Inet6Address) {
            // fc00::/7 : adresses locales uniques
            return (bytes[0] & 0xFE) != 0xFC;
        }
        // 100.64.0.0/10 : NAT opérateur, utilisé par certains réseaux internes de cloud
        return !((bytes[0] & 0xFF) == 100 && (bytes[1] & 0xC0) == 64);
    }

    private static String readCapped(InputStream body) throws IOException {
        return new String(body.readNBytes(MAX_BYTES), StandardCharsets.UTF_8);
    }
}
