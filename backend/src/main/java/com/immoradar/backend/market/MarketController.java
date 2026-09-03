package com.immoradar.backend.market;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/market")
@CrossOrigin(origins = "*")
public class MarketController {

    private final DvfMarketService dvfMarketService;

    public MarketController(DvfMarketService dvfMarketService) {
        this.dvfMarketService = dvfMarketService;
    }

    @GetMapping("/deals/{dealId}/dvf")
    public DvfMarketAnalysis getDealMarketAnalysis(@PathVariable String dealId) {
        return dvfMarketService.analyzeDeal(dealId);
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
