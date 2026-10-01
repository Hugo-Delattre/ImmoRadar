package com.immoradar.backend.deal;

/**
 * Classe énergie du DPE. La loi Climat et résilience interdit de louer un logement classé G depuis 2025,
 * F à partir de 2028 et E à partir de 2034 ; les loyers des logements F et G sont gelés depuis août 2022.
 */
public enum EnergyClass {
    A, B, C, D, E, F, G;

    /** Année à partir de laquelle un nouveau bail est interdit, ou {@code 0} si aucune échéance. */
    public int rentalBanYear() {
        return switch (this) {
            case G -> 2025;
            case F -> 2028;
            case E -> 2034;
            default -> 0;
        };
    }
}
