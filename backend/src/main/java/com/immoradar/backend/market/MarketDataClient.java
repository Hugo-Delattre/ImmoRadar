package com.immoradar.backend.market;

import java.util.List;
import java.util.Optional;

/** Accès aux données publiques : référentiel des communes et ventes DVF. */
public interface MarketDataClient {

    /** Trouve la commune d'après son nom et, si connu, son code postal ou son département. */
    Optional<Commune> resolveCommune(String name, String postalOrDepartmentCode);

    /** Ventes de logements de la commune pour une année ; liste vide si le fichier n'existe pas. */
    List<DvfSale> sales(Commune commune, int year);
}
