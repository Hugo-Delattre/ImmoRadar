package com.immoradar.backend.market;

import com.immoradar.backend.deal.DealService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

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
        return dvfMarketService.analyze(deal.getLocation(), deal.getPrice(), deal.getSurface());
    }

    @GetMapping("/dvf")
    public DvfMarketAnalysis getCustomMarketAnalysis(
            @RequestParam String location,
            @RequestParam BigDecimal pricePerM2,
            @RequestParam BigDecimal totalPrice,
            @RequestParam BigDecimal surface) {
        return dvfMarketService.analyze(location, pricePerM2, totalPrice, surface);
    }
}
