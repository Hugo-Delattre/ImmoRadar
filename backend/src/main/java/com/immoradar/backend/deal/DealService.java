package com.immoradar.backend.deal;

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

    public Deal createDeal(com.immoradar.backend.deal.dto.CreateDealRequest request) {
        String id = java.util.UUID.randomUUID().toString();

        // Calcul automatique de l'Opportunity Score (0 à 10)
        double grossYield = (request.monthlyRent() * 12) / request.price() * 100;
        double netMonthly = request.monthlyRent() - request.monthlyCharges() - (request.propertyTax() / 12);
        
        double scoreYield = Math.min(5.0, (grossYield / 10.0) * 5.0);
        double scoreCashflow = Math.min(5.0, Math.max(0, (netMonthly / 400.0) * 5.0));
        double opportunityScore = Math.round((scoreYield + scoreCashflow) * 10.0) / 10.0;

        String image = (request.imageUrl() != null && !request.imageUrl().isBlank()) 
                ? request.imageUrl() 
                : "https://images.unsplash.com/photo-1560518883-ce09059eeffa?auto=format&fit=crop&w=800&q=80";

        Deal deal = new Deal(
                id,
                request.title(),
                request.price(),
                request.monthlyRent(),
                request.monthlyCharges(),
                request.propertyTax(),
                request.renovationCost(),
                request.location(),
                request.surface(),
                request.propertyType(),
                request.description() != null ? request.description() : "",
                opportunityScore,
                image
        );

        return dealRepository.save(deal);
    }
}
