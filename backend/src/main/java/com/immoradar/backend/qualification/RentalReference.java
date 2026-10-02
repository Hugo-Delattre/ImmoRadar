package com.immoradar.backend.qualification;

import com.immoradar.backend.deal.PropertyType;
import jakarta.persistence.*;
import org.jspecify.annotations.Nullable;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(indexes = @Index(name = "rental_reference_deal_idx", columnList = "dealId"))
public class RentalReference {
    @Id private String id = "";
    @Column(nullable = false) private String dealId = "";
    @Column(nullable = false, length = 2048) private String sourceUrl = "";
    private LocalDate observedOn = LocalDate.MIN;
    @Column(nullable = false, length = 120) private String location = "";
    @Enumerated(EnumType.STRING) private PropertyType propertyType = PropertyType.APARTMENT;
    private BigDecimal surface = BigDecimal.ZERO;
    private BigDecimal monthlyRent = BigDecimal.ZERO;
    @Enumerated(EnumType.STRING) private RentalReferenceRequest.RentalMode rentalMode = RentalReferenceRequest.RentalMode.FURNISHED;
    @Enumerated(EnumType.STRING) private RentalReferenceRequest.Kind kind = RentalReferenceRequest.Kind.ASKING_RENT;
    @Column(nullable = false, length = 1000) private String note = "";
    private Instant recordedAt = Instant.EPOCH;
    @Version private @Nullable Long version;

    protected RentalReference() {}

    public RentalReference(String dealId, RentalReferenceRequest request, Instant recordedAt) {
        id = UUID.randomUUID().toString();
        this.dealId = dealId;
        sourceUrl = request.sourceUrl().trim();
        observedOn = request.observedOn();
        location = request.location().trim();
        propertyType = request.propertyType();
        surface = request.surface();
        monthlyRent = request.monthlyRent();
        rentalMode = request.rentalMode();
        kind = request.kind();
        note = request.note().trim();
        this.recordedAt = recordedAt;
    }

    public View view() {
        return new View(id, sourceUrl, observedOn, location, propertyType, surface, monthlyRent,
                rentalMode, kind, note, recordedAt);
    }

    public record View(String id, String sourceUrl, LocalDate observedOn, String location,
                       PropertyType propertyType, BigDecimal surface, BigDecimal monthlyRent,
                       RentalReferenceRequest.RentalMode rentalMode, RentalReferenceRequest.Kind kind,
                       String note, Instant recordedAt) {}
}
