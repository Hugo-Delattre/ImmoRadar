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

@Entity
@Table(name = "deals")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class Deal {

    @Id
    private String id;
    private String title;
    private double price;
    private double monthlyRent;
    private double monthlyCharges;
    private double propertyTax;
    private double renovationCost;
    private String location;
    private double surface;
    
    @Enumerated(EnumType.STRING)
    private PropertyType propertyType;
    
    @Column(length = 1000)
    private String description;
    
    private double opportunityScore;
    private String imageUrl;
}
