package com.lavagest.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO de SALIDA que alimenta todos los KPIs, tarjetas y métricas del Dashboard.
 * Lo produce el DashboardController y lo consume la vista MainFrame.
 */
public class DashboardMetricsDTO {

    // ------ KPIs principales (filtrados por periodo) ------
    private int totalWashes;             // Total de lavados en el periodo
    private int carsCount;               // Lavados de Carros
    private int motosCount;              // Lavados de Motos
    private BigDecimal carsPercentage;   // % de carros sobre el total
    private BigDecimal motosPercentage;  // % de motos sobre el total

    // Finanzas del periodo
    private BigDecimal totalIncome;          // Ingresos brutos
    private BigDecimal totalWorkerPayments;  // Comisiones de trabajadores (egresos)
    private BigDecimal totalNetProfit;       // Ganancia neta del negocio
    private BigDecimal avgTicketPrice;       // Ticket promedio
    private BigDecimal avgProfit;            // Ganancia promedio por lavado
    private BigDecimal profitMarginPct;      // Margen % de utilidad

    // ------ Tendencia semanal (para la gráfica de barras) ------
    private List<DayTrendDTO> weeklyTrend;

    // ------ Nómina / Liquidacion del dia ------
    private BigDecimal totalPayoutsDisbursed; // Dinero entregado en caja hoy

    // ------ Trabajador líder del periodo ------
    private String topWorkerName;
    private int topWorkerWashes;

    public DashboardMetricsDTO() {
    }

    // ---------- Getters y Setters ----------

    public int getTotalWashes() { return totalWashes; }
    public void setTotalWashes(int totalWashes) { this.totalWashes = totalWashes; }

    public int getCarsCount() { return carsCount; }
    public void setCarsCount(int carsCount) { this.carsCount = carsCount; }

    public int getMotosCount() { return motosCount; }
    public void setMotosCount(int motosCount) { this.motosCount = motosCount; }

    public BigDecimal getCarsPercentage() { return carsPercentage; }
    public void setCarsPercentage(BigDecimal carsPercentage) { this.carsPercentage = carsPercentage; }

    public BigDecimal getMotosPercentage() { return motosPercentage; }
    public void setMotosPercentage(BigDecimal motosPercentage) { this.motosPercentage = motosPercentage; }

    public BigDecimal getTotalIncome() { return totalIncome; }
    public void setTotalIncome(BigDecimal totalIncome) { this.totalIncome = totalIncome; }

    public BigDecimal getTotalWorkerPayments() { return totalWorkerPayments; }
    public void setTotalWorkerPayments(BigDecimal totalWorkerPayments) { this.totalWorkerPayments = totalWorkerPayments; }

    public BigDecimal getTotalNetProfit() { return totalNetProfit; }
    public void setTotalNetProfit(BigDecimal totalNetProfit) { this.totalNetProfit = totalNetProfit; }

    public BigDecimal getAvgTicketPrice() { return avgTicketPrice; }
    public void setAvgTicketPrice(BigDecimal avgTicketPrice) { this.avgTicketPrice = avgTicketPrice; }

    public BigDecimal getAvgProfit() { return avgProfit; }
    public void setAvgProfit(BigDecimal avgProfit) { this.avgProfit = avgProfit; }

    public BigDecimal getProfitMarginPct() { return profitMarginPct; }
    public void setProfitMarginPct(BigDecimal profitMarginPct) { this.profitMarginPct = profitMarginPct; }

    public List<DayTrendDTO> getWeeklyTrend() { return weeklyTrend; }
    public void setWeeklyTrend(List<DayTrendDTO> weeklyTrend) { this.weeklyTrend = weeklyTrend; }

    public BigDecimal getTotalPayoutsDisbursed() { return totalPayoutsDisbursed; }
    public void setTotalPayoutsDisbursed(BigDecimal totalPayoutsDisbursed) { this.totalPayoutsDisbursed = totalPayoutsDisbursed; }

    public String getTopWorkerName() { return topWorkerName; }
    public void setTopWorkerName(String topWorkerName) { this.topWorkerName = topWorkerName; }

    public int getTopWorkerWashes() { return topWorkerWashes; }
    public void setTopWorkerWashes(int topWorkerWashes) { this.topWorkerWashes = topWorkerWashes; }

    // ------ DTO anidado para cada barra del gráfico semanal ------
    public static class DayTrendDTO {
        private String dayName;        // "Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom"
        private int washesCount;
        private BigDecimal totalIncome;
        private BigDecimal totalProfit;

        public DayTrendDTO() {}

        public DayTrendDTO(String dayName, int washesCount, BigDecimal totalIncome, BigDecimal totalProfit) {
            this.dayName = dayName;
            this.washesCount = washesCount;
            this.totalIncome = totalIncome;
            this.totalProfit = totalProfit;
        }

        public String getDayName() { return dayName; }
        public void setDayName(String dayName) { this.dayName = dayName; }

        public int getWashesCount() { return washesCount; }
        public void setWashesCount(int washesCount) { this.washesCount = washesCount; }

        public BigDecimal getTotalIncome() { return totalIncome; }
        public void setTotalIncome(BigDecimal totalIncome) { this.totalIncome = totalIncome; }

        public BigDecimal getTotalProfit() { return totalProfit; }
        public void setTotalProfit(BigDecimal totalProfit) { this.totalProfit = totalProfit; }
    }
}
