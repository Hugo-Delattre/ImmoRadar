package com.immoradar.backend.market;

import com.immoradar.backend.deal.DealService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.RoundingMode;

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
        var pricePerSquareMeter = deal.getPrice().divide(deal.getSurface(), 0, RoundingMode.HALF_UP);
        return dvfMarketService.analyze(deal.getLocation(), pricePerSquareMeter, deal.getPropertyType(), deal.getSurface());
    }
}
