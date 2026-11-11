package com.lavagest.dto;

import com.lavagest.model.VehicleType;
import java.math.BigDecimal;

/**
 * DTO de ENTRADA para registrar un nuevo lavado desde la UI.
 * La UI llena estos campos y se los pasa al WashController.
 */
public class CreateWashRequest {
    private String plate;                   // Placa del vehículo
    private String vehicleModel;            // Modelo + color (opcional)
    private VehicleType vehicleType;        // Enum legacy CARRO/MOTO (compatibilidad)
    private Long vehicleTypeCatalogId;      // FK a vehicle_types_catalog (nuevo)
    private String vehicleTypeName;         // nombre del tipo de vehículo (nuevo)
    private Long washTypeId;                // FK a wash_types (nuevo)
    private String washTypeName;            // nombre del tipo de lavado (nuevo)
    private String clientName;              // nombre del cliente (nuevo)
    private String clientPhone;             // teléfono del cliente (opcional)
    private Long workerId;                  // ID del trabajador asignado
    private BigDecimal price;               // Precio cobrado
    private BigDecimal commissionPct;       // % comisión (default 40.00)
    private BigDecimal workerPayment;       // Si null, se calcula automáticamente
    private Long createdByUserId;           // FK a app_users (nuevo)
    private String notes;

    public CreateWashRequest() {}

    public CreateWashRequest(String plate, String vehicleModel, VehicleType vehicleType,
                             Long workerId, BigDecimal price) {
        this.plate = plate;
        this.vehicleModel = vehicleModel;
        this.vehicleType = vehicleType;
        this.workerId = workerId;
        this.price = price;
        this.commissionPct = new BigDecimal("40.00");
    }

    // ---------- Getters y Setters ----------

    public String getPlate() { return plate; }
    public void setPlate(String plate) { this.plate = plate; }

    public String getVehicleModel() { return vehicleModel; }
    public void setVehicleModel(String vehicleModel) { this.vehicleModel = vehicleModel; }

    public VehicleType getVehicleType() { return vehicleType; }
    public void setVehicleType(VehicleType vehicleType) { this.vehicleType = vehicleType; }

    public Long getVehicleTypeCatalogId() { return vehicleTypeCatalogId; }
    public void setVehicleTypeCatalogId(Long vehicleTypeCatalogId) { this.vehicleTypeCatalogId = vehicleTypeCatalogId; }

    public String getVehicleTypeName() { return vehicleTypeName; }
    public void setVehicleTypeName(String vehicleTypeName) { this.vehicleTypeName = vehicleTypeName; }

    public Long getWashTypeId() { return washTypeId; }
    public void setWashTypeId(Long washTypeId) { this.washTypeId = washTypeId; }

    public String getWashTypeName() { return washTypeName; }
    public void setWashTypeName(String washTypeName) { this.washTypeName = washTypeName; }

    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }

    public String getClientPhone() { return clientPhone; }
    public void setClientPhone(String clientPhone) { this.clientPhone = clientPhone; }

    public Long getWorkerId() { return workerId; }
    public void setWorkerId(Long workerId) { this.workerId = workerId; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public BigDecimal getCommissionPct() { return commissionPct; }
    public void setCommissionPct(BigDecimal commissionPct) { this.commissionPct = commissionPct; }

    public BigDecimal getWorkerPayment() { return workerPayment; }
    public void setWorkerPayment(BigDecimal workerPayment) { this.workerPayment = workerPayment; }

    public Long getCreatedByUserId() { return createdByUserId; }
    public void setCreatedByUserId(Long createdByUserId) { this.createdByUserId = createdByUserId; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
