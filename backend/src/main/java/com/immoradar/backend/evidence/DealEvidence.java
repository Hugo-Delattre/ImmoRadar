package com.immoradar.backend.evidence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Version;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.time.LocalDate;

@Entity
public class DealEvidence {
    @Id
    private String id = "";
    @Column(nullable = false)
    private String dealId = "";
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EvidenceField field = EvidenceField.PRICE;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EvidenceStatus status = EvidenceStatus.UNVERIFIED;
    @Column(length = 2048, nullable = false)
    private String sourceUrl = "";
    @Column(length = 1000, nullable = false)
    private String note = "";
    @Column(length = 2048, nullable = false)
    private String valueSnapshot = "";
    private LocalDate checkedOn = LocalDate.MIN;
    private Instant updatedAt = Instant.EPOCH;
    @Version
    private @Nullable Long version;

    protected DealEvidence() {}

    public DealEvidence(String dealId, EvidenceField field) {
        this.id = dealId + ":" + field.name();
        this.dealId = dealId;
        this.field = field;
    }

    public void update(UpdateEvidenceRequest request, String valueSnapshot, Instant updatedAt) {
        this.status = request.status();
        this.sourceUrl = request.sourceUrl().trim();
        this.note = request.note().trim();
        this.checkedOn = request.checkedOn();
        this.valueSnapshot = valueSnapshot;
        this.updatedAt = updatedAt;
    }

    public EvidenceField getField() { return field; }
    public EvidenceStatus getStatus() { return status; }
    public String getSourceUrl() { return sourceUrl; }
    public String getNote() { return note; }
    public String getValueSnapshot() { return valueSnapshot; }
    public LocalDate getCheckedOn() { return checkedOn; }
    public Instant getUpdatedAt() { return updatedAt; }
}
