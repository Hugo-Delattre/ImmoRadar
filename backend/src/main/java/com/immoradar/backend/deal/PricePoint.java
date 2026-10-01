package com.immoradar.backend.deal;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Prix observé à une date : l'historique sert à repérer les baisses de prix. */
@Embeddable
public record PricePoint(
        @Column(name = "observed_on") LocalDate observedOn,
        @Column(name = "price", precision = 14, scale = 2) BigDecimal price
) {}
