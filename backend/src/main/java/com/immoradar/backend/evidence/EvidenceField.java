package com.immoradar.backend.evidence;

public enum EvidenceField {
    PRICE("Prix d'achat", "Annonce datée, confirmation du vendeur ou proposition écrite."),
    SURFACE("Surface", "Mesurage, plan ou document précisant la surface retenue."),
    RENT("Loyer cible", "Bail existant ou références locatives locales avec dates et caractéristiques."),
    CHARGES("Charges", "Décompte annuel et distinction des charges récupérables."),
    PROPERTY_TAX("Taxe foncière", "Dernier avis, avec distinction de la taxe d'enlèvement des ordures ménagères."),
    RENOVATION("Budget travaux", "Devis et provision pour imprévus, ou justification de l'absence de travaux."),
    DPE("Diagnostic énergétique", "Diagnostic complet, date et numéro de référence."),
    COOWNERSHIP("Copropriété", "PV d'assemblée, charges et travaux votés ; préciser si le bien est hors copropriété."),
    RENTAL_DEMAND("Demande locative", "Références locales et hypothèse de vacance argumentée."),
    LISTING_AVAILABILITY("Disponibilité de l'annonce", "Confirmation du vendeur ou de l'agence dans les 30 derniers jours.");

    private final String label;
    private final String guidance;

    EvidenceField(String label, String guidance) {
        this.label = label;
        this.guidance = guidance;
    }

    public String label() { return label; }
    public String guidance() { return guidance; }
}
