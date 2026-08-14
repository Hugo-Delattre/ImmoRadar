package com.immoradar.backend.deal.dto;

import com.immoradar.backend.deal.PropertyType;

import java.math.BigDecimal;

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
        BigDecimal pricePerSquareMeter
) {}
