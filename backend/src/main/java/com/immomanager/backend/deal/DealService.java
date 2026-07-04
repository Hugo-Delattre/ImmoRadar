package com.immomanager.backend.deal;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DealService {

    private final DealRepository dealRepository;

    public DealService(DealRepository dealRepository) {
        this.dealRepository = dealRepository;
    }

    public List<Deal> getDeals(
            @Nullable Double priceMax,
            @Nullable Double yieldMin,
            @Nullable Double cashflowMin,
            @Nullable String location) {
        
        List<Deal> allDeals = dealRepository.findAll();
        
        return allDeals.stream()
                .filter(deal -> {
                    if (priceMax != null && deal.getPrice() > priceMax) {
                        return false;
                    }
                    if (location != null && !location.isBlank() && 
                        !deal.getLocation().toLowerCase().contains(location.toLowerCase())) {
                        return false;
                    }
                    if (yieldMin != null) {
                        double grossYield = (deal.getMonthlyRent() * 12) / deal.getPrice() * 100;
                        if (grossYield < yieldMin) {
                            return false;
                        }
                    }
                    if (cashflowMin != null) {
                        double estimatedCashFlow = deal.getMonthlyRent() - deal.getMonthlyCharges() - (deal.getPropertyTax() / 12);
                        if (estimatedCashFlow < cashflowMin) {
                            return false;
                        }
                    }
                    return true;
                })
                .toList();
    }
}
