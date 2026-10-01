package com.immoradar.backend.evidence;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/deals/{dealId}/evidence")
public class DealEvidenceController {
    private final DealEvidenceService service;

    public DealEvidenceController(DealEvidenceService service) { this.service = service; }

    @GetMapping
    public EvidenceSummary get(@PathVariable String dealId) { return service.getSummary(dealId); }

    @PutMapping("/{field}")
    public EvidenceSummary update(@PathVariable String dealId, @PathVariable EvidenceField field,
                                  @Valid @RequestBody UpdateEvidenceRequest request) {
        return service.update(dealId, field, request);
    }
}
