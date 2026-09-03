package com.immoradar.backend.market;

import java.math.BigDecimal;

/**
 * Analyse comparative de marché basée sur les données officielles DVF (Demande de Valeur Foncière - data.gouv.fr).
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
        String liquidityScore, // A+, A, B, C
        int averageSaleDelayDays,
        String advice
) {}
