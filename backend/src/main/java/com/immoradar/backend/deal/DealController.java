package com.immoradar.backend.deal;

import com.immoradar.backend.deal.dto.CreateDealRequest;
import com.immoradar.backend.deal.dto.DealResponse;
import com.immoradar.backend.deal.dto.DealSearchResponse;
import com.immoradar.backend.deal.dto.FavoriteRequest;
import com.immoradar.backend.deal.dto.StatusRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@Validated
@RestController
@RequestMapping("/api/deals")
public class DealController {

    private final DealService dealService;

    public DealController(DealService dealService) {
        this.dealService = dealService;
    }

    @GetMapping
    public DealSearchResponse search(
            @RequestParam(required = false) @PositiveOrZero @Nullable BigDecimal priceMax,
            @RequestParam(required = false) @PositiveOrZero @Nullable BigDecimal yieldMin,
            @RequestParam(required = false) @Nullable BigDecimal cashflowMin,
            @RequestParam(required = false) @Nullable String location,
            @RequestParam(defaultValue = "false") boolean favoritesOnly,
            @RequestParam(required = false) @Nullable String propertyType,
            @RequestParam(defaultValue = "false") boolean excludeEnergySieves,
            @RequestParam(required = false) @Nullable DealStatus status,
            @RequestParam(defaultValue = "SCORE") DealSort sort,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "24") @Min(1) @Max(100) int size) {
        var type = propertyType == null || propertyType.isBlank() ? null : PropertyType.fromValue(propertyType);
        var criteria = new DealSearchCriteria(
                priceMax, yieldMin, cashflowMin, location, favoritesOnly, type, excludeEnergySieves, status, sort);
        return dealService.search(criteria, page, size);
    }

    @GetMapping("/{dealId}")
    public DealResponse get(@PathVariable String dealId) {
        return dealService.get(dealId);
    }

    @PostMapping
    public ResponseEntity<DealResponse> create(@RequestBody @Valid CreateDealRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(dealService.create(request));
    }

    @PutMapping("/{dealId}")
    public DealResponse update(@PathVariable String dealId, @RequestBody @Valid CreateDealRequest request) {
        return dealService.update(dealId, request);
    }

    @PatchMapping("/{dealId}/favorite")
    public DealResponse setFavorite(@PathVariable String dealId, @RequestBody FavoriteRequest request) {
        return dealService.setFavorite(dealId, request.favorite());
    }

    @PatchMapping("/{dealId}/status")
    public DealResponse setStatus(@PathVariable String dealId, @RequestBody @Valid StatusRequest request) {
        return dealService.setStatus(dealId, request.status());
    }

    @DeleteMapping("/{dealId}")
    public ResponseEntity<Void> delete(@PathVariable String dealId) {
        dealService.delete(dealId);
        return ResponseEntity.noContent().build();
    }
}
