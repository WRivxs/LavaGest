package com.lavagest.controller;

import com.lavagest.dto.CreatePayoutRequest;
import com.lavagest.model.Worker;
import com.lavagest.model.WorkerPayout;
import com.lavagest.service.PayoutService;
import com.lavagest.service.WorkerService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Controlador de Nómina y Liquidaciones a Trabajadores.
 * Conecta la UI ("Liquidación y Nómina") con PayoutService y WorkerService.
 */
public class PayoutController {
    private final PayoutService payoutService;
    private final WorkerService workerService;

    public PayoutController(PayoutService payoutService, WorkerService workerService) {
        this.payoutService = payoutService;
        this.workerService = workerService;
    }

    /**
     * Registra una liquidación de nómina a un trabajador.
     * Valida la existencia del trabajador, registra el egreso y pasa sus lavados a 'Liquidado'.
     */
    public WorkerPayout registerPayout(CreatePayoutRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("La solicitud de liquidacion no puede ser nula.");
        }
        if (request.getWorkerId() == null) {
            throw new IllegalArgumentException("Debes seleccionar un trabajador a liquidar.");
        }
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto a liquidar debe ser mayor a cero.");
        }

        // Verificar que el trabajador existe
        Worker worker = workerService.getWorkerById(request.getWorkerId());
        if (worker == null) {
            throw new IllegalArgumentException("No se encontro el trabajador con ID: " + request.getWorkerId());
        }

        return payoutService.registerPayout(
                request.getWorkerId(),
                request.getAmount(),
                request.getPaymentMethod(),
                request.getNotes()
        );
    }

    /**
     * Obtiene el monto total de comisiones pendientes por pagar a un trabajador.
     */
    public BigDecimal getPendingAmount(Long workerId) {
        return payoutService.calculatePendingAmount(workerId);
    }

    /**
     * Obtiene la cantidad de servicios pendientes de liquidar a un trabajador.
     */
    public int getPendingWashesCount(Long workerId) {
        return payoutService.countPendingWashes(workerId);
    }

    /**
     * Historial de liquidaciones realizadas.
     */
    public List<WorkerPayout> getAllPayouts() {
        return payoutService.getAllPayouts();
    }

    /**
     * Liquidaciones realizadas hoy.
     */
    public List<WorkerPayout> getPayoutsToday() {
        return payoutService.getPayoutsToday();
    }

    /**
     * Liquidaciones en un rango de fechas.
     */
    public List<WorkerPayout> getPayoutsByDateRange(LocalDate from, LocalDate to) {
        return payoutService.getPayoutsByDateRange(from, to);
    }
}
