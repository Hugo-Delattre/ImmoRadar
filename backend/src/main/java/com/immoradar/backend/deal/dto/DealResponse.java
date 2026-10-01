package com.immoradar.backend.deal.dto;

import com.immoradar.backend.deal.DealStatus;
import com.immoradar.backend.deal.EnergyClass;
import com.immoradar.backend.deal.PricePoint;
import com.immoradar.backend.deal.PropertyType;
import com.immoradar.backend.deal.ScoreFactor;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record DealResponse(
        String id,
        String title,
        BigDecimal price,
        BigDecimal monthlyRent,
        BigDecimal monthlyCharges,
        BigDecimal propertyTax,
        BigDecimal renovationCost,
        String location,
        BigDecimal surface,
        PropertyType propertyType,
        String description,
        double opportunityScore,
        String imageUrl,
        boolean favorite,
        BigDecimal grossYield,
        BigDecimal monthlyOperatingIncome,
        BigDecimal pricePerSquareMeter,
        DealStatus status,
        @Nullable EnergyClass energyClass,
        @Nullable String sourceUrl,
        @Nullable LocalDate listedOn,
        @Nullable Long daysOnMarket,
        double priceDropPercent,
        @Nullable Double marketDeltaPercent,
        BigDecimal referenceMonthlyCashFlow,
        List<PricePoint> priceHistory,
        List<ScoreFactor> scoreBreakdown,
        List<String> alerts
) {
    public DealResponse {
        priceHistory = List.copyOf(priceHistory);
        scoreBreakdown = List.copyOf(scoreBreakdown);
        alerts = List.copyOf(alerts);
    }
}
