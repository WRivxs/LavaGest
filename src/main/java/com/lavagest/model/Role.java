package com.lavagest.model;

public enum Role {
    ADMIN("admin"),
    ENCARGADO("encargado");

    private final String label;

    Role(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static Role fromLabel(String label) {
        for (Role r : values()) {
            if (r.label.equalsIgnoreCase(label)) return r;
        }
        throw new IllegalArgumentException("Rol desconocido: " + label);
    }
}
