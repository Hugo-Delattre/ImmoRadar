package com.immoradar.backend.qualification;

import com.immoradar.backend.deal.Deal;
import com.immoradar.backend.deal.DealService;
import com.immoradar.backend.evidence.DealEvidenceService;
import com.immoradar.backend.evidence.EvidenceField;
import com.immoradar.backend.market.DvfMarketService;
import com.immoradar.backend.simulation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import static com.immoradar.backend.qualification.QualificationResponse.*;

/** A versioned screening policy on declared evidence, never an independent certification. */
@Service
public class QualificationService {
    public static final String POLICY_VERSION = "potential-v1";
    private final DealService deals;
    private final DealEvidenceService evidence;
    private final RentalReferenceService references;
    private final DvfMarketService market;
    private final StressTestService stress;
    private final Clock clock;

    @Autowired
    public QualificationService(DealService deals, DealEvidenceService evidence, RentalReferenceService references,
                                DvfMarketService market, StressTestService stress) {
        this(deals, evidence, references, market, stress, Clock.systemUTC());
    }

    QualificationService(DealService deals, DealEvidenceService evidence, RentalReferenceService references,
                          DvfMarketService market, StressTestService stress, Clock clock) {
        this.deals = deals;
        this.evidence = evidence;
        this.references = references;
        this.market = market;
        this.stress = stress;
        this.clock = clock;
    }

    public QualificationResponse assess(QualificationRequest request) {
        String id = request.base().dealId();
        Deal deal = deals.getEntity(id);
        LocalDate today = LocalDate.now(clock);
        var dossier = evidence.getSummary(id);
        var checks = new ArrayList<Check>();
        boolean realListing = deal.getSourceUrl() != null && validSource(deal.getSourceUrl())
                && !deal.getTitle().toLowerCase(Locale.ROOT).contains("fictif")
                && !List.of("1", "2", "3", "4", "5").contains(deal.getId());
        checks.add(new Check("LISTING", "Annonce sourcée", realListing ? State.PASS : State.MISSING,
                realListing ? "Lien déclaré conservé ; contenu et disponibilité non contrôlés automatiquement."
                        : "Annonce HTTPS requise ; les exemples fictifs ne peuvent pas être qualifiés."));
        var outdated = dossier.checks().stream().filter(check -> !check.complete() || check.checkedOn() == null
                || check.checkedOn().isBefore(today.minusDays(check.field() == EvidenceField.LISTING_AVAILABILITY ? 7 : 365))
                || check.checkedOn().isAfter(today)).map(check -> check.label()).toList();
        boolean dossierComplete = dossier.readyForReview() && dossier.checks().size() == EvidenceField.values().length
                && outdated.isEmpty();
        checks.add(new Check("EVIDENCE", "Dossier documenté et récent", dossierComplete ? State.PASS : State.MISSING,
                dossierComplete ? "10 contrôles déclarés, ≤365 jours ; disponibilité ≤7 jours. Aucun document n'est authentifié."
                        : "Actualiser les justificatifs (≤365 jours, disponibilité ≤7 jours) : " + String.join(", ", outdated)));

        boolean modeCompatible = switch (request.base().taxRegime()) {
            case NU -> request.rentalMode() == RentalReferenceRequest.RentalMode.UNFURNISHED;
            case REEL_LMNP, MICRO_BIC -> request.rentalMode() == RentalReferenceRequest.RentalMode.FURNISHED;
            case SCI_IS -> true;
        };
        checks.add(new Check("RENTAL_MODE", "Location et simulation cohérentes", modeCompatible ? State.PASS : State.FAIL,
                "Les références doivent correspondre au mode meublé/non meublé simulé ; l'éligibilité fiscale n'est pas certifiée."));

        var seen = new HashSet<String>();
        var referenceChecks = references.list(id).stream().map(ref -> {
            String reason = eligibility(ref, deal, request, today);
            if (reason.isEmpty() && !seen.add(RentalReferenceService.sourceKey(ref.sourceUrl()))) reason = "Source répétée.";
            return new ReferenceCheck(ref, reason.isEmpty(), reason.isEmpty() ? "Comparable déclaré retenu." : reason);
        }).toList();
        var eligible = referenceChecks.stream().filter(ReferenceCheck::eligible).map(ReferenceCheck::reference).toList();
        long leases = eligible.stream().filter(ref -> ref.kind() == RentalReferenceRequest.Kind.ACTUAL_LEASE).count();
        boolean enoughRent = eligible.size() >= 3 && leases >= 1;
        checks.add(new Check("RENT_REFERENCES", "Références locatives pertinentes", enoughRent ? State.PASS : State.MISSING,
                eligible.size() + " retenue(s), " + leases + " bail(s) déclaré(s). Minimum 3 sources distinctes dont 1 bail ; "
                        + "même commune saisie, type, mode locatif, surface ±20 %, observation ≤180 jours. "
                        + "Les loyers d'annonce ne sont pas des loyers signés et les déclarations ne sont pas authentifiées."));
        BigDecimal supported = enoughRent ? median(eligible.stream().map(ref -> ref.monthlyRent()
                .divide(ref.surface(), 8, RoundingMode.HALF_UP)).toList())
                .min(median(eligible.stream().filter(ref -> ref.kind() == RentalReferenceRequest.Kind.ACTUAL_LEASE)
                        .map(ref -> ref.monthlyRent().divide(ref.surface(), 8, RoundingMode.HALF_UP)).toList()))
                .multiply(deal.getSurface()).setScale(2, RoundingMode.HALF_UP) : null;
        checks.add(new Check("RENT_LEVEL", "Loyer cible étayé hors charges",
                supported == null ? State.MISSING : deal.getMonthlyRent().compareTo(supported) <= 0 ? State.PASS : State.FAIL,
                supported == null ? "Impossible de valider le loyer sans références suffisantes."
                        : "Cible " + deal.getMonthlyRent() + " €/mois ; repère déclaré ajusté à la surface " + supported
                        + " €/mois HC (minimum des médianes de toutes les références et des baux). "
                        + "Ni estimation professionnelle ni intervalle de confiance ; quartier/état à revoir."));

        var analysis = market.analyzeDeal(id);
        boolean marketReady = analysis.available() && analysis.medianPricePerSquareMeter() != null
                && analysis.medianPricePerSquareMeter().signum() > 0 && analysis.comparableCount() >= 20
                && analysis.referenceYear() != null && analysis.referenceYear() >= today.getYear() - 3
                && analysis.referenceYear() <= today.getYear() && analysis.sourceUrl() != null && validSource(analysis.sourceUrl());
        checks.add(new Check("MARKET", "Repère de vente exploitable", marketReady ? State.PASS : State.MISSING,
                marketReady ? analysis.comparableCount() + " ventes ; millésime " + analysis.referenceYear()
                        + ". Agrégat communal via FoncierData : pas une expertise du bien ni une décote prouvée."
                        : "Repère indisponible/insuffisant : ≥20 ventes, millésime ≤3 ans et source requis. " + analysis.notice()));
        // Fixed server-side shocks: editable exploratory settings cannot weaken qualification.
        var fixedStress = new StressTestRequest(request.base(), new BigDecimal("10"), new BigDecimal("10"),
                new BigDecimal("15"), new BigDecimal("5"));
        var comparison = stress.compare(fixedStress);
        var prudent = comparison.scenarios().stream().filter(s -> s.key().equals("PRUDENT")).findFirst().orElseThrow();
        var adverse = comparison.scenarios().stream().filter(s -> s.key().equals("ADVERSE")).findFirst().orElseThrow();
        BigDecimal ceiling = marketReady ? analysis.medianPricePerSquareMeter().multiply(deal.getSurface())
                .multiply(new BigDecimal("0.90")).setScale(2, RoundingMode.HALF_UP) : null;
        checks.add(new Check("PRICE_MARGIN", "Marge sur le repère communal",
                ceiling == null ? State.MISSING : prudent.totalProjectCost().compareTo(ceiling) <= 0 ? State.PASS : State.FAIL,
                ceiling == null ? "Pas de conclusion de prix sans marché exploitable."
                        : "Coût prudent " + prudent.totalProjectCost() + " € (prix + notaire 7,5 % + travaux majorés) ; "
                        + "plafond de tri à 90 % du repère communal : " + ceiling + " €. État/rue non pris en compte."));
        checks.add(new Check("RESILIENCE", "Trésorerie robuste au financement choisi",
                prudent.monthlyCashFlow().compareTo(new BigDecimal("100")) >= 0 && adverse.monthlyCashFlow().signum() >= 0
                        ? State.PASS : State.FAIL,
                "Prudent " + prudent.monthlyCashFlow() + " €/mois (minimum 100) ; dégradé " + adverse.monthlyCashFlow()
                        + " €/mois (minimum 0). Chocs fixes : loyer −10 %, coûts +10 %, travaux +15 %, vacance +5 points ; "
                        + "dégradé = double. Apport, crédit et fiscalité : ceux de la simulation jointe."));
        // A known failure takes precedence over incomplete evidence, while every missing check remains visible.
        Outcome outcome = checks.stream().anyMatch(c -> c.state() == State.FAIL) ? Outcome.NOT_QUALIFIED
                : checks.stream().anyMatch(c -> c.state() == State.MISSING) ? Outcome.INCOMPLETE : Outcome.POTENTIAL;
        var snapshot = new DealSnapshot(deal.getTitle(), deal.getPrice(), deal.getMonthlyRent(), deal.getMonthlyCharges(),
                deal.getPropertyTax(), deal.getRenovationCost(), deal.getLocation(), deal.getSurface(), deal.getPropertyType(), deal.getSourceUrl());
        return new QualificationResponse(id, POLICY_VERSION, clock.instant(), outcome, false, snapshot, request, checks, supported,
                referenceChecks, dossier, analysis, comparison,
                "Présélection algorithmique sur données et références déclarées, pas certification indépendante ni garantie. "
                        + "Seuils de politique produit, pas normes officielles. Faire contrôler annonce, baux, diagnostics, "
                        + "copropriété, légalité du loyer et fiscalité. Assurance de prêt, garanties/frais de crédit, coûts "
                        + "exceptionnels et sortie non modélisés ; trésorerie réelle potentiellement inférieure. "
                        + "Toute modification ou nouvelle information impose un nouveau calcul.");
    }

    private static String eligibility(RentalReference.View ref, Deal deal, QualificationRequest request, LocalDate today) {
        if (!validSource(ref.sourceUrl())) return "Source HTTPS invalide.";
        if (ref.observedOn().isBefore(today.minusDays(180)) || ref.observedOn().isAfter(today)) return "Observation trop ancienne ou future.";
        if (!normalize(ref.location()).equals(normalize(deal.getLocation()))) return "Commune saisie différente.";
        if (ref.propertyType() != deal.getPropertyType()) return "Type de bien différent.";
        if (ref.rentalMode() != request.rentalMode()) return "Mode locatif différent.";
        if (ref.surface().compareTo(deal.getSurface().multiply(new BigDecimal("0.8"))) < 0
                || ref.surface().compareTo(deal.getSurface().multiply(new BigDecimal("1.2"))) > 0) return "Surface hors ±20 %.";
        return "";
    }

    private static boolean validSource(String source) {
        try { RentalReferenceService.sourceKey(source); return true; }
        catch (IllegalArgumentException ex) { return false; }
    }

    private static String normalize(String location) {
        return Normalizer.normalize(location, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT).trim().replaceAll("\\s+", " ");
    }

    private static BigDecimal median(List<BigDecimal> values) {
        var sorted = values.stream().sorted().toList();
        int middle = sorted.size() / 2;
        return sorted.size() % 2 == 1 ? sorted.get(middle)
                : sorted.get(middle - 1).add(sorted.get(middle)).divide(BigDecimal.TWO, 8, RoundingMode.HALF_UP);
    }
}
