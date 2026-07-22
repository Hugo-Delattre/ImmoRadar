package com.immoradar.backend.deal;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

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
                    "Immeuble de Rapport - 4 Lots",
                    245000,
                    2150,
                    180,
                    1600,
                    35000,
                    "Saint-Étienne (42)",
                    140,
                    PropertyType.BUILDING,
                    "Immeuble de rapport composé de 2 studios et 2 T2 en parfait état. Tous les lots sont actuellement loués. Compteurs électriques individuels. Faible taxe foncière.",
                    9.2,
                    "https://images.unsplash.com/photo-1570129477492-45c003edd2be?auto=format&fit=crop&w=800&q=80"
                ),
                new Deal(
                    "2",
                    "Appartement T4 Spécial Colocation",
                    135000,
                    1200,
                    110,
                    950,
                    15000,
                    "Limoges (87)",
                    78,
                    PropertyType.APARTMENT,
                    "Appartement T4 proche des facultés. Aménagé en 3 chambres pour colocation étudiante. Vendu entièrement meublé et équipé. Rendement optimal immédiat.",
                    8.8,
                    "https://images.unsplash.com/photo-1502672260266-1c1ef2d93688?auto=format&fit=crop&w=800&q=80"
                ),
                new Deal(
                    "3",
                    "Studio meublé hyper-centre",
                    89000,
                    620,
                    65,
                    520,
                    5000,
                    "Mulhouse (68)",
                    24,
                    PropertyType.STUDIO,
                    "Studio entièrement rénové par un architecte d'intérieur. Emplacement numéro 1, à 2 minutes à pied de la gare et des commerces. Idéal LMNP.",
                    8.4,
                    "https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?auto=format&fit=crop&w=800&q=80"
                ),
                new Deal(
                    "4",
                    "Maison divisée en 2 appartements",
                    195000,
                    1480,
                    120,
                    1250,
                    20000,
                    "Le Mans (72)",
                    115,
                    PropertyType.HOUSE,
                    "Maison de ville divisée en un T3 avec jardin privatif et un T2 à l'étage. Entrées séparées. Fort potentiel de revente après découpe cadastrale officielle.",
                    7.9,
                    "https://images.unsplash.com/photo-1512917774080-9991f1c4c750?auto=format&fit=crop&w=800&q=80"
                ),
                new Deal(
                    "5",
                    "Petit immeuble de centre-ville",
                    310000,
                    2600,
                    220,
                    2100,
                    45000,
                    "Belfort (90)",
                    180,
                    PropertyType.BUILDING,
                    "Immeuble de rapport comprenant 5 appartements. Toiture refaite en 2024. Travaux de rafraîchissement à prévoir sur 2 appartements pour optimiser les loyers.",
                    8.1,
                    "https://images.unsplash.com/photo-1564013799919-ab600027ffc6?auto=format&fit=crop&w=800&q=80"
                )
            );
            dealRepository.saveAll(seedDeals);
        }
    }
}
