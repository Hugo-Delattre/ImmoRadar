package com.immoradar.backend.qualification;

import com.immoradar.backend.deal.*;
import com.immoradar.backend.evidence.*;
import com.immoradar.backend.market.*;
import com.immoradar.backend.simulation.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.*;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;
import static com.immoradar.backend.qualification.QualificationResponse.*;
import static com.immoradar.backend.qualification.RentalReferenceRequest.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class QualificationServiceTests {
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 2);
    private final DealService deals = mock(DealService.class);
    private final DealEvidenceService evidence = mock(DealEvidenceService.class);
    private final RentalReferenceService references = mock(RentalReferenceService.class);
    private final DvfMarketService market = mock(DvfMarketService.class);
    private final StressTestService stress = spy(new StressTestService(deals, new FinancialSimulationService(deals)));
    private final QualificationService service = new QualificationService(deals, evidence, references, market, stress,
            Clock.fixed(Instant.parse("2026-10-02T12:00:00Z"), ZoneOffset.UTC));
    private Deal deal;

    @BeforeEach
    void setup() {
        deal = new Deal("actual", "T2 à examiner", n("80000"), n("900"), n("40"), n("600"), n("5000"),
                "Limoges (87)", n("50"), PropertyType.APARTMENT, "", 0, "", false, "https://agency.fr/vente/abc");
        when(deals.getEntity("actual")).thenReturn(deal);
        when(evidence.getSummary("actual")).thenReturn(dossier(TODAY, TODAY));
        when(references.list("actual")).thenReturn(refs(3, true, TODAY, "Limoges (87)", "50", "1000", RentalMode.FURNISHED));
        when(market.analyzeDeal("actual")).thenReturn(snapshot(2025, 25, "3000"));
    }

    @Test
    void qualifiesOnlyAsPotentialWithReproducibleInputsAndFixedShocks() {
        var result = service.assess(request("30000", RentalMode.FURNISHED, TaxRegime.REEL_LMNP));
        assertThat(result.outcome()).isEqualTo(Outcome.POTENTIAL);
        assertThat(result.certified()).isFalse();
        assertThat(result.policyVersion()).isEqualTo("potential-v1");
        assertThat(result.assessedAt()).isEqualTo(Instant.parse("2026-10-02T12:00:00Z"));
        assertThat(result.checks()).allMatch(check -> check.state() == State.PASS);
        assertThat(result.supportedMonthlyRent()).isEqualByComparingTo("1000");
        assertThat(result.rentalReferences()).hasSize(3);
        assertThat(result.market().sourceUrl()).isEqualTo("https://foncierdata.fr/test");
        verify(stress).compare(new StressTestRequest(result.assumptions().base(), n("10"), n("10"), n("15"), n("5")));
        assertThat(deal.getMonthlyRent()).isEqualByComparingTo("900");
    }

    @Test
    void unavailableOrStaleOrSmallMarketNeverQualifies() {
        for (var snapshot : List.of(DvfMarketAnalysis.unavailable("Limoges", n("1600"), "API indisponible"),
                snapshot(2022, 100, "3000"), snapshot(2027, 100, "3000"), snapshot(2025, 19, "3000"))) {
            when(market.analyzeDeal("actual")).thenReturn(snapshot);
            var result = assess();
            assertThat(result.outcome()).isEqualTo(Outcome.INCOMPLETE);
            assertThat(check(result, "MARKET").state()).isEqualTo(State.MISSING);
        }
    }

    @Test
    void insufficientReferencesOrOnlyAskingRentsNeverQualify() {
        for (var list : List.of(refs(2, true, TODAY, "Limoges (87)", "50", "1000", RentalMode.FURNISHED),
                refs(3, false, TODAY, "Limoges (87)", "50", "1000", RentalMode.FURNISHED))) {
            when(references.list("actual")).thenReturn(list);
            assertThat(assess().outcome()).isEqualTo(Outcome.INCOMPLETE);
        }
    }

    @Test
    void excludesStaleFutureWrongTownSurfaceAndModeWithAnExplanation() {
        var invalid = List.of(
                refs(3, true, TODAY.minusDays(181), "Limoges (87)", "50", "1000", RentalMode.FURNISHED),
                refs(3, true, TODAY.plusDays(1), "Limoges (87)", "50", "1000", RentalMode.FURNISHED),
                refs(3, true, TODAY, "Lyon", "50", "1000", RentalMode.FURNISHED),
                refs(3, true, TODAY, "Limoges (87)", "61", "1000", RentalMode.FURNISHED),
                refs(3, true, TODAY, "Limoges (87)", "39", "1000", RentalMode.FURNISHED),
                refs(3, true, TODAY, "Limoges (87)", "50", "1000", RentalMode.UNFURNISHED));
        for (var list : invalid) {
            when(references.list("actual")).thenReturn(list);
            var result = assess();
            assertThat(result.outcome()).isEqualTo(Outcome.INCOMPLETE);
            assertThat(result.rentalReferences()).allSatisfy(ref -> {
                assertThat(ref.eligible()).isFalse(); assertThat(ref.reason()).isNotBlank();
            });
        }
    }

    @Test
    void repeatedSourcesDoNotIncreaseTheSampleAndTypesMustMatch() {
        var same = refs(1, true, TODAY, "Limoges (87)", "50", "1000", RentalMode.FURNISHED).getFirst();
        when(references.list("actual")).thenReturn(List.of(same, same, same));
        assertThat(assess().rentalReferences().stream().filter(ReferenceCheck::eligible)).hasSize(1);
        assertThat(assess().outcome()).isEqualTo(Outcome.INCOMPLETE);
        deal.setPropertyType(PropertyType.HOUSE);
        when(references.list("actual")).thenReturn(refs(3, true, TODAY, "Limoges (87)", "50", "1000", RentalMode.FURNISHED));
        assertThat(assess().rentalReferences()).allMatch(ref -> !ref.eligible());
    }

    @Test
    void rentAboveSupportedLevelFailsEvenWithStrongCashFlow() {
        when(references.list("actual")).thenReturn(refs(3, true, TODAY, "Limoges (87)", "50", "800", RentalMode.FURNISHED));
        var result = assess();
        assertThat(result.outcome()).isEqualTo(Outcome.NOT_QUALIFIED);
        assertThat(check(result, "RENT_LEVEL").state()).isEqualTo(State.FAIL);
    }

    @Test
    void expensiveAskingRentsCannotOverrideALowerDeclaredLease() {
        var list = new java.util.ArrayList<>(refs(3, false, TODAY, "Limoges (87)", "50", "1200", RentalMode.FURNISHED));
        list.set(0, refs(1, true, TODAY, "Limoges (87)", "50", "800", RentalMode.FURNISHED).getFirst());
        when(references.list("actual")).thenReturn(list);
        var result = assess();
        assertThat(result.supportedMonthlyRent()).isEqualByComparingTo("800");
        assertThat(result.outcome()).isEqualTo(Outcome.NOT_QUALIFIED);
    }

    @Test
    void projectPriceAndFinancialResilienceAreIndependentGates() {
        when(market.analyzeDeal("actual")).thenReturn(snapshot(2025, 25, "1800"));
        assertThat(check(assess(), "PRICE_MARGIN").state()).isEqualTo(State.FAIL);
        when(market.analyzeDeal("actual")).thenReturn(snapshot(2025, 25, "3000"));
        deal.setMonthlyCharges(n("500"));
        assertThat(check(assess(), "RESILIENCE").state()).isEqualTo(State.FAIL);
        assertThat(assess().outcome()).isEqualTo(Outcome.NOT_QUALIFIED);
    }

    @Test
    void changingFinancingRecomputesRatherThanReusingAQualifiedVerdict() {
        var request = request("0", RentalMode.FURNISHED, TaxRegime.REEL_LMNP);
        var highRate = new SimulationRequest("actual", n("0"), n("15"), 5, TaxRegime.REEL_LMNP,
                n("30"), n("4"), n("0"), n("180"), n("1.5"), n("1.2"));
        var result = service.assess(new QualificationRequest(highRate, request.rentalMode()));
        assertThat(result.outcome()).isEqualTo(Outcome.NOT_QUALIFIED);
        assertThat(result.assumptions().base().interestRate()).isEqualByComparingTo("15");
    }

    @Test
    void completeButOldDossierAndUnavailableListingRemainIncomplete() {
        when(evidence.getSummary("actual")).thenReturn(dossier(TODAY.minusDays(366), TODAY));
        assertThat(check(assess(), "EVIDENCE").state()).isEqualTo(State.MISSING);
        when(evidence.getSummary("actual")).thenReturn(dossier(TODAY, TODAY.minusDays(8)));
        assertThat(check(assess(), "EVIDENCE").state()).isEqualTo(State.MISSING);
        deal.setSourceUrl(null);
        assertThat(check(assess(), "LISTING").state()).isEqualTo(State.MISSING);
        deal.setTitle("Exemple fictif · annonce"); deal.setSourceUrl("https://agency.fr/x");
        assertThat(check(assess(), "LISTING").state()).isEqualTo(State.MISSING);
    }

    @Test
    void incompatibleRentalTaxRegimeFails() {
        var result = service.assess(request("30000", RentalMode.FURNISHED, TaxRegime.NU));
        assertThat(check(result, "RENTAL_MODE").state()).isEqualTo(State.FAIL);
    }

    private QualificationResponse assess() { return service.assess(request("30000", RentalMode.FURNISHED, TaxRegime.REEL_LMNP)); }
    private static Check check(QualificationResponse result, String key) { return result.checks().stream().filter(c -> c.key().equals(key)).findFirst().orElseThrow(); }
    private static QualificationRequest request(String downpayment, RentalMode mode, TaxRegime tax) {
        return new QualificationRequest(new SimulationRequest("actual", n(downpayment), n("3.5"), 20, tax,
                n("30"), n("4"), n("0"), n("180"), n("1.5"), n("1.2")), mode);
    }
    private static EvidenceSummary dossier(LocalDate date, LocalDate availability) {
        return new EvidenceSummary("actual", 10, 10, true, Arrays.stream(EvidenceField.values()).map(field ->
                new EvidenceSummary.Check(field, field.name(), "", "", EvidenceStatus.DOCUMENTED, "", "Document consulté",
                        field == EvidenceField.LISTING_AVAILABILITY ? availability : date, Instant.EPOCH, false, true)).toList());
    }
    private static List<RentalReference.View> refs(int count, boolean lease, LocalDate date, String town, String surface,
                                                  String rent, RentalMode mode) {
        return IntStream.range(0, count).mapToObj(i -> new RentalReference.View("ref" + i, "https://agency.fr/location/" + i,
                date, town, PropertyType.APARTMENT, n(surface), n(rent), mode,
                i == 0 && lease ? Kind.ACTUAL_LEASE : Kind.ASKING_RENT, "Quartier et état similaires — fixture fictive", Instant.EPOCH)).toList();
    }
    private static DvfMarketAnalysis snapshot(int year, int count, String median) {
        return new DvfMarketAnalysis(true, "Limoges", "87085", "Appartement", n("1600"), n(median), n("0"), count,
                year, "Fixture", "https://foncierdata.fr/test", null, List.of(), null, "Fixture fictive");
    }
    private static BigDecimal n(String value) { return new BigDecimal(value); }
}
