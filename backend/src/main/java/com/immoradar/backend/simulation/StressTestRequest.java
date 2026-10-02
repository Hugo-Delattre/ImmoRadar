package com.immoradar.backend.simulation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record StressTestRequest(
        @NotNull @Valid SimulationRequest base,
        @NotNull @DecimalMin("0") @DecimalMax("40") BigDecimal rentDropPercent,
        @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal operatingCostIncreasePercent,
        @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal renovationIncreasePercent,
        @NotNull @DecimalMin("0") @DecimalMax("30") BigDecimal vacancyIncreasePoints
) {}
