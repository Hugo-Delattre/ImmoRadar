package com.immoradar.backend.deal;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.math.BigDecimal;

@Component
public class DatabaseSeeder implements CommandLineRunner {

    private final DealRepository dealRepository;
    private final DealService dealService;
    private final Clock clock;

    public DatabaseSeeder(DealRepository dealRepository, DealService dealService, Clock clock) {
        this.dealRepository = dealRepository;
        this.dealService = dealService;
        this.clock = clock;
    }

    @Override
    public void run(String... args) {
        if (dealRepository.count() == 0) {
            List<Deal> seedDeals = List.of(
                new Deal(
                    "1",
                    "Immeuble de Rapport - 4 Lots",
                    amount("245000"), amount("2150"), amount("180"), amount("1600"), amount("35000"),
                    "Saint-Étienne (42)",
                    amount("140"),
                    PropertyType.BUILDING,
                    "Immeuble de rapport composé de 2 studios et 2 T2 en parfait état. Tous les lots sont actuellement loués. Compteurs électriques individuels. Faible taxe foncière.",
                    9.2,
                    "https://images.unsplash.com/photo-1570129477492-45c003edd2be?auto=format&fit=crop&w=800&q=80",
                    true
                ),
                new Deal(
                    "2",
                    "Appartement T4 Spécial Colocation",
                    amount("135000"), amount("1200"), amount("110"), amount("950"), amount("15000"),
                    "Limoges (87)",
                    amount("78"),
                    PropertyType.APARTMENT,
                    "Appartement T4 proche des facultés. Aménagé en 3 chambres pour colocation étudiante. Vendu entièrement meublé et équipé. Rendement optimal immédiat.",
                    8.8,
                    "https://images.unsplash.com/photo-1502672260266-1c1ef2d93688?auto=format&fit=crop&w=800&q=80",
                    false
                ),
                new Deal(
                    "3",
                    "Studio meublé hyper-centre",
                    amount("89000"), amount("620"), amount("65"), amount("520"), amount("5000"),
                    "Mulhouse (68)",
                    amount("24"),
                    PropertyType.STUDIO,
                    "Studio entièrement rénové par un architecte d'intérieur. Emplacement numéro 1, à 2 minutes à pied de la gare et des commerces. Idéal LMNP.",
                    8.4,
                    "https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?auto=format&fit=crop&w=800&q=80",
                    true
                ),
                new Deal(
                    "4",
                    "Maison divisée en 2 appartements",
                    amount("195000"), amount("1480"), amount("120"), amount("1250"), amount("20000"),
                    "Le Mans (72)",
                    amount("115"),
                    PropertyType.HOUSE,
                    "Maison de ville divisée en un T3 avec jardin privatif et un T2 à l'étage. Entrées séparées. Fort potentiel de revente après découpe cadastrale officielle.",
                    7.9,
                    "https://images.unsplash.com/photo-1512917774080-9991f1c4c750?auto=format&fit=crop&w=800&q=80",
                    false
                ),
                new Deal(
                    "5",
                    "Petit immeuble de centre-ville",
                    amount("310000"), amount("2600"), amount("220"), amount("2100"), amount("45000"),
                    "Belfort (90)",
                    amount("180"),
                    PropertyType.BUILDING,
                    "Immeuble de rapport comprenant 5 appartements. Toiture refaite en 2024. Travaux de rafraîchissement à prévoir sur 2 appartements pour optimiser les loyers.",
                    8.1,
                    "https://images.unsplash.com/photo-1564013799919-ab600027ffc6?auto=format&fit=crop&w=800&q=80",
                    false
                )
            );
            enrich(seedDeals);
            dealRepository.saveAll(seedDeals);
        }
        // Les biens créés avant l'ajout du score détaillé sont recalculés à chaque démarrage.
        dealService.rescoreAll();
    }

    /** Ajoute DPE, ancienneté et historique de prix aux biens de démonstration pour illustrer le radar. */
    private void enrich(List<Deal> deals) {
        var today = LocalDate.now(clock);
        EnergyClass[] energy = {EnergyClass.D, EnergyClass.C, EnergyClass.E, EnergyClass.F, EnergyClass.D};
        int[] daysOnline = {124, 18, 47, 210, 63};
        for (int index = 0; index < deals.size(); index++) {
            var deal = deals.get(index);
            deal.setEnergyClass(energy[index]);
            deal.setListedOn(today.minusDays(daysOnline[index]));
            deal.setCreatedOn(today.minusDays(Math.min(daysOnline[index], 10)));
            deal.setStatus(DealStatus.TO_REVIEW);
        }
        // Immeuble de Saint-Étienne : deux baisses de prix depuis sa mise en ligne
        var building = deals.getFirst();
        building.getPriceHistory().add(new PricePoint(today.minusDays(124), amount("269000")));
        building.getPriceHistory().add(new PricePoint(today.minusDays(60), amount("255000")));
        building.getPriceHistory().add(new PricePoint(today.minusDays(12), building.getPrice()));
    }

    private static BigDecimal amount(String value) {
        return new BigDecimal(value);
    }
}
