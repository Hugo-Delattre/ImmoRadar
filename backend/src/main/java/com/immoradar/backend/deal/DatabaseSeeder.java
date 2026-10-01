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
                    "Exemple fictif · Immeuble de 4 lots",
                    amount("245000"), amount("2150"), amount("180"), amount("1600"), amount("35000"),
                    "Saint-Étienne (42)",
                    amount("140"),
                    PropertyType.BUILDING,
                    "Immeuble de rapport composé de 2 studios et 2 T2 en parfait état. Tous les lots sont actuellement loués. Compteurs électriques individuels. Faible taxe foncière.",
                    9.2,
                    "/images/property-placeholder.svg",
                    true
                ),
                new Deal(
                    "2",
                    "Exemple fictif · Appartement T4",
                    amount("135000"), amount("1200"), amount("110"), amount("950"), amount("15000"),
                    "Limoges (87)",
                    amount("78"),
                    PropertyType.APARTMENT,
                    "Appartement T4 proche des facultés. Aménagé en 3 chambres pour colocation étudiante. Vendu entièrement meublé et équipé. Rendement optimal immédiat.",
                    8.8,
                    "/images/property-placeholder.svg",
                    false
                ),
                new Deal(
                    "3",
                    "Exemple fictif · Studio meublé",
                    amount("89000"), amount("620"), amount("65"), amount("520"), amount("5000"),
                    "Mulhouse (68)",
                    amount("24"),
                    PropertyType.STUDIO,
                    "Studio entièrement rénové par un architecte d'intérieur. Emplacement numéro 1, à 2 minutes à pied de la gare et des commerces. Idéal LMNP.",
                    8.4,
                    "/images/property-placeholder.svg",
                    true
                ),
                new Deal(
                    "4",
                    "Exemple fictif · Maison divisée",
                    amount("195000"), amount("1480"), amount("120"), amount("1250"), amount("20000"),
                    "Le Mans (72)",
                    amount("115"),
                    PropertyType.HOUSE,
                    "Maison de ville divisée en un T3 avec jardin privatif et un T2 à l'étage. Entrées séparées. Fort potentiel de revente après découpe cadastrale officielle.",
                    7.9,
                    "/images/property-placeholder.svg",
                    false
                ),
                new Deal(
                    "5",
                    "Exemple fictif · Petit immeuble",
                    amount("310000"), amount("2600"), amount("220"), amount("2100"), amount("45000"),
                    "Belfort (90)",
                    amount("180"),
                    PropertyType.BUILDING,
                    "Immeuble de rapport comprenant 5 appartements. Toiture refaite en 2024. Travaux de rafraîchissement à prévoir sur 2 appartements pour optimiser les loyers.",
                    8.1,
                    "/images/property-placeholder.svg",
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
