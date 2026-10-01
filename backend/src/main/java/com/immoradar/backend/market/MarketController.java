package com.immoradar.backend.market;

import com.immoradar.backend.deal.DealService;
import com.immoradar.backend.deal.PropertyType;
import jakarta.validation.constraints.Positive;
import org.jspecify.annotations.Nullable;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@Validated
@RestController
@RequestMapping("/api/market")
public class MarketController {

    private final DvfMarketService dvfMarketService;
    private final DealService dealService;

    public MarketController(DvfMarketService dvfMarketService, DealService dealService) {
        this.dvfMarketService = dvfMarketService;
        this.dealService = dealService;
    }

    @GetMapping("/deals/{dealId}/dvf")
    public DvfMarketAnalysis getDealMarketAnalysis(@PathVariable String dealId) {
        var deal = dealService.getEntity(dealId);
        return dvfMarketService.analyze(deal.getLocation(), deal.getPrice(), deal.getSurface(), deal.getPropertyType());
    }

    @GetMapping("/dvf")
    public DvfMarketAnalysis getCustomMarketAnalysis(
            @RequestParam String location,
            @RequestParam @Positive BigDecimal totalPrice,
            @RequestParam @Positive BigDecimal surface,
            @RequestParam(required = false) @Nullable String propertyType) {
        var type = propertyType == null || propertyType.isBlank() ? null : PropertyType.fromValue(propertyType);
        return dvfMarketService.analyze(location, totalPrice, surface, type);
    }
}
