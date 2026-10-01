package com.immoradar.backend.market;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A published DVF sale, used as an illustration rather than a valuation estimate. */
public record RecentSale(
        LocalDate date,
        String propertyCategory,
        BigDecimal surface,
        BigDecimal price,
        BigDecimal pricePerSquareMeter
) {}
