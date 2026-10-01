package com.immoradar.backend.market;

/** Commune (ou arrondissement municipal) identifiée par son code INSEE. */
public record Commune(String inseeCode, String name, String departmentCode) {}
