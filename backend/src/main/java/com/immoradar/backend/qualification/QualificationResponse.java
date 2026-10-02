package com.immoradar.backend.qualification;

import com.immoradar.backend.evidence.EvidenceSummary;
import com.immoradar.backend.deal.PropertyType;
import com.immoradar.backend.market.DvfMarketAnalysis;
import com.immoradar.backend.simulation.StressTestResponse;
import org.jspecify.annotations.Nullable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record QualificationResponse(String dealId, String policyVersion, Instant assessedAt,
        Outcome outcome, boolean certified, DealSnapshot deal, QualificationRequest assumptions, List<Check> checks,
        @Nullable BigDecimal supportedMonthlyRent, List<ReferenceCheck> rentalReferences,
        EvidenceSummary evidence, DvfMarketAnalysis market, StressTestResponse stress, String notice) {
    public QualificationResponse {
        checks = List.copyOf(checks);
        rentalReferences = List.copyOf(rentalReferences);
    }
    public enum Outcome { POTENTIAL, INCOMPLETE, NOT_QUALIFIED }
    public enum State { PASS, MISSING, FAIL }
    public record DealSnapshot(String title, BigDecimal price, BigDecimal monthlyRent, BigDecimal monthlyCharges,
                               BigDecimal propertyTax, BigDecimal renovationCost, String location, BigDecimal surface,
                               PropertyType propertyType, @Nullable String sourceUrl) {}
    public record Check(String key, String label, State state, String detail) {}
    public record ReferenceCheck(RentalReference.View reference, boolean eligible, String reason) {}
}
