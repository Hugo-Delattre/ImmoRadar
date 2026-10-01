package com.immoradar.backend.listing;

import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.util.List;

/**
 * Résultat d'un import d'annonce. Un champ que l'annonce ne donne pas reste {@code null} : l'interface
 * le laisse à compléter au lieu d'afficher une valeur inventée. {@code extractedFields} liste les champs
 * réellement lus et {@code warnings} ce que l'utilisateur doit vérifier.
 */
public record ListingExtractDto(
        @Nullable String title,
        @Nullable BigDecimal price,
        @Nullable BigDecimal monthlyRent,
        @Nullable BigDecimal surface,
        @Nullable String location,
        @Nullable String propertyType,
        @Nullable BigDecimal renovationCost,
        @Nullable BigDecimal monthlyCharges,
        @Nullable BigDecimal propertyTax,
        @Nullable String imageUrl,
        @Nullable String description,
        String sourceUrl,
        String platform,
        List<String> extractedFields,
        List<String> warnings,
        boolean demo
) {
    public ListingExtractDto {
        extractedFields = List.copyOf(extractedFields);
        warnings = List.copyOf(warnings);
    }
}
