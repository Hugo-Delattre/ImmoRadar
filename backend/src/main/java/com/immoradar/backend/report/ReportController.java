package com.immoradar.backend.report;

import com.immoradar.backend.simulation.SimulationRequest;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final InvestmentReportService reportService;

    public ReportController(InvestmentReportService reportService) {
        this.reportService = reportService;
    }

    @PostMapping(value = "/investment", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> generate(@RequestBody @Valid SimulationRequest request) {
        var disposition = ContentDisposition.attachment()
                .filename("dossier-investissement-immoradar.pdf", StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.APPLICATION_PDF)
                .body(reportService.generate(request));
    }
}
