package com.immoradar.backend.deal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "deals")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
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
}
