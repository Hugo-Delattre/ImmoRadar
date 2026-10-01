package com.immoradar.backend.market;

import com.immoradar.backend.deal.PropertyType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class DvfMarketService {
    private final MarketDataClient marketDataClient;

    public DvfMarketService(MarketDataClient marketDataClient) {
        this.marketDataClient = marketDataClient;
    }

    public DvfMarketAnalysis analyze(String location, BigDecimal pricePerSquareMeter,
                                      PropertyType propertyType, BigDecimal surface) {
        if (location == null || location.isBlank() || pricePerSquareMeter == null
                || pricePerSquareMeter.signum() <= 0 || surface == null || surface.signum() <= 0) {
            throw new IllegalArgumentException("Une commune, un prix au m² et une surface positifs sont requis.");
        }

        String category = switch (propertyType) {
            case STUDIO, APARTMENT -> "Appartement";
            case HOUSE -> "Maison";
            case BUILDING -> null;
        };
        if (category == null) {
            return DvfMarketAnalysis.unavailable(location, pricePerSquareMeter,
                    "Un immeuble entier n'est pas comparable aux ventes d'un logement individuel.");
        }

        var snapshot = marketDataClient.findComparable(location, category, surface);
        if (snapshot.isEmpty()) {
            return DvfMarketAnalysis.unavailable(location, pricePerSquareMeter,
                    "Aucune référence vérifiable et suffisamment comparable n'est disponible pour ce bien.");
        }

        var data = snapshot.get();
        var difference = pricePerSquareMeter.subtract(data.medianPricePerSquareMeter())
                .multiply(BigDecimal.valueOf(100))
                .divide(data.medianPricePerSquareMeter(), 1, RoundingMode.HALF_UP);
        return new DvfMarketAnalysis(true, data.location(), data.codeInsee(), category,
                pricePerSquareMeter, data.medianPricePerSquareMeter(), difference,
                data.comparableCount(), data.referenceYear(), data.reliability(),
                data.sourceUrl(), data.methodologyUrl(),
                "Médiane communale de ventes individuelles comparables. L'état, la rue, les travaux et le loyer ne sont pas pris en compte.");
    }
}
