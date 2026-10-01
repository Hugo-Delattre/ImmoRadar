package com.immoradar.backend.deal;

import com.immoradar.backend.deal.dto.CreateDealRequest;
import com.immoradar.backend.deal.dto.DealResponse;
import com.immoradar.backend.deal.dto.DealSearchResponse;
import com.immoradar.backend.market.DvfMarketService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class DealService {

    private static final String DEFAULT_IMAGE =
            "https://images.unsplash.com/photo-1560518883-ce09059eeffa?auto=format&fit=crop&w=1200&q=85";
    private static final BigDecimal TWELVE = BigDecimal.valueOf(12);
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final DealRepository dealRepository;
    private final DvfMarketService marketService;
    private final Clock clock;

    public DealService(DealRepository dealRepository, DvfMarketService marketService, Clock clock) {
        this.dealRepository = dealRepository;
        this.marketService = marketService;
        this.clock = clock;
    }

    public DealSearchResponse search(DealSearchCriteria criteria, int page, int size) {
        var pageable = PageRequest.of(page, size, criteria.sort().toSort());
        var deals = dealRepository.findAll(DealSpecifications.matching(criteria), pageable);

        return new DealSearchResponse(
                deals.getContent().stream().map(this::toResponse).toList(),
                deals.getTotalElements(),
                deals.getNumber(),
                deals.getSize(),
                deals.getTotalPages());
    }

    public DealResponse get(String dealId) {
        return toResponse(getEntity(dealId));
    }

    @Transactional
    public DealResponse create(CreateDealRequest request) {
        var deal = new Deal();
        deal.setId(UUID.randomUUID().toString());
        deal.setStatus(DealStatus.TO_REVIEW);
        deal.setCreatedOn(today());
        apply(deal, request);
        deal.setPrice(request.price());
        return toResponse(dealRepository.save(rescore(deal)));
    }

    @Transactional
    public DealResponse update(String dealId, CreateDealRequest request) {
        var deal = getEntity(dealId);
        apply(deal, request);
        deal.recordPrice(request.price(), today());
        return toResponse(dealRepository.save(rescore(deal)));
    }

    @Transactional
    public DealResponse setFavorite(String dealId, boolean favorite) {
        var deal = getEntity(dealId);
        deal.setFavorite(favorite);
        return toResponse(dealRepository.save(deal));
    }

    @Transactional
    public DealResponse setStatus(String dealId, DealStatus status) {
        var deal = getEntity(dealId);
        deal.setStatus(status);
        return toResponse(dealRepository.save(deal));
    }

    @Transactional
    public void delete(String dealId) {
        dealRepository.delete(getEntity(dealId));
    }

    /** Recalcule les valeurs dérivées et le score de tous les biens, par exemple après une évolution du barème. */
    @Transactional
    public void rescoreAll() {
        dealRepository.findAll().forEach(deal -> {
            deal.setStatus(deal.getStatus());
            dealRepository.save(rescore(deal));
        });
    }

    public Deal getEntity(String dealId) {
        return dealRepository.findById(dealId).orElseThrow(() -> new DealNotFoundException(dealId));
    }

    private void apply(Deal deal, CreateDealRequest request) {
        deal.setTitle(request.title().trim());
        deal.setMonthlyRent(request.monthlyRent());
        deal.setMonthlyCharges(request.monthlyCharges());
        deal.setPropertyTax(request.propertyTax());
        deal.setRenovationCost(request.renovationCost());
        deal.setLocation(request.location().trim());
        deal.setSurface(request.surface());
        deal.setPropertyType(request.propertyType());
        deal.setDescription(request.description() == null ? "" : request.description().trim());
        deal.setImageUrl(request.imageUrl() == null || request.imageUrl().isBlank()
                ? DEFAULT_IMAGE : request.imageUrl().trim());
        deal.setEnergyClass(request.energyClass());
        deal.setSourceUrl(request.sourceUrl() == null || request.sourceUrl().isBlank() ? null : request.sourceUrl().trim());
        deal.setListedOn(request.listedOn());
    }

    private Deal rescore(Deal deal) {
        deal.refreshDerivedValues();
        var market = marketService.analyze(
                deal.getLocation(), deal.getPrice(), deal.getSurface(), deal.getPropertyType());
        deal.setMarketDeltaPercent(market.deltaPercentage().doubleValue());
        deal.setOpportunityScore(DealScoring.score(DealScoring.breakdown(deal, today())));
        return deal;
    }

    private DealResponse toResponse(Deal deal) {
        var today = today();
        return new DealResponse(
                deal.getId(), deal.getTitle(), deal.getPrice(), deal.getMonthlyRent(),
                deal.getMonthlyCharges(), deal.getPropertyTax(), deal.getRenovationCost(),
                deal.getLocation(), deal.getSurface(), deal.getPropertyType(), deal.getDescription(),
                deal.getOpportunityScore(), deal.getImageUrl(), deal.isFavorite(),
                percentage(deal.getMonthlyRent().multiply(TWELVE), deal.getPrice()),
                monthlyOperatingIncome(deal.getMonthlyRent(), deal.getMonthlyCharges(), deal.getPropertyTax()),
                deal.getPrice().divide(deal.getSurface(), 0, RoundingMode.HALF_UP),
                deal.getStatus(),
                deal.getEnergyClass(),
                deal.getSourceUrl(),
                deal.getListedOn(),
                DealScoring.daysOnMarket(deal, today),
                deal.getPriceDropPercent() == null ? 0 : deal.getPriceDropPercent(),
                deal.getMarketDeltaPercent(),
                BigDecimal.valueOf(DealScoring.referenceMonthlyCashFlow(deal)).setScale(0, RoundingMode.HALF_UP),
                deal.getPriceHistory(),
                DealScoring.breakdown(deal, today),
                DealScoring.alerts(deal, today));
    }

    private LocalDate today() {
        return LocalDate.now(clock);
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
