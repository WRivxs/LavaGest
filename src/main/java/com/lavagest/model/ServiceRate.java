package com.lavagest.model;

import java.math.BigDecimal;

/**
 * Representa una tarifa o catálogo de servicio configurable (Carro o Moto).
 * Corresponde a la tabla service_rates de la base de datos.
 */
public class ServiceRate {
    private Long id;
    private VehicleType vehicleType;
    private String serviceName;          // Ej: "Lavado General + Aspirado"
    private BigDecimal defaultPrice;     // Ej: $30.000 COP
    private BigDecimal commissionPct;    // Ej: 40.00 %
    private boolean active;

    public ServiceRate() {
    }

    public ServiceRate(Long id, VehicleType vehicleType, String serviceName,
                       BigDecimal defaultPrice, BigDecimal commissionPct, boolean active) {
        this.id = id;
        this.vehicleType = vehicleType;
        this.serviceName = serviceName;
        this.defaultPrice = defaultPrice;
        this.commissionPct = commissionPct != null ? commissionPct : new BigDecimal("40.00");
        this.active = active;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public VehicleType getVehicleType() { return vehicleType; }
    public void setVehicleType(VehicleType vehicleType) { this.vehicleType = vehicleType; }

    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }

    public BigDecimal getDefaultPrice() { return defaultPrice; }
    public void setDefaultPrice(BigDecimal defaultPrice) { this.defaultPrice = defaultPrice; }

    public BigDecimal getCommissionPct() { return commissionPct; }
    public void setCommissionPct(BigDecimal commissionPct) { this.commissionPct = commissionPct; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
