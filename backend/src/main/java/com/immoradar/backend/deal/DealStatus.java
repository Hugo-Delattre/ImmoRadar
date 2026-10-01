package com.immoradar.backend.deal;

/** Étape du bien dans le pipeline d'acquisition. */
public enum DealStatus {
    TO_REVIEW,
    TO_VISIT,
    OFFER_MADE,
    ACQUIRED,
    REJECTED
}
