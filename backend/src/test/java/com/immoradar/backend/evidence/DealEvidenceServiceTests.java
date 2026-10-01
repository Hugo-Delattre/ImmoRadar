package com.immoradar.backend.evidence;

import com.immoradar.backend.deal.Deal;
import com.immoradar.backend.deal.DealService;
import com.immoradar.backend.deal.PropertyType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DealEvidenceServiceTests {
    private final DealService deals = mock(DealService.class);
    private final DealEvidenceRepository repository = mock(DealEvidenceRepository.class);
    private final DealEvidenceService service = new DealEvidenceService(deals, repository);
    private final HashMap<String, DealEvidence> saved = new HashMap<>();
    private final Deal deal = new Deal("test", "Appartement", new BigDecimal("100000"), new BigDecimal("700"),
            new BigDecimal("80"), new BigDecimal("700"), BigDecimal.ZERO, "Limoges (87)", new BigDecimal("50"),
            PropertyType.APARTMENT, "", 0, "", false);

    @BeforeEach
    void setUp() {
        when(deals.getEntity("test")).thenReturn(deal);
        when(repository.findById(anyString())).thenAnswer(invocation -> Optional.ofNullable(saved.get(invocation.getArgument(0))));
        when(repository.findAllByDealId("test")).thenAnswer(invocation -> saved.values().stream().toList());
        when(repository.saveAndFlush(any())).thenAnswer(invocation -> {
            DealEvidence evidence = invocation.getArgument(0);
            saved.put("test:" + evidence.getField().name(), evidence);
            return evidence;
        });
    }

    @Test
    void estimatesAndObservationsDoNotCompleteTheDossier() {
        service.update("test", EvidenceField.RENT, request(EvidenceStatus.ESTIMATED, "Hypothèse personnelle", LocalDate.now()));
        var summary = service.update("test", EvidenceField.PRICE, request(EvidenceStatus.OBSERVED, "Prix indiqué dans l'annonce", LocalDate.now()));
        assertThat(summary.documentedCount()).isZero();
        assertThat(summary.readyForReview()).isFalse();
        assertThat(summary.checks()).hasSize(10);
    }

    @Test
    void documentationNeedsANoteAndOnlyCoownershipMayBeWaived() {
        assertThatThrownBy(() -> service.update("test", EvidenceField.RENT, request(EvidenceStatus.DOCUMENTED, "  ", LocalDate.now())))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.update("test", EvidenceField.DPE, request(EvidenceStatus.NOT_APPLICABLE, "Sans diagnostic", LocalDate.now())))
                .isInstanceOf(IllegalArgumentException.class);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void changedNumbersInvalidateEarlierDocumentation() {
        service.update("test", EvidenceField.RENT, request(EvidenceStatus.DOCUMENTED, "Bail fourni par le vendeur", LocalDate.now()));
        deal.setMonthlyRent(new BigDecimal("950"));
        var summary = service.getSummary("test");
        assertThat(summary.documentedCount()).isZero();
        assertThat(summary.checks()).filteredOn(check -> check.field() == EvidenceField.RENT)
                .allMatch(check -> check.stale() && !check.complete());
    }

    @Test
    void oldAvailabilityConfirmationMustBeRenewed() {
        var summary = service.update("test", EvidenceField.LISTING_AVAILABILITY,
                request(EvidenceStatus.DOCUMENTED, "Agent contacté", LocalDate.now().minusDays(31)));
        assertThat(summary.documentedCount()).isZero();
        assertThat(summary.checks()).filteredOn(check -> check.field() == EvidenceField.LISTING_AVAILABILITY)
                .allMatch(EvidenceSummary.Check::stale);
    }

    @Test
    void fullDocumentedDossierCanBeReviewedButIsNotAnInvestmentCertification() {
        for (EvidenceField field : EvidenceField.values()) {
            service.update("test", field, request(field == EvidenceField.COOWNERSHIP
                    ? EvidenceStatus.NOT_APPLICABLE : EvidenceStatus.DOCUMENTED, "Référence consultée et consignée", LocalDate.now()));
        }
        var summary = service.getSummary("test");
        assertThat(summary.readyForReview()).isTrue();
        assertThat(summary.documentedCount()).isEqualTo(summary.requiredCount());
    }

    private UpdateEvidenceRequest request(EvidenceStatus status, String note, LocalDate date) {
        return new UpdateEvidenceRequest(status, "", note, date);
    }
}
