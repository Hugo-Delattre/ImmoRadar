package com.immomanager.backend.deal;

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
}
