package com.immomanager.backend.deal;

import org.jspecify.annotations.Nullable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/deals")
public class DealController {

    private final DealService dealService;

    public DealController(DealService dealService) {
        this.dealService = dealService;
    }

    @GetMapping
    public List<Deal> getDeals(
            @RequestParam(required = false) @Nullable Double priceMax,
            @RequestParam(required = false) @Nullable Double yieldMin,
            @RequestParam(required = false) @Nullable Double cashflowMin,
            @RequestParam(required = false) @Nullable String location) {
        return dealService.getDeals(priceMax, yieldMin, cashflowMin, location);
    }
}
