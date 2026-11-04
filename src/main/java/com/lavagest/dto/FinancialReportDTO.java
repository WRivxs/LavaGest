package com.lavagest.dto;

import java.math.BigDecimal;

/**
 * DTO de SALIDA para el módulo de Reportes Financieros.
 * Agrupa los datos de ingresos para un período (día, mes o rango personalizado).
 * Cada instancia representa un período (una fila en la tabla de reportes).
 */
public class FinancialReportDTO {
    private String period;          // Ej: "2026-10-30" (diario) o "2026-10" (mensual)
    private String periodLabel;     // Ej: "Jueves 30 Oct" o "Octubre 2026"

    private int totalWashes;
    private int carsCount;
    private int motosCount;

    // Ingresos por tipo
    private BigDecimal incomeCars;
    private BigDecimal incomeMotos;

    // Consolidado financiero
    private BigDecimal grossIncome;       // Ingresos Brutos Totales
    private BigDecimal totalCommissions;  // Comisiones de trabajadores causadas
    private BigDecimal netProfit;         // Ganancia Neta del Negocio

    // Caja real
    private BigDecimal cashPayoutsDisbursed; // Dinero efectivamente desembolsado ese día

    // Promedios
    private BigDecimal avgTicketPrice;
    private BigDecimal profitMarginPct;

    // Solo para reporte mensual
    private int operatingDays;
    private BigDecimal avgDailyIncome;

    public FinancialReportDTO() {
    }

    // ---------- Getters y Setters ----------

    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }

    public String getPeriodLabel() { return periodLabel; }
    public void setPeriodLabel(String periodLabel) { this.periodLabel = periodLabel; }

    public int getTotalWashes() { return totalWashes; }
    public void setTotalWashes(int totalWashes) { this.totalWashes = totalWashes; }

    public int getCarsCount() { return carsCount; }
    public void setCarsCount(int carsCount) { this.carsCount = carsCount; }

    public int getMotosCount() { return motosCount; }
    public void setMotosCount(int motosCount) { this.motosCount = motosCount; }

    public BigDecimal getIncomeCars() { return incomeCars; }
    public void setIncomeCars(BigDecimal incomeCars) { this.incomeCars = incomeCars; }

    public BigDecimal getIncomeMotos() { return incomeMotos; }
    public void setIncomeMotos(BigDecimal incomeMotos) { this.incomeMotos = incomeMotos; }

    public BigDecimal getGrossIncome() { return grossIncome; }
    public void setGrossIncome(BigDecimal grossIncome) { this.grossIncome = grossIncome; }

    public BigDecimal getTotalCommissions() { return totalCommissions; }
    public void setTotalCommissions(BigDecimal totalCommissions) { this.totalCommissions = totalCommissions; }

    public BigDecimal getNetProfit() { return netProfit; }
    public void setNetProfit(BigDecimal netProfit) { this.netProfit = netProfit; }

    public BigDecimal getCashPayoutsDisbursed() { return cashPayoutsDisbursed; }
    public void setCashPayoutsDisbursed(BigDecimal cashPayoutsDisbursed) { this.cashPayoutsDisbursed = cashPayoutsDisbursed; }

    public BigDecimal getAvgTicketPrice() { return avgTicketPrice; }
    public void setAvgTicketPrice(BigDecimal avgTicketPrice) { this.avgTicketPrice = avgTicketPrice; }

    public BigDecimal getProfitMarginPct() { return profitMarginPct; }
    public void setProfitMarginPct(BigDecimal profitMarginPct) { this.profitMarginPct = profitMarginPct; }

    public int getOperatingDays() { return operatingDays; }
    public void setOperatingDays(int operatingDays) { this.operatingDays = operatingDays; }

    public BigDecimal getAvgDailyIncome() { return avgDailyIncome; }
    public void setAvgDailyIncome(BigDecimal avgDailyIncome) { this.avgDailyIncome = avgDailyIncome; }
}
