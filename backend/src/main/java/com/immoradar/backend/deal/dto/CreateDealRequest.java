package com.immoradar.backend.deal.dto;

import com.immoradar.backend.deal.EnergyClass;
import com.immoradar.backend.deal.PropertyType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO Java 25 Record pour la création d'un deal avec validation déclarative Jakarta.
 */
public record CreateDealRequest(
    @NotBlank(message = "Le titre de l'annonce est obligatoire")
    String title,

    @NotNull @Positive(message = "Le prix d'achat doit être strictement supérieur à zéro")
    BigDecimal price,

    @NotNull @Positive(message = "Le loyer mensuel estimé doit être strictement supérieur à zéro")
    BigDecimal monthlyRent,

    @NotNull @PositiveOrZero(message = "Les charges mensuelles ne peuvent pas être négatives")
    BigDecimal monthlyCharges,

    @NotNull @PositiveOrZero(message = "La taxe foncière annuelle ne peut pas être négative")
    BigDecimal propertyTax,

    @NotNull @PositiveOrZero(message = "Le coût des travaux ne peut pas être négatif")
    BigDecimal renovationCost,

    @NotBlank(message = "La localisation (ville ou département) est obligatoire")
    String location,

    @NotNull @Positive(message = "La surface habitable doit être strictement supérieure à zéro")
    BigDecimal surface,

    @NotNull(message = "Le type de bien (Studio, Appartement, Immeuble, Maison) est obligatoire")
    PropertyType propertyType,

    @Nullable String description,

    @Nullable String imageUrl,

    @Nullable EnergyClass energyClass,

    @Nullable @Size(max = 2048)
    @Pattern(regexp = "^(|https://[^\\s]+)$", message = "L'URL source doit utiliser HTTPS")
    String sourceUrl,

    @Nullable @PastOrPresent(message = "La date de mise en ligne ne peut pas être dans le futur")
    LocalDate listedOn
) {
    public CreateDealRequest(
            String title, BigDecimal price, BigDecimal monthlyRent, BigDecimal monthlyCharges, BigDecimal propertyTax,
            BigDecimal renovationCost, String location, BigDecimal surface, PropertyType propertyType,
            @Nullable String description, @Nullable String imageUrl) {
        this(title, price, monthlyRent, monthlyCharges, propertyTax, renovationCost, location, surface, propertyType,
                description, imageUrl, null, null, null);
    }
}
