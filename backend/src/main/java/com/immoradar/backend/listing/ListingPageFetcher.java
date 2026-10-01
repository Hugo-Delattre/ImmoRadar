package com.immoradar.backend.listing;

import java.net.URI;
import java.util.Optional;

/**
 * Récupère le HTML d'une page d'annonce. Renvoie {@link Optional#empty()} quand la page ne peut pas être lue
 * (portail protégé, page absente, délai dépassé) : l'import continue alors avec ce que l'URL elle-même révèle.
 */
@FunctionalInterface
public interface ListingPageFetcher {

    Optional<String> fetch(URI uri);
}
