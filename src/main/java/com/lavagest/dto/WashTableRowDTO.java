package com.lavagest.dto;

import java.math.BigDecimal;

/**
 * DTO de SALIDA para una fila del historial de lavados.
 * Contiene los datos combinados de Wash + Worker listos para mostrar en la tabla.
 */
public class WashTableRowDTO {
    private Long id;
    private String washDate;         // Formateado: "30/10"
    private String washTime;         // Formateado: "14:42"
    private String plate;
    private String vehicleModel;
    private String vehicleTypeLabel; // "Carro" o "Moto"
    private String workerName;
    private String workerBay;
    private String payoutStatus;     // "Pendiente" o "Liquidado"
    private BigDecimal price;
    private BigDecimal workerPayment;
    private BigDecimal profit;
    private BigDecimal profitMarginPct;

    public WashTableRowDTO() {
    }

    // ---------- Getters y Setters ----------

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getWashDate() { return washDate; }
    public void setWashDate(String washDate) { this.washDate = washDate; }

    public String getWashTime() { return washTime; }
    public void setWashTime(String washTime) { this.washTime = washTime; }

    public String getPlate() { return plate; }
    public void setPlate(String plate) { this.plate = plate; }

    public String getVehicleModel() { return vehicleModel; }
    public void setVehicleModel(String vehicleModel) { this.vehicleModel = vehicleModel; }

    public String getVehicleTypeLabel() { return vehicleTypeLabel; }
    public void setVehicleTypeLabel(String vehicleTypeLabel) { this.vehicleTypeLabel = vehicleTypeLabel; }

    public String getWorkerName() { return workerName; }
    public void setWorkerName(String workerName) { this.workerName = workerName; }

    public String getWorkerBay() { return workerBay; }
    public void setWorkerBay(String workerBay) { this.workerBay = workerBay; }

    public String getPayoutStatus() { return payoutStatus; }
    public void setPayoutStatus(String payoutStatus) { this.payoutStatus = payoutStatus; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public BigDecimal getWorkerPayment() { return workerPayment; }
    public void setWorkerPayment(BigDecimal workerPayment) { this.workerPayment = workerPayment; }

    public BigDecimal getProfit() { return profit; }
    public void setProfit(BigDecimal profit) { this.profit = profit; }

    public BigDecimal getProfitMarginPct() { return profitMarginPct; }
    public void setProfitMarginPct(BigDecimal profitMarginPct) { this.profitMarginPct = profitMarginPct; }
}
