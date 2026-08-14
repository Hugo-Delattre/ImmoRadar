package com.immoradar.backend.deal.dto;

import com.immoradar.backend.deal.PropertyType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/**
 * DTO Java 25 Record pour la création d'un deal avec validation déclarative Jakarta.
 */
public record CreateDealRequest(
    @NotBlank(message = "Le titre de l'annonce est obligatoire")
    String title,

    @Positive(message = "Le prix d'achat doit être strictement supérieur à zéro")
    BigDecimal price,

    @Positive(message = "Le loyer mensuel estimé doit être strictement supérieur à zéro")
    BigDecimal monthlyRent,

    @PositiveOrZero(message = "Les charges mensuelles ne peuvent pas être négatives")
    BigDecimal monthlyCharges,

    @PositiveOrZero(message = "La taxe foncière annuelle ne peut pas être négative")
    BigDecimal propertyTax,

    @PositiveOrZero(message = "Le coût des travaux ne peut pas être négatif")
    BigDecimal renovationCost,

    @NotBlank(message = "La localisation (ville ou département) est obligatoire")
    String location,

    @Positive(message = "La surface habitable doit être strictement supérieure à zéro")
    BigDecimal surface,

    @NotNull(message = "Le type de bien (Studio, Appartement, Immeuble, Maison) est obligatoire")
    PropertyType propertyType,

    String description,

    String imageUrl
) {}
