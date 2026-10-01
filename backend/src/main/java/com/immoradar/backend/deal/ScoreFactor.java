package com.immoradar.backend.deal;

/** Contribution d'un critère au Radar score, avec l'explication affichée à l'utilisateur. */
public record ScoreFactor(String key, String label, double points, double maxPoints, String detail) {}
