package com.lavagest.controller;

import com.lavagest.dto.WorkerKpiDTO;
import com.lavagest.model.PayoutStatus;
import com.lavagest.model.VehicleType;
import com.lavagest.model.Wash;
import com.lavagest.model.Worker;
import com.lavagest.service.LavagestService;
import com.lavagest.service.WorkerService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Controlador de trabajadores.
 * Gestiona el CRUD de lavadores y construye los KPIs individuales para la UI.
 */
public class WorkerController {
    private final WorkerService workerService;
    private final LavagestService lavagestService;

    public WorkerController(WorkerService workerService, LavagestService lavagestService) {
        this.workerService = workerService;
        this.lavagestService = lavagestService;
    }

    /**
     * Registra un nuevo trabajador.
     * Devuelve su KPI (con todos los contadores en 0 por ser nuevo).
     */
    public WorkerKpiDTO createWorker(String name, String phone, String bay, String shift) {
        Worker worker = workerService.createWorker(name, phone, bay, shift);
        return buildKpi(worker, lavagestService.getAllWashes(), LocalDate.MIN, LocalDate.MAX);
    }

    /**
     * Desactiva un trabajador (baja logica).
     */
    public WorkerKpiDTO deactivateWorker(Long workerId) {
        Worker worker = workerService.deactivateWorker(workerId);
        return buildKpi(worker, lavagestService.getAllWashes(), LocalDate.MIN, LocalDate.MAX);
    }

    /**
     * Reactiva un trabajador.
     */
    public WorkerKpiDTO reactivateWorker(Long workerId) {
        Worker worker = workerService.reactivateWorker(workerId);
        return buildKpi(worker, lavagestService.getAllWashes(), LocalDate.MIN, LocalDate.MAX);
    }

    /**
     * Devuelve los KPIs de todos los trabajadores para el rango de fechas indicado.
     * Alimenta la tabla del panel "Trabajadores" y las tarjetas de rendimiento del Dashboard.
     */
    public List<WorkerKpiDTO> getAllWorkersKpi(LocalDate from, LocalDate to) {
        List<Wash> washes = lavagestService.getWashesByDateRange(from, to);
        List<WorkerKpiDTO> result = new ArrayList<WorkerKpiDTO>();
        for (Worker worker : workerService.getAllWorkers()) {
            result.add(buildKpi(worker, washes, from, to));
        }
        return result;
    }

    /**
     * KPIs históricos de todos los trabajadores.
     */
    public List<WorkerKpiDTO> getAllWorkersKpiHistoric() {
        List<Wash> washes = lavagestService.getAllWashes();
        List<WorkerKpiDTO> result = new ArrayList<WorkerKpiDTO>();
        for (Worker worker : workerService.getAllWorkers()) {
            result.add(buildKpi(worker, washes, LocalDate.MIN, LocalDate.MAX));
        }
        return result;
    }

    /**
     * KPIs de hoy para la sección de rendimiento del Dashboard.
     */
    public List<WorkerKpiDTO> getWorkersKpiToday() {
        LocalDate today = LocalDate.now();
        return getAllWorkersKpi(today, today);
    }

    /**
     * KPI individual de un trabajador con su nómina pendiente.
     */
    public WorkerKpiDTO getWorkerKpi(Long workerId) {
        Worker worker = workerService.getWorkerById(workerId);
        List<Wash> all = lavagestService.getAllWashes();
        return buildKpi(worker, all, LocalDate.MIN, LocalDate.MAX);
    }

    public List<Worker> getActiveWorkers() {
        return workerService.getActiveWorkers();
    }

    public Worker getWorkerById(Long id) {
        return workerService.getWorkerById(id);
    }

    public Worker updateWorker(Long id, String name, String phone, String bay, String shift) {
        return workerService.updateWorker(id, name, phone, bay, shift);
    }

    /**
     * Construye KPIs de trabajadores activos en memoria a partir de lavados ya cargados (0 consultas extra).
     */
    public List<WorkerKpiDTO> buildKpisFromWashes(List<Wash> washes, LocalDate from, LocalDate to) {
        List<WorkerKpiDTO> result = new ArrayList<WorkerKpiDTO>();
        for (Worker worker : workerService.getActiveWorkers()) {
            result.add(buildKpi(worker, washes, from, to));
        }
        return result;
    }

    // ---- Conversores internos ----

    /**
     * Construye el KPI de un trabajador calculando sus metricas sobre el subconjunto
     * de lavados recibidos. Asi el mismo metodo sirve para filtros de periodo.
     */
    private WorkerKpiDTO buildKpi(Worker worker, List<Wash> washes, LocalDate from, LocalDate to) {
        WorkerKpiDTO kpi = new WorkerKpiDTO();
        kpi.setWorkerId(worker.getId());
        kpi.setWorkerName(worker.getName());
        kpi.setPhone(worker.getPhone());
        kpi.setBay(worker.getBay());
        kpi.setShift(worker.getShift());
        kpi.setActive(worker.isActive());

        int totalWashes = 0;
        int cars = 0;
        int motos = 0;
        BigDecimal generated = BigDecimal.ZERO;
        BigDecimal commission = BigDecimal.ZERO;
        BigDecimal profit = BigDecimal.ZERO;
        int pendingWashes = 0;
        BigDecimal pendingAmount = BigDecimal.ZERO;

        for (Wash w : washes) {
            if (!worker.getId().equals(w.getWorkerId())) continue;
            // Aplicar filtro de fecha si viene
            if (w.getDate() != null && !from.equals(LocalDate.MIN)) {
                if (w.getDate().isBefore(from) || w.getDate().isAfter(to)) continue;
            }
            totalWashes++;
            generated = generated.add(w.getPrice());
            commission = commission.add(w.getWorkerPayment());
            profit = profit.add(w.getProfit());
            if (VehicleType.CARRO == w.getVehicleType()) cars++;
            else motos++;
            if (PayoutStatus.PENDIENTE == w.getPayoutStatus()) {
                pendingWashes++;
                pendingAmount = pendingAmount.add(w.getWorkerPayment());
            }
        }

        kpi.setTotalWashes(totalWashes);
        kpi.setCarsWashed(cars);
        kpi.setMotosWashed(motos);
        kpi.setTotalGenerated(generated);
        kpi.setTotalCommission(commission);
        kpi.setBusinessProfit(profit);
        kpi.setAvgTicket(totalWashes > 0
                ? generated.divide(BigDecimal.valueOf(totalWashes), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO);
        kpi.setPendingWashes(pendingWashes);
        kpi.setPendingToPay(pendingAmount);

        return kpi;
    }
}
