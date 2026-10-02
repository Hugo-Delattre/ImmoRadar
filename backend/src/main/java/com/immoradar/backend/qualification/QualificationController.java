package com.immoradar.backend.qualification;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/deals/{dealId}")
public class QualificationController {
    private final RentalReferenceService references;
    private final QualificationService qualifications;

    public QualificationController(RentalReferenceService references, QualificationService qualifications) {
        this.references = references;
        this.qualifications = qualifications;
    }

    @GetMapping("/rental-references")
    public List<RentalReference.View> list(@PathVariable String dealId) { return references.list(dealId); }

    @PostMapping("/rental-references")
    public List<RentalReference.View> add(@PathVariable String dealId, @Valid @RequestBody RentalReferenceRequest request) {
        return references.add(dealId, request);
    }

    @DeleteMapping("/rental-references/{referenceId}")
    public List<RentalReference.View> remove(@PathVariable String dealId, @PathVariable String referenceId) {
        return references.remove(dealId, referenceId);
    }

    @PostMapping("/qualification")
    public QualificationResponse assess(@PathVariable String dealId, @Valid @RequestBody QualificationRequest request) {
        if (!dealId.equals(request.base().dealId())) throw new IllegalArgumentException("Le bien et la simulation doivent correspondre.");
        return qualifications.assess(request);
    }
}
