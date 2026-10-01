package com.immoradar.backend.deal;

import com.fasterxml.jackson.annotation.JsonValue;

public enum PropertyType {
    STUDIO("Studio"),
    APARTMENT("Apartment"),
    BUILDING("Building"),
    HOUSE("House");

    private final String value;

    PropertyType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    /** Accepte la valeur JSON ("Apartment") comme le nom de l'énumération ("APARTMENT"). */
    public static PropertyType fromValue(String raw) {
        for (var type : values()) {
            if (type.value.equalsIgnoreCase(raw) || type.name().equalsIgnoreCase(raw)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Type de bien inconnu : " + raw);
    }
}
