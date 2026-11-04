package com.lavagest.controller;

import com.lavagest.dto.FinancialReportDTO;
import com.lavagest.service.ReportService;

import java.time.LocalDate;
import java.util.List;

/**
 * Controlador de Reportes Financieros.
 * Provee a la UI ("Reportes Financieros") información consolidada de ingresos,
 * comisiones, margen neto y desglose de vehículos por periodos.
 */
public class ReportController {
    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    /**
     * Reporte financiero diario dentro de un rango de fechas.
     */
    public List<FinancialReportDTO> getDailyReport(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("Las fechas de inicio y fin son obligatorias.");
        }
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("La fecha de inicio no puede ser posterior a la de fin.");
        }
        return reportService.getDailyReport(from, to);
    }

    /**
     * Reporte diario de los últimos 30 días.
     */
    public List<FinancialReportDTO> getLast30DaysDailyReport() {
        LocalDate today = LocalDate.now();
        return getDailyReport(today.minusDays(29), today);
    }

    /**
     * Reporte financiero mensual agrupado por mes (ej. para el año actual).
     */
    public List<FinancialReportDTO> getMonthlyReport(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("Las fechas de inicio y fin son obligatorias.");
        }
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("La fecha de inicio no puede ser posterior a la de fin.");
        }
        return reportService.getMonthlyReport(from, to);
    }

    /**
     * Reporte mensual de los últimos 12 meses.
     */
    public List<FinancialReportDTO> getLast12MonthsReport() {
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusMonths(11).withDayOfMonth(1);
        return getMonthlyReport(start, today);
    }

    /**
     * Resumen consolidado acumulado para un rango de fechas.
     */
    public FinancialReportDTO getSummaryReport(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("Las fechas son obligatorias.");
        }
        return reportService.getSummaryReport(from, to);
    }
}
