package com.immoradar.backend.deal;

import jakarta.persistence.criteria.Predicate;
import org.jspecify.annotations.Nullable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Locale;

final class DealSpecifications {

    private DealSpecifications() {}

    static Specification<Deal> matching(
            @Nullable BigDecimal priceMax,
            @Nullable BigDecimal yieldMin,
            @Nullable BigDecimal cashflowMin,
            @Nullable String location,
            boolean favoritesOnly) {
        return (root, query, builder) -> {
            var predicates = new ArrayList<Predicate>();

            if (priceMax != null) {
                predicates.add(builder.lessThanOrEqualTo(root.get("price"), priceMax));
            }
            if (location != null && !location.isBlank()) {
                predicates.add(builder.like(
                        builder.lower(root.get("location")),
                        "%" + location.trim().toLowerCase(Locale.FRENCH) + "%"));
            }
            if (yieldMin != null) {
                var monthlyRent = root.get("monthlyRent").as(Double.class);
                var price = root.get("price").as(Double.class);
                var grossYield = builder.quot(
                        builder.prod(monthlyRent, 1200.0),
                        price);
                predicates.add(builder.ge(grossYield, yieldMin.doubleValue()));
            }
            if (cashflowMin != null) {
                var monthlyRent = root.get("monthlyRent").as(Double.class);
                var monthlyCharges = root.get("monthlyCharges").as(Double.class);
                var propertyTax = root.get("propertyTax").as(Double.class);
                var monthlyOperatingIncome = builder.diff(
                        builder.diff(monthlyRent, monthlyCharges),
                        builder.quot(propertyTax, 12.0));
                predicates.add(builder.ge(monthlyOperatingIncome, cashflowMin.doubleValue()));
            }
            if (favoritesOnly) {
                predicates.add(builder.isTrue(root.get("favorite")));
            }

            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }
}
