package com.lavagest.model;

public enum VehicleType {
    CARRO("Carro"),
    MOTO("Moto");

    private final String label;

    VehicleType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static VehicleType fromLabel(String value) {
        for (VehicleType type : values()) {
            if (type.label.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Tipo de vehiculo no soportado: " + value);
    }
}
