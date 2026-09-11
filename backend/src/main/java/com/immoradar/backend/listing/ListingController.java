package com.immoradar.backend.listing;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/listings")
public class ListingController {

    private final ListingExtractorService extractorService;

    public ListingController(ListingExtractorService extractorService) {
        this.extractorService = extractorService;
    }

    @PostMapping("/extract")
    public ResponseEntity<ListingExtractDto> extractListing(@Valid @RequestBody ListingExtractRequest request) {
        var result = extractorService.extract(request.url());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/extract")
    public ResponseEntity<ListingExtractDto> extractListingByQuery(@RequestParam String url) {
        var result = extractorService.extract(url);
        return ResponseEntity.ok(result);
    }
}
