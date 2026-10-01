package com.immoradar.backend.deal.dto;

import com.immoradar.backend.deal.DealStatus;
import jakarta.validation.constraints.NotNull;

public record StatusRequest(@NotNull DealStatus status) {}
