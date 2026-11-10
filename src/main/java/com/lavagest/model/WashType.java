package com.lavagest.model;

import java.math.BigDecimal;

public class WashType {
    private Long id;
    private String name;
    private BigDecimal price;
    private Long vehicleTypeCatalogId;
    private String vehicleTypeName;
    private boolean active;

    public WashType() {}

    public WashType(Long id, String name, BigDecimal price, Long vehicleTypeCatalogId,
                    String vehicleTypeName, boolean active) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.vehicleTypeCatalogId = vehicleTypeCatalogId;
        this.vehicleTypeName = vehicleTypeName;
        this.active = active;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public Long getVehicleTypeCatalogId() { return vehicleTypeCatalogId; }
    public void setVehicleTypeCatalogId(Long vehicleTypeCatalogId) {
        this.vehicleTypeCatalogId = vehicleTypeCatalogId;
    }

    public String getVehicleTypeName() { return vehicleTypeName; }
    public void setVehicleTypeName(String vehicleTypeName) {
        this.vehicleTypeName = vehicleTypeName;
    }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    @Override
    public String toString() {
        return name + " ($" + price + ")";
    }
}
