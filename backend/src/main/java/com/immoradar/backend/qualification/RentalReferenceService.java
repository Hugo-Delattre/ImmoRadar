package com.immoradar.backend.qualification;

import com.immoradar.backend.deal.DealService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class RentalReferenceService {
    private final DealService deals;
    private final RentalReferenceRepository references;

    public RentalReferenceService(DealService deals, RentalReferenceRepository references) {
        this.deals = deals;
        this.references = references;
    }

    public List<RentalReference.View> list(String dealId) {
        deals.getEntity(dealId);
        return references.findAllByDealIdOrderByRecordedAtDesc(dealId).stream().map(RentalReference::view).toList();
    }

    @Transactional
    public List<RentalReference.View> add(String dealId, RentalReferenceRequest request) {
        var existing = list(dealId);
        String key = sourceKey(request.sourceUrl());
        if (existing.size() >= 20) throw new IllegalArgumentException("Maximum 20 références par bien.");
        if (existing.stream().anyMatch(ref -> sourceKey(ref.sourceUrl()).equals(key)))
            throw new IllegalArgumentException("Cette source est déjà renseignée (paramètres et fragment ignorés).");
        references.saveAndFlush(new RentalReference(dealId, request, Instant.now()));
        return list(dealId);
    }

    @Transactional
    public List<RentalReference.View> remove(String dealId, String referenceId) {
        deals.getEntity(dealId);
        var reference = references.findByIdAndDealId(referenceId, dealId)
                .orElseThrow(() -> new IllegalArgumentException("Référence introuvable pour ce bien."));
        references.delete(reference);
        references.flush();
        return list(dealId);
    }

    /** Conservative identity: tracking/query variations cannot increase the sample size. No URL is fetched. */
    static String sourceKey(String source) {
        URI uri;
        try { uri = URI.create(source.trim()); }
        catch (IllegalArgumentException ex) { throw new IllegalArgumentException("Lien HTTPS invalide."); }
        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null || uri.getUserInfo() != null)
            throw new IllegalArgumentException("Une source HTTPS avec un domaine valide est requise.");
        return uri.getHost().toLowerCase(Locale.ROOT) + (uri.getPort() < 0 ? "" : ":" + uri.getPort())
                + (uri.getPath() == null ? "" : uri.getPath().replaceAll("/+$", ""));
    }
}
