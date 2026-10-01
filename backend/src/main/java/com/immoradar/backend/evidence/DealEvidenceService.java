package com.immoradar.backend.evidence;

import com.immoradar.backend.deal.Deal;
import com.immoradar.backend.deal.DealService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.EnumMap;

@Service
@Transactional(readOnly = true)
public class DealEvidenceService {
    private final DealService deals;
    private final DealEvidenceRepository repository;

    public DealEvidenceService(DealService deals, DealEvidenceRepository repository) {
        this.deals = deals;
        this.repository = repository;
    }

    public EvidenceSummary getSummary(String dealId) {
        Deal deal = deals.getEntity(dealId);
        var saved = new EnumMap<EvidenceField, DealEvidence>(EvidenceField.class);
        repository.findAllByDealId(dealId).forEach(item -> saved.put(item.getField(), item));
        var checks = Arrays.stream(EvidenceField.values()).map(field -> {
            String value = currentValue(deal, field);
            DealEvidence item = saved.get(field);
            if (item == null) return new EvidenceSummary.Check(field, field.label(), field.guidance(), value,
                    EvidenceStatus.UNVERIFIED, "", "", null, null, false, false);
            boolean stale = !snapshot(deal, field).equals(item.getValueSnapshot())
                    || (field == EvidenceField.LISTING_AVAILABILITY && item.getCheckedOn().isBefore(LocalDate.now().minusDays(30)));
            boolean complete = !stale && (item.getStatus() == EvidenceStatus.DOCUMENTED
                    || (field == EvidenceField.COOWNERSHIP && item.getStatus() == EvidenceStatus.NOT_APPLICABLE));
            return new EvidenceSummary.Check(field, field.label(), field.guidance(), value, item.getStatus(),
                    item.getSourceUrl(), item.getNote(), item.getCheckedOn(), item.getUpdatedAt(), stale, complete);
        }).toList();
        int documented = (int) checks.stream().filter(EvidenceSummary.Check::complete).count();
        return new EvidenceSummary(dealId, documented, checks.size(), documented == checks.size(), checks);
    }

    @Transactional
    public EvidenceSummary update(String dealId, EvidenceField field, UpdateEvidenceRequest request) {
        Deal deal = deals.getEntity(dealId);
        if (request.status() == EvidenceStatus.NOT_APPLICABLE && field != EvidenceField.COOWNERSHIP) {
            throw new IllegalArgumentException("Seule la copropriété peut être déclarée non applicable.");
        }
        if (request.status() != EvidenceStatus.UNVERIFIED && request.note().isBlank()) {
            throw new IllegalArgumentException("Précise le justificatif, l'observation ou la méthode d'estimation dans la note.");
        }
        var evidence = repository.findById(dealId + ":" + field.name()).orElseGet(() -> new DealEvidence(dealId, field));
        evidence.update(request, snapshot(deal, field), Instant.now());
        repository.saveAndFlush(evidence);
        return getSummary(dealId);
    }

    private static String currentValue(Deal deal, EvidenceField field) {
        return switch (field) {
            case PRICE -> decimal(deal.getPrice());
            case SURFACE -> decimal(deal.getSurface());
            case RENT -> decimal(deal.getMonthlyRent());
            case CHARGES -> decimal(deal.getMonthlyCharges());
            case PROPERTY_TAX -> decimal(deal.getPropertyTax());
            case RENOVATION -> decimal(deal.getRenovationCost());
            default -> "";
        };
    }

    private static String decimal(BigDecimal value) { return value.stripTrailingZeros().toPlainString(); }

    private static String snapshot(Deal deal, EvidenceField field) {
        return field == EvidenceField.LISTING_AVAILABILITY
                ? (deal.getSourceUrl() == null ? "" : deal.getSourceUrl()) : currentValue(deal, field);
    }
}
