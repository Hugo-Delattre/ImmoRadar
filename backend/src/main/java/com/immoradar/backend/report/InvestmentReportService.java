package com.immoradar.backend.report;

import com.immoradar.backend.deal.Deal;
import com.immoradar.backend.deal.DealService;
import com.immoradar.backend.simulation.ProjectionPoint;
import com.immoradar.backend.simulation.FinancialSimulationService;
import com.immoradar.backend.simulation.SimulationRequest;
import com.immoradar.backend.simulation.SimulationResponse;
import org.openpdf.text.Document;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.FontFactory;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
public class InvestmentReportService {

    private static final Color INK = new Color(18, 32, 28);
    private static final Color MUTED = new Color(102, 119, 112);
    private static final Color LIME = new Color(185, 238, 96);
    private static final Color PALE = new Color(239, 246, 242);
    private final DealService dealService;
    private final FinancialSimulationService simulationService;

    public InvestmentReportService(DealService dealService, FinancialSimulationService simulationService) {
        this.dealService = dealService;
        this.simulationService = simulationService;
    }

    public byte[] generate(SimulationRequest request) {
        Deal deal = dealService.getEntity(request.dealId());
        SimulationResponse simulation = simulationService.simulate(request);
        var output = new ByteArrayOutputStream();
        var document = new Document(PageSize.A4, 42, 42, 42, 42);

        PdfWriter.getInstance(document, output);
        document.addTitle("Dossier d'investissement - " + deal.getTitle());
        document.addAuthor("ImmoRadar");
        document.addSubject("Analyse financière prévisionnelle d'un investissement locatif");
        document.open();

        addCover(document, deal, simulation);
        addSectionTitle(document, "Synthèse de l'opportunité");
        document.add(metricTable(simulation));
        addSectionTitle(document, "Bien et financement");
        document.add(financingTable(deal, request, simulation));
        document.newPage();
        addSectionTitle(document, "Projection patrimoniale");
        document.add(projectionTable(simulation));
        addProjectionReading(document, simulation);
        addDisclaimer(document);

        document.close();
        return output.toByteArray();
    }

    private void addCover(Document document, Deal deal, SimulationResponse simulation) {
        var brand = new Paragraph("IMMORADAR", font(11, Font.BOLD, INK));
        brand.setSpacingAfter(34);
        document.add(brand);

        var eyebrow = new Paragraph("DOSSIER D'INVESTISSEMENT LOCATIF", font(9, Font.BOLD, MUTED));
        eyebrow.setSpacingAfter(9);
        document.add(eyebrow);

        var title = new Paragraph(deal.getTitle(), font(28, Font.BOLD, INK));
        title.setLeading(32);
        title.setSpacingAfter(8);
        document.add(title);

        var location = new Paragraph(deal.getLocation() + "  •  " + deal.getSurface() + " m²", font(11, Font.NORMAL, MUTED));
        location.setSpacingAfter(24);
        document.add(location);

        var highlight = new PdfPTable(new float[]{1.2f, 1f, 1.2f});
        highlight.setWidthPercentage(100);
        highlight.addCell(highlightCell("Cash-flow mensuel", signedCurrency(simulation.monthlyCashFlow())));
        highlight.addCell(highlightCell("Rendement net", simulation.netYield() + " %"));
        highlight.addCell(highlightCell("TRI (Horizon " + simulation.projection().size() + " ans)", simulation.internalRateOfReturn() + " %"));
        highlight.setSpacingAfter(15);
        document.add(highlight);

        var date = LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.FRANCE));
        var generated = new Paragraph("Analyse générée le " + date + " • Hypothèses modifiables dans ImmoRadar", font(8, Font.NORMAL, MUTED));
        generated.setSpacingAfter(28);
        document.add(generated);
    }

    private PdfPTable metricTable(SimulationResponse simulation) {
        var table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setSpacingAfter(24);
        table.addCell(metricCell("Coût total", currency(simulation.totalProjectCost())));
        table.addCell(metricCell("Montant financé", currency(simulation.loanAmount())));
        table.addCell(metricCell("Mensualité crédit", currency(simulation.monthlyMortgage())));
        table.addCell(metricCell("Rendement brut", simulation.grossYield() + " %"));
        table.addCell(metricCell("TRI (IRR)", simulation.internalRateOfReturn() + " %"));
        table.addCell(metricCell("VAN (NPV à 4%)", signedCurrency(simulation.netPresentValue())));
        table.addCell(metricCell("Fiscalité annuelle", currency(simulation.taxAnnual())));
        table.addCell(metricCell("Loyer d'équilibre", currency(simulation.breakEvenRent())));
        return table;
    }

    private PdfPTable financingTable(Deal deal, SimulationRequest request, SimulationResponse simulation) {
        var table = new PdfPTable(new float[]{1.5f, 1f});
        table.setWidthPercentage(100);
        table.setSpacingAfter(24);
        addRow(table, "Prix d'achat", currency(deal.getPrice()));
        addRow(table, "Frais de notaire estimés (7.5%)", currency(deal.getPrice().multiply(new BigDecimal("0.075"))));
        addRow(table, "Travaux prévus", currency(deal.getRenovationCost()));
        addRow(table, "Apport personnel", currency(request.downpayment()));
        addRow(table, "Taux nominal / durée", request.interestRate() + " % / " + request.loanTermYears() + " ans");
        addRow(table, "Taux d'effort bancaire (HCSF)", simulation.debtEffortRatio() + " % (max 35%)");
        addRow(table, "Régime fiscal sélectionné", request.taxRegime().name().replace('_', ' '));
        addRow(table, "Vacance / gestion d'agence", request.vacancyRate() + " % / " + request.managementRate() + " %");
        addRow(table, "Charges d'exploitation annuelles", currency(simulation.annualOperatingExpenses()));
        return table;
    }

    private PdfPTable projectionTable(SimulationResponse simulation) {
        var table = new PdfPTable(new float[]{.5f, 1.2f, 1.2f, 1.2f, 1.2f});
        table.setWidthPercentage(100);
        table.setHeaderRows(1);
        for (String label : new String[]{"Année", "Cash-flow", "Capital restant", "Valeur du bien", "Patrimoine net"}) {
            table.addCell(headerCell(label));
        }
        for (ProjectionPoint point : simulation.projection()) {
            if (point.year() == 1 || point.year() % 5 == 0 || point.year() == simulation.projection().size()) {
                table.addCell(bodyCell(Integer.toString(point.year()), Element.ALIGN_CENTER));
                table.addCell(bodyCell(currency(point.annualCashFlow()), Element.ALIGN_RIGHT));
                table.addCell(bodyCell(currency(point.remainingLoan()), Element.ALIGN_RIGHT));
                table.addCell(bodyCell(currency(point.estimatedPropertyValue()), Element.ALIGN_RIGHT));
                table.addCell(bodyCell(currency(point.netWorth()), Element.ALIGN_RIGHT));
            }
        }
        table.setSpacingAfter(28);
        return table;
    }

    private void addSectionTitle(Document document, String text) {
        var title = new Paragraph(text, font(14, Font.BOLD, INK));
        title.setSpacingBefore(4);
        title.setSpacingAfter(12);
        document.add(title);
    }

    private void addDisclaimer(Document document) {
        var line = new Paragraph(
                "Document d'aide à la décision, non contractuel. Les projections dépendent des hypothèses saisies et ne constituent ni un conseil fiscal, ni une offre de financement. Faites valider votre montage par les professionnels compétents.",
                font(7, Font.NORMAL, MUTED));
        line.setLeading(10);
        document.add(line);
    }

    private void addProjectionReading(Document document, SimulationResponse simulation) {
        var finalPoint = simulation.projection().getLast();
        addSectionTitle(document, "Lecture du scénario");

        var summary = new Paragraph(
                "Au terme du financement, le patrimoine net projeté atteint " + currency(finalPoint.netWorth())
                        + ". Le scénario intègre la vacance, les charges récurrentes, la fiscalité sélectionnée et la valorisation saisie.",
                font(9, Font.NORMAL, INK));
        summary.setLeading(14);
        summary.setSpacingAfter(14);
        document.add(summary);

        var checks = new PdfPTable(1);
        checks.setWidthPercentage(100);
        checks.addCell(noteCell("1", "Valider le loyer cible avec des références locatives comparables."));
        checks.addCell(noteCell("2", "Faire chiffrer les travaux et conserver une marge pour les imprévus."));
        checks.addCell(noteCell("3", "Confirmer le régime fiscal avec un professionnel avant l'acquisition."));
        checks.setSpacingAfter(24);
        document.add(checks);
    }

    private PdfPCell noteCell(String index, String text) {
        var cell = new PdfPCell();
        cell.setPadding(10);
        cell.setBorderColor(new Color(222, 231, 226));
        cell.addElement(new Paragraph(index + "  " + text, font(8, Font.NORMAL, INK)));
        return cell;
    }

    private PdfPCell highlightCell(String label, String value) {
        var cell = new PdfPCell();
        cell.setPadding(14);
        cell.setBorderColor(PALE);
        cell.setBackgroundColor(PALE);
        cell.addElement(new Paragraph(label.toUpperCase(Locale.FRENCH), font(7, Font.BOLD, MUTED)));
        cell.addElement(new Paragraph(value, font(19, Font.BOLD, INK)));
        return cell;
    }

    private PdfPCell metricCell(String label, String value) {
        var cell = new PdfPCell();
        cell.setPadding(11);
        cell.setBorderColor(new Color(222, 231, 226));
        cell.addElement(new Paragraph(label, font(7, Font.BOLD, MUTED)));
        cell.addElement(new Paragraph(value, font(11, Font.BOLD, INK)));
        return cell;
    }

    private void addRow(PdfPTable table, String label, String value) {
        table.addCell(bodyCell(label, Element.ALIGN_LEFT));
        table.addCell(bodyCell(value, Element.ALIGN_RIGHT));
    }

    private PdfPCell headerCell(String text) {
        var cell = new PdfPCell(new Phrase(text, font(7, Font.BOLD, INK)));
        cell.setPadding(8);
        cell.setBackgroundColor(LIME);
        cell.setBorderColor(LIME);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        return cell;
    }

    private PdfPCell bodyCell(String text, int alignment) {
        var cell = new PdfPCell(new Phrase(text, font(7, Font.NORMAL, INK)));
        cell.setPadding(8);
        cell.setBorderColor(new Color(222, 231, 226));
        cell.setHorizontalAlignment(alignment);
        return cell;
    }

    private static Font font(float size, int style, Color color) {
        return FontFactory.getFont(FontFactory.HELVETICA, size, style, color);
    }

    private static String currency(BigDecimal value) {
        return NumberFormat.getCurrencyInstance(Locale.FRANCE).format(value);
    }

    private static String signedCurrency(BigDecimal value) {
        return (value.signum() >= 0 ? "+" : "−") + currency(value.abs());
    }
}
