package com.immoradar.backend.deal;

import com.immoradar.backend.deal.dto.CreateDealRequest;
import com.immoradar.backend.deal.dto.DealResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class DealServiceIntegrationTests {

    @Autowired
    private DealService dealService;
    @Autowired
    private DealRepository dealRepository;

    @BeforeEach
    void clean() {
        dealRepository.deleteAll();
    }

    @Test
    void tracksPriceDropsWhenAListingIsUpdated() {
        var created = dealService.create(request("Appartement Limoges", "150000", EnergyClass.D));

        var updated = dealService.update(created.id(), request("Appartement Limoges", "135000", EnergyClass.D));

        assertThat(updated.priceDropPercent()).isEqualTo(10.0);
        assertThat(updated.priceHistory()).extracting(point -> point.price().intValue()).containsExactly(150000, 135000);
        assertThat(updated.opportunityScore()).isGreaterThan(created.opportunityScore());
        assertThat(updated.scoreBreakdown()).anyMatch(f -> f.key().equals("negotiation") && f.points() == 1.0);
    }

    @Test
    void hidesRejectedDealsAndSortsByPrice() {
        var cheap = dealService.create(request("Studio", "90000", EnergyClass.C));
        dealService.create(request("T3", "160000", EnergyClass.C));
        var rejected = dealService.create(request("T2", "120000", EnergyClass.C));
        dealService.setStatus(rejected.id(), DealStatus.REJECTED);

        var visible = search(new DealSearchCriteria(null, null, null, null, false, null, false, null, DealSort.PRICE_ASC));
        var onlyRejected = search(new DealSearchCriteria(null, null, null, null, false, null, false, DealStatus.REJECTED, DealSort.SCORE));

        assertThat(visible).extracting(DealResponse::title).containsExactly("Studio", "T3");
        assertThat(visible.getFirst().id()).isEqualTo(cheap.id());
        assertThat(onlyRejected).extracting(DealResponse::id).containsExactly(rejected.id());
    }

    @Test
    void canExcludeEnergySievesAndFilterOnStoredYield() {
        dealService.create(request("Passoire", "100000", EnergyClass.G));
        dealService.create(request("Rénové", "100000", EnergyClass.B));
        dealService.create(request("Cher", "300000", null));

        var results = search(new DealSearchCriteria(null, new BigDecimal("6"), null, null, false, null, true, null, DealSort.SCORE));

        assertThat(results).extracting(DealResponse::title).containsExactly("Rénové");
        assertThat(results.getFirst().alerts()).noneMatch(alert -> alert.startsWith("DPE"));
    }

    private java.util.List<DealResponse> search(DealSearchCriteria criteria) {
        return dealService.search(criteria, 0, 20).content();
    }

    private static CreateDealRequest request(String title, String price, EnergyClass energyClass) {
        return new CreateDealRequest(title, new BigDecimal(price), new BigDecimal("900"), new BigDecimal("60"),
                new BigDecimal("800"), BigDecimal.ZERO, "Limoges (87)", new BigDecimal("55"), PropertyType.APARTMENT,
                null, null, energyClass, "https://www.pap.fr/annonce/1", LocalDate.of(2026, 9, 1));
    }
}
