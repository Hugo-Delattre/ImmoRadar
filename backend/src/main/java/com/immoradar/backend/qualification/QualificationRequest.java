package com.immoradar.backend.qualification;

import com.immoradar.backend.simulation.SimulationRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record QualificationRequest(@NotNull @Valid SimulationRequest base,
                                   @NotNull RentalReferenceRequest.RentalMode rentalMode) {}
