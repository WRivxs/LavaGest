package com.lavagest.dto;

import com.lavagest.model.VehicleType;
import java.math.BigDecimal;

/**
 * DTO de ENTRADA para registrar un nuevo lavado desde la UI.
 * La UI llena estos campos y se los pasa al WashController.
 */
public class CreateWashRequest {
    private String plate;           // Placa del vehículo (obligatorio)
    private String vehicleModel;    // Modelo + color (opcional, ej: "Mazda 3 Blanco")
    private VehicleType vehicleType; // CARRO o MOTO (obligatorio)
    private Long workerId;          // ID del trabajador asignado (obligatorio)
    private BigDecimal price;       // Precio cobrado al cliente (obligatorio)
    private BigDecimal commissionPct; // % comisión (default 40.00 si viene null)
    private BigDecimal workerPayment; // Si viene null, se calcula automáticamente
    private String notes;

    public CreateWashRequest() {
    }

    public CreateWashRequest(String plate, String vehicleModel, VehicleType vehicleType,
                             Long workerId, BigDecimal price) {
        this.plate = plate;
        this.vehicleModel = vehicleModel;
        this.vehicleType = vehicleType;
        this.workerId = workerId;
        this.price = price;
        this.commissionPct = new BigDecimal("40.00"); // Comisión estándar del 40%
    }

    // ---------- Getters y Setters ----------

    public String getPlate() { return plate; }
    public void setPlate(String plate) { this.plate = plate; }

    public String getVehicleModel() { return vehicleModel; }
    public void setVehicleModel(String vehicleModel) { this.vehicleModel = vehicleModel; }

    public VehicleType getVehicleType() { return vehicleType; }
    public void setVehicleType(VehicleType vehicleType) { this.vehicleType = vehicleType; }

    public Long getWorkerId() { return workerId; }
    public void setWorkerId(Long workerId) { this.workerId = workerId; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public BigDecimal getCommissionPct() { return commissionPct; }
    public void setCommissionPct(BigDecimal commissionPct) { this.commissionPct = commissionPct; }

    public BigDecimal getWorkerPayment() { return workerPayment; }
    public void setWorkerPayment(BigDecimal workerPayment) { this.workerPayment = workerPayment; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
