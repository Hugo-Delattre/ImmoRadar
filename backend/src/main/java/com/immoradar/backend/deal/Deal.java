package com.immoradar.backend.deal;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "deals")
@Getter @Setter
@NoArgsConstructor
public class Deal {

    @Id
    private String id;
    private String title;
    @Column(precision = 14, scale = 2, nullable = false)
    private BigDecimal price;
    @Column(precision = 14, scale = 2, nullable = false)
    private BigDecimal monthlyRent;
    @Column(precision = 14, scale = 2, nullable = false)
    private BigDecimal monthlyCharges;
    @Column(precision = 14, scale = 2, nullable = false)
    private BigDecimal propertyTax;
    @Column(precision = 14, scale = 2, nullable = false)
    private BigDecimal renovationCost;
    private String location;
    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal surface;

    @Enumerated(EnumType.STRING)
    private PropertyType propertyType;

    @Column(length = 1000)
    private String description;

    private double opportunityScore;
    private String imageUrl;
    private boolean favorite;

    // Colonnes ajoutées après la première version : elles restent nullables pour que la mise à jour
    // automatique du schéma fonctionne sur une base existante.
    @Enumerated(EnumType.STRING)
    private @Nullable EnergyClass energyClass;
    @Enumerated(EnumType.STRING)
    private @Nullable DealStatus status;
    @Column(length = 2048)
    private @Nullable String sourceUrl;
    private @Nullable LocalDate listedOn;
    private @Nullable LocalDate createdOn;

    // Valeurs dérivées, stockées pour pouvoir trier et filtrer en SQL.
    private @Nullable Double grossYield;
    private @Nullable Double pricePerSquareMeter;
    private @Nullable Double priceDropPercent;
    private @Nullable Double marketDeltaPercent;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "deal_price_history", joinColumns = @JoinColumn(name = "deal_id"))
    @OrderBy("observedOn ASC")
    private List<PricePoint> priceHistory = new ArrayList<>();

    public Deal(
            String id, String title, BigDecimal price, BigDecimal monthlyRent, BigDecimal monthlyCharges,
            BigDecimal propertyTax, BigDecimal renovationCost, String location, BigDecimal surface,
            PropertyType propertyType, String description, double opportunityScore, String imageUrl,
            boolean favorite) {
        this.id = id;
        this.title = title;
        this.price = price;
        this.monthlyRent = monthlyRent;
        this.monthlyCharges = monthlyCharges;
        this.propertyTax = propertyTax;
        this.renovationCost = renovationCost;
        this.location = location;
        this.surface = surface;
        this.propertyType = propertyType;
        this.description = description;
        this.opportunityScore = opportunityScore;
        this.imageUrl = imageUrl;
        this.favorite = favorite;
    }

    public DealStatus getStatus() {
        return status == null ? DealStatus.TO_REVIEW : status;
    }

    /** Prix le plus élevé observé : référence pour mesurer une baisse de prix. */
    public BigDecimal highestObservedPrice() {
        return priceHistory.stream().map(PricePoint::price).reduce(price, BigDecimal::max);
    }

    /** Enregistre un nouveau prix dans l'historique s'il diffère du dernier observé. */
    public void recordPrice(BigDecimal newPrice, LocalDate observedOn) {
        if (priceHistory.isEmpty() && price != null) {
            var since = listedOn != null ? listedOn : createdOn != null ? createdOn : observedOn;
            priceHistory.add(new PricePoint(since, price));
        }
        var last = priceHistory.isEmpty() ? null : priceHistory.getLast().price();
        if (last == null || last.compareTo(newPrice) != 0) {
            priceHistory.add(new PricePoint(observedOn, newPrice));
        }
        price = newPrice;
    }

    @PrePersist
    @PreUpdate
    void refreshDerivedValues() {
        if (createdOn == null) {
            createdOn = LocalDate.now();
        }
        if (priceHistory.isEmpty()) {
            priceHistory.add(new PricePoint(listedOn != null ? listedOn : createdOn, price));
        }
        grossYield = monthlyRent.multiply(BigDecimal.valueOf(1200))
                .divide(price, 2, RoundingMode.HALF_UP).doubleValue();
        pricePerSquareMeter = price.divide(surface, 0, RoundingMode.HALF_UP).doubleValue();
        var highest = highestObservedPrice();
        priceDropPercent = highest.subtract(price).multiply(BigDecimal.valueOf(100))
                .divide(highest, 1, RoundingMode.HALF_UP).doubleValue();
    }
}
