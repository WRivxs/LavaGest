package com.lavagest.model;

/**
 * Estado de liquidacion de la comision de un lavado.
 * PENDIENTE  → la comision aun no ha sido entregada al trabajador.
 * LIQUIDADO  → ya fue incluida en un pago registrado en worker_payouts.
 */
public enum PayoutStatus {
    PENDIENTE("Pendiente"),
    LIQUIDADO("Liquidado");

    private final String label;

    PayoutStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static PayoutStatus fromLabel(String value) {
        for (PayoutStatus s : values()) {
            if (s.label.equalsIgnoreCase(value)) {
                return s;
            }
        }
        throw new IllegalArgumentException("Estado de pago no reconocido: " + value);
    }
}
