package com.lavagest.dto;

import java.math.BigDecimal;

/**
 * DTO de SALIDA para el rendimiento individual de un trabajador.
 * Alimenta la tabla de "Trabajadores" y las tarjetas de rendimiento del Dashboard.
 */
public class WorkerKpiDTO {
    private Long workerId;
    private String workerName;
    private String bay;
    private String shift;
    private boolean active;

    // Métricas de producción
    private int totalWashes;          // Total de lavados realizados (en el periodo)
    private int carsWashed;           // Solo carros
    private int motosWashed;          // Solo motos

    // Métricas financieras
    private BigDecimal totalGenerated;   // Total cobrado por sus lavados (ingresos que generó)
    private BigDecimal totalCommission;  // Comisión acumulada histórica (lo que le toca al trabajador)
    private BigDecimal businessProfit;   // Ganancia neta que aportó al negocio
    private BigDecimal avgTicket;        // Ticket promedio de sus lavados

    // Nómina / pendiente de pago
    private BigDecimal pendingToPay;     // Comisiones acumuladas aún no liquidadas
    private int pendingWashes;           // Lavados con payout en PENDIENTE

    public WorkerKpiDTO() {
    }

    // ---------- Getters y Setters ----------

    public Long getWorkerId() { return workerId; }
    public void setWorkerId(Long workerId) { this.workerId = workerId; }

    public String getWorkerName() { return workerName; }
    public void setWorkerName(String workerName) { this.workerName = workerName; }

    public String getBay() { return bay; }
    public void setBay(String bay) { this.bay = bay; }

    public String getShift() { return shift; }
    public void setShift(String shift) { this.shift = shift; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public int getTotalWashes() { return totalWashes; }
    public void setTotalWashes(int totalWashes) { this.totalWashes = totalWashes; }

    public int getCarsWashed() { return carsWashed; }
    public void setCarsWashed(int carsWashed) { this.carsWashed = carsWashed; }

    public int getMotosWashed() { return motosWashed; }
    public void setMotosWashed(int motosWashed) { this.motosWashed = motosWashed; }

    public BigDecimal getTotalGenerated() { return totalGenerated; }
    public void setTotalGenerated(BigDecimal totalGenerated) { this.totalGenerated = totalGenerated; }

    public BigDecimal getTotalCommission() { return totalCommission; }
    public void setTotalCommission(BigDecimal totalCommission) { this.totalCommission = totalCommission; }

    public BigDecimal getBusinessProfit() { return businessProfit; }
    public void setBusinessProfit(BigDecimal businessProfit) { this.businessProfit = businessProfit; }

    public BigDecimal getAvgTicket() { return avgTicket; }
    public void setAvgTicket(BigDecimal avgTicket) { this.avgTicket = avgTicket; }

    public BigDecimal getPendingToPay() { return pendingToPay; }
    public void setPendingToPay(BigDecimal pendingToPay) { this.pendingToPay = pendingToPay; }

    public int getPendingWashes() { return pendingWashes; }
    public void setPendingWashes(int pendingWashes) { this.pendingWashes = pendingWashes; }
}
