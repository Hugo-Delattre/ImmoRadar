package com.immoradar.backend.deal;

import com.immoradar.backend.deal.dto.CreateDealRequest;
import com.immoradar.backend.deal.dto.DealResponse;
import com.immoradar.backend.deal.dto.DealSearchResponse;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class DealService {

    private static final String DEFAULT_IMAGE =
            "https://images.unsplash.com/photo-1560518883-ce09059eeffa?auto=format&fit=crop&w=1200&q=85";
    private static final BigDecimal TWELVE = BigDecimal.valueOf(12);
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final DealRepository dealRepository;

    public DealService(DealRepository dealRepository) {
        this.dealRepository = dealRepository;
    }

    public DealSearchResponse search(
            @Nullable BigDecimal priceMax,
            @Nullable BigDecimal yieldMin,
            @Nullable BigDecimal cashflowMin,
            @Nullable String location,
            boolean favoritesOnly,
            int page,
            int size) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "opportunityScore"));
        var deals = dealRepository.findAll(
                DealSpecifications.matching(priceMax, yieldMin, cashflowMin, location, favoritesOnly),
                pageable);

        return new DealSearchResponse(
                deals.getContent().stream().map(this::toResponse).toList(),
                deals.getTotalElements(),
                deals.getNumber(),
                deals.getSize(),
                deals.getTotalPages());
    }

    @Transactional
    public DealResponse create(CreateDealRequest request) {
        var deal = new Deal(
                UUID.randomUUID().toString(),
                request.title().trim(),
                request.price(),
                request.monthlyRent(),
                request.monthlyCharges(),
                request.propertyTax(),
                request.renovationCost(),
                request.location().trim(),
                request.surface(),
                request.propertyType(),
                request.description() == null ? "" : request.description().trim(),
                calculateOpportunityScore(request),
                request.imageUrl() == null || request.imageUrl().isBlank() ? DEFAULT_IMAGE : request.imageUrl().trim(),
                false);

        return toResponse(dealRepository.save(deal));
    }

    @Transactional
    public DealResponse setFavorite(String dealId, boolean favorite) {
        var deal = getEntity(dealId);
        deal.setFavorite(favorite);
        return toResponse(dealRepository.save(deal));
    }

    public Deal getEntity(String dealId) {
        return dealRepository.findById(dealId).orElseThrow(() -> new DealNotFoundException(dealId));
    }

    private double calculateOpportunityScore(CreateDealRequest request) {
        var grossYield = percentage(request.monthlyRent().multiply(TWELVE), request.price());
        var operatingIncome = monthlyOperatingIncome(
                request.monthlyRent(), request.monthlyCharges(), request.propertyTax());
        var yieldScore = Math.min(6.0, grossYield.doubleValue() * 0.6);
        var cashflowScore = Math.min(4.0, Math.max(0, operatingIncome.doubleValue() / 100));
        return BigDecimal.valueOf(yieldScore + cashflowScore)
                .setScale(1, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private DealResponse toResponse(Deal deal) {
        return new DealResponse(
                deal.getId(), deal.getTitle(), deal.getPrice(), deal.getMonthlyRent(),
                deal.getMonthlyCharges(), deal.getPropertyTax(), deal.getRenovationCost(),
                deal.getLocation(), deal.getSurface(), deal.getPropertyType(), deal.getDescription(),
                deal.getOpportunityScore(), deal.getImageUrl(), deal.isFavorite(),
                percentage(deal.getMonthlyRent().multiply(TWELVE), deal.getPrice()),
                monthlyOperatingIncome(deal.getMonthlyRent(), deal.getMonthlyCharges(), deal.getPropertyTax()),
                deal.getPrice().divide(deal.getSurface(), 0, RoundingMode.HALF_UP));
    }

    private static BigDecimal percentage(BigDecimal numerator, BigDecimal denominator) {
        return numerator.multiply(HUNDRED).divide(denominator, 2, RoundingMode.HALF_UP);
    }

    private static BigDecimal monthlyOperatingIncome(
            BigDecimal rent, BigDecimal charges, BigDecimal annualPropertyTax) {
        return rent.subtract(charges)
                .subtract(annualPropertyTax.divide(TWELVE, 2, RoundingMode.HALF_UP))
                .setScale(2, RoundingMode.HALF_UP);
    }
}
