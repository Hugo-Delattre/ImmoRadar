package com.immoradar.backend.deal;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class DealSpecifications {

    private DealSpecifications() {}

    static Specification<Deal> matching(DealSearchCriteria criteria) {
        return (root, query, builder) -> {
            var predicates = new ArrayList<Predicate>();

            if (criteria.priceMax() != null) {
                predicates.add(builder.lessThanOrEqualTo(root.get("price"), criteria.priceMax()));
            }
            if (criteria.location() != null && !criteria.location().isBlank()) {
                predicates.add(builder.like(
                        builder.lower(root.get("location")),
                        "%" + criteria.location().trim().toLowerCase(Locale.FRENCH) + "%"));
            }
            if (criteria.yieldMin() != null) {
                predicates.add(builder.ge(root.get("grossYield"), criteria.yieldMin().doubleValue()));
            }
            if (criteria.cashflowMin() != null) {
                var monthlyRent = root.get("monthlyRent").as(Double.class);
                var monthlyCharges = root.get("monthlyCharges").as(Double.class);
                var propertyTax = root.get("propertyTax").as(Double.class);
                var monthlyOperatingIncome = builder.diff(
                        builder.diff(monthlyRent, monthlyCharges),
                        builder.quot(propertyTax, 12.0));
                predicates.add(builder.ge(monthlyOperatingIncome, criteria.cashflowMin().doubleValue()));
            }
            if (criteria.favoritesOnly()) {
                predicates.add(builder.isTrue(root.get("favorite")));
            }
            if (criteria.propertyType() != null) {
                predicates.add(builder.equal(root.get("propertyType"), criteria.propertyType()));
            }
            if (criteria.excludeEnergySieves()) {
                var energyClass = root.<EnergyClass>get("energyClass");
                predicates.add(builder.or(
                        builder.isNull(energyClass),
                        builder.not(energyClass.in(List.of(EnergyClass.F, EnergyClass.G)))));
            }
            var status = root.<DealStatus>get("status");
            if (criteria.status() != null) {
                predicates.add(criteria.status() == DealStatus.TO_REVIEW
                        ? builder.or(builder.isNull(status), builder.equal(status, DealStatus.TO_REVIEW))
                        : builder.equal(status, criteria.status()));
            } else {
                predicates.add(builder.or(builder.isNull(status), builder.notEqual(status, DealStatus.REJECTED)));
            }

            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }
}
