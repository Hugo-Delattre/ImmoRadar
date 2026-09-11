package com.immoradar.backend.listing;

import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.URL;

public record ListingExtractRequest(
        @NotBlank(message = "L'URL de l'annonce est obligatoire.")
        String url
) {
}
