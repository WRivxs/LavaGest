package com.lavagest.controller;

import com.lavagest.dto.DashboardMetricsDTO;
import com.lavagest.service.DashboardService;

import java.time.DayOfWeek;
import java.time.LocalDate;

/**
 * Controlador del Dashboard principal.
 * Recibe la selección de periodo de la UI y devuelve el DTO completo de métricas.
 * Es el punto de entrada para todos los KPIs de la pantalla principal.
 */
public class DashboardController {
    /** Periodos predefinidos que maneja la barra de filtros de la UI. */
    public enum Period { HOY, AYER, ESTA_SEMANA, ESTE_MES, PERSONALIZADO }

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /**
     * Devuelve los KPIs del Dashboard para el periodo predefinido seleccionado.
     * La UI llama a este metodo cuando el usuario hace clic en Hoy / Ayer / Semana / Mes.
     */
    public DashboardMetricsDTO getMetrics(Period period) {
        LocalDate today = LocalDate.now();
        switch (period) {
            case HOY:
                return dashboardService.buildDashboardMetrics(today, today);
            case AYER:
                LocalDate yesterday = today.minusDays(1);
                return dashboardService.buildDashboardMetrics(yesterday, yesterday);
            case ESTA_SEMANA:
                LocalDate monday = today.with(DayOfWeek.MONDAY);
                return dashboardService.buildDashboardMetrics(monday, today);
            case ESTE_MES:
                LocalDate firstDay = today.withDayOfMonth(1);
                return dashboardService.buildDashboardMetrics(firstDay, today);
            default:
                return dashboardService.buildDashboardMetrics(today, today);
        }
    }

    /**
     * Devuelve los KPIs del Dashboard para un rango personalizado de fechas.
     * La UI llama a este metodo cuando el usuario usa el filtro "Personalizado".
     */
    public DashboardMetricsDTO getMetrics(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("El rango de fechas personalizado es obligatorio.");
        }
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("La fecha de inicio no puede ser posterior a la fecha de fin.");
        }
        return dashboardService.buildDashboardMetrics(from, to);
    }

    /** Shortcut para el arranque inicial de la app (muestra hoy por defecto). */
    public DashboardMetricsDTO getMetricsDefault() {
        return getMetrics(Period.HOY);
    }

    /**
     * Paquete optimizado de alto rendimiento para el Dashboard.
     * Carga métricas, lavados y tendencia en sólo 2 consultas.
     */
    public DashboardService.DashboardBundle getDashboardBundle(LocalDate date) {
        return dashboardService.buildDashboardBundle(date);
    }
}
