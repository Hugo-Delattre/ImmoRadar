package com.immoradar.backend.deal;

public class DealNotFoundException extends RuntimeException {
    public DealNotFoundException(String dealId) {
        super("Aucun bien ne correspond à l'identifiant " + dealId + ".");
    }
}
