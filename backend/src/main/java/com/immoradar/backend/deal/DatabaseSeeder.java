package com.immoradar.backend.deal;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.math.BigDecimal;

@Component
public class DatabaseSeeder implements CommandLineRunner {

    private final DealRepository dealRepository;

    public DatabaseSeeder(DealRepository dealRepository) {
        this.dealRepository = dealRepository;
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
            dealRepository.saveAll(seedDeals);
        }
    }

    private static BigDecimal amount(String value) {
        return new BigDecimal(value);
    }
}
