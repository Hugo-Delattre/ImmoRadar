package com.immoradar.backend.evidence;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record UpdateEvidenceRequest(
        @NotNull EvidenceStatus status,
        @NotNull @Size(max = 2048) @Pattern(regexp = "^(|https://[^\\s]+)$", message = "Le lien doit utiliser HTTPS") String sourceUrl,
        @NotNull @Size(max = 1000) String note,
        @NotNull @PastOrPresent LocalDate checkedOn
) {}
