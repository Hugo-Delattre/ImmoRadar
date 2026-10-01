package com.immoradar.backend.evidence;

import org.jspecify.annotations.Nullable;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record EvidenceSummary(String dealId, int documentedCount, int requiredCount,
                              boolean readyForReview, List<Check> checks) {
    public EvidenceSummary { checks = List.copyOf(checks); }

    public record Check(EvidenceField field, String label, String guidance, String currentValue,
                        EvidenceStatus status, String sourceUrl, String note,
                        @Nullable LocalDate checkedOn, @Nullable Instant updatedAt,
                        boolean stale, boolean complete) {}
}
