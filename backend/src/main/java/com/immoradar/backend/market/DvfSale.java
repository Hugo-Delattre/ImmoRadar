package com.immoradar.backend.market;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Vente d'un logement issue des fichiers DVF géolocalisés (un seul local d'habitation par mutation). */
public record DvfSale(String mutationId, LocalDate date, BigDecimal price, BigDecimal surface, String localType) {

    public double pricePerSquareMeter() {
        return price.doubleValue() / surface.doubleValue();
    }
}
