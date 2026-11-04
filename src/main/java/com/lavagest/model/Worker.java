package com.lavagest.model;

public class Worker {
    private Long id;
    private String name;
    private String phone;
    private String bay;    // Bahia asignada (ej: Bahia 1, Bahia 2)
    private String shift;  // Turno (ej: Turno Completo, Medio Turno)
    private boolean active;

    public Worker() {
    }

    public Worker(Long id, String name, String phone, String bay, String shift, boolean active) {
        this.id = id;
        this.name = name;
        this.phone = phone;
        this.bay = bay;
        this.shift = shift;
        this.active = active;
    }

    // Mantiene compatibilidad con el constructor anterior
    public Worker(Long id, String name, String phone, boolean active) {
        this(id, name, phone, "Bahia 1", "Turno Completo", active);
    }

    // ---------- Getters y Setters ----------

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getBay() { return bay; }
    public void setBay(String bay) { this.bay = bay; }

    public String getShift() { return shift; }
    public void setShift(String shift) { this.shift = shift; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
