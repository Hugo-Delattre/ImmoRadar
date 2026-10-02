package com.immoradar.backend.qualification;

import com.immoradar.backend.deal.PropertyType;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record RentalReferenceRequest(
        @NotBlank @Size(max = 2048) @Pattern(regexp = "https://[^\\s]+") String sourceUrl,
        @NotNull @PastOrPresent LocalDate observedOn,
        @NotBlank @Size(max = 120) String location,
        @NotNull PropertyType propertyType,
        @NotNull @DecimalMin("1") @DecimalMax("10000") BigDecimal surface,
        @NotNull @DecimalMin("1") @DecimalMax("100000") BigDecimal monthlyRent,
        @NotNull RentalMode rentalMode,
        @NotNull Kind kind,
        @NotBlank @Size(max = 1000) String note
) {
    public enum RentalMode { FURNISHED, UNFURNISHED }
    public enum Kind { ASKING_RENT, ACTUAL_LEASE }
}
