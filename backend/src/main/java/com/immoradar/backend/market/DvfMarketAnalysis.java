package com.immoradar.backend.market;

import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Comparaison du prix d'un bien avec le marché.
 *
 * <p>{@code source} vaut {@code DVF} quand la référence vient des ventes réelles de la commune
 * (avec leur nombre et leur période), ou {@code ESTIMATION} quand seul un barème départemental
 * indicatif est disponible. {@code averageSaleDelayDays} n'est connu que pour le barème.
 */
public record DvfMarketAnalysis(
        String location,
        BigDecimal dealPricePerSquareMeter,
        BigDecimal dvfMedianPricePerSquareMeter,
        BigDecimal dvfLowPricePerSquareMeter,
        BigDecimal dvfHighPricePerSquareMeter,
        BigDecimal deltaPercentage,
        String marketStatus, // SOUS_EVALUE, ALIGNE, SUREVALUE
        BigDecimal suggestedOfferPrice,
        BigDecimal negotiationMargin,
        int transactionsCount5Years,
        String liquidityScore,
        @Nullable Integer averageSaleDelayDays,
        String advice,
        String source,
        String scope,
        @Nullable LocalDate periodStart,
        @Nullable LocalDate periodEnd
) {}
