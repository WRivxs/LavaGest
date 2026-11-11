package com.lavagest.model;

import java.math.BigDecimal;

public class VehicleTypeCatalog {
    private Long id;
    private String name;
    private boolean active;
    private BigDecimal commissionPct;

    public VehicleTypeCatalog() {}

    public VehicleTypeCatalog(Long id, String name, boolean active) {
        this.id = id;
        this.name = name;
        this.active = active;
        this.commissionPct = new BigDecimal("40.00");
    }

    public VehicleTypeCatalog(Long id, String name, boolean active, BigDecimal commissionPct) {
        this.id = id;
        this.name = name;
        this.active = active;
        this.commissionPct = commissionPct;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public BigDecimal getCommissionPct() { return commissionPct; }
    public void setCommissionPct(BigDecimal commissionPct) { this.commissionPct = commissionPct; }

    @Override
    public String toString() { return name; }
}
