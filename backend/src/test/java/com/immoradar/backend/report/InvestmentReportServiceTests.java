package com.immoradar.backend.report;

import com.immoradar.backend.deal.Deal;
import com.immoradar.backend.deal.DealService;
import com.immoradar.backend.deal.PropertyType;
import com.immoradar.backend.simulation.FinancialSimulationService;
import com.immoradar.backend.simulation.ProjectionPoint;
import com.immoradar.backend.simulation.SimulationRequest;
import com.immoradar.backend.simulation.SimulationResponse;
import com.immoradar.backend.simulation.TaxRegime;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InvestmentReportServiceTests {

    @Test
    void generatesAValidPdfInvestmentDossier() {
        var dealService = mock(DealService.class);
        var simulationService = mock(FinancialSimulationService.class);
        var reportService = new InvestmentReportService(dealService, simulationService);
        var request = new SimulationRequest(
                "deal-1",
                new BigDecimal("30000"),
                new BigDecimal("3.5"),
                20,
                TaxRegime.REEL_LMNP,
                new BigDecimal("30"),
                new BigDecimal("4"),
                BigDecimal.ZERO,
                new BigDecimal("180"),
                new BigDecimal("1.5"),
                new BigDecimal("1.2")
        );
        var deal = new Deal(
                "deal-1", "T2 lumineux - Lyon 7e", new BigDecimal("210000"),
                new BigDecimal("1050"), new BigDecimal("110"), new BigDecimal("850"),
                new BigDecimal("12000"), "Lyon 7e", new BigDecimal("43"), PropertyType.APARTMENT,
                "À proximité des transports", 91, "https://example.com/deal.jpg", true
        );
        var projection = List.of(
                new ProjectionPoint(1, new BigDecimal("1610"), new BigDecimal("1610"),
                        new BigDecimal("180000"), new BigDecimal("224000"), new BigDecimal("45610")),
                new ProjectionPoint(20, new BigDecimal("2200"), new BigDecimal("38000"),
                        BigDecimal.ZERO, new BigDecimal("278000"), new BigDecimal("316000"))
        );
        var simulation = new SimulationResponse(
                new BigDecimal("237750"), new BigDecimal("207750"), new BigDecimal("1204.15"),
                new BigDecimal("134.22"), new BigDecimal("6.00"), new BigDecimal("4.83"),
                new BigDecimal("380"), new BigDecimal("3510"), new BigDecimal("915"),
                "POSITIVE", projection
        );

        when(dealService.getEntity("deal-1")).thenReturn(deal);
        when(simulationService.simulate(request)).thenReturn(simulation);

        byte[] report = reportService.generate(request);

        assertThat(report).hasSizeGreaterThan(2_000);
        assertThat(new String(report, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
    }
}
