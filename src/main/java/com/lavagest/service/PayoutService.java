package com.lavagest.service;

import com.lavagest.model.PayoutStatus;
import com.lavagest.model.Wash;
import com.lavagest.model.WorkerPayout;
import com.lavagest.model.WorkerPayout.PaymentMethod;
import com.lavagest.model.WorkerPayout.PayoutPaymentStatus;
import com.lavagest.repository.WashRepository;
import com.lavagest.repository.WorkerPayoutRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Servicio de logica de negocio para liquidaciones y pagos de nomina.
 *
 * Flujo de negocio:
 *   1. Al final del turno, el dueño consulta cuanto le debe a cada trabajador.
 *   2. El dueño registra el pago => se crea un WorkerPayout.
 *   3. Todos los lavados PENDIENTES del trabajador en ese rango quedan LIQUIDADOS.
 */
public class PayoutService {
    private final WorkerPayoutRepository payoutRepository;
    private final WashRepository washRepository;

    public PayoutService(WorkerPayoutRepository payoutRepository, WashRepository washRepository) {
        this.payoutRepository = payoutRepository;
        this.washRepository = washRepository;
    }

    /**
     * Calcula cuanto se le debe al trabajador en comisiones pendientes (no liquidadas).
     * Sirve para mostrar el total antes de confirmar el pago.
     */
    public BigDecimal calculatePendingAmount(Long workerId) {
        List<Wash> pending = washRepository.findByWorkerIdAndPayoutStatus(workerId, PayoutStatus.PENDIENTE);
        BigDecimal total = BigDecimal.ZERO;
        for (Wash w : pending) {
            total = total.add(w.getWorkerPayment());
        }
        return total;
    }

    /**
     * Calcula cuantos lavados pendientes tiene un trabajador.
     */
    public int countPendingWashes(Long workerId) {
        return washRepository.findByWorkerIdAndPayoutStatus(workerId, PayoutStatus.PENDIENTE).size();
    }

    /**
     * Registra el pago de comisiones a un trabajador y marca todos sus lavados
     * pendientes como LIQUIDADOS.
     *
     * Reglas:
     * - El monto a pagar debe ser mayor a cero.
     * - Se registra el pago en worker_payouts.
     * - Se actualiza el payout_status de todos los lavados pendientes del trabajador.
     */
    public WorkerPayout registerPayout(Long workerId, BigDecimal amount,
                                       String paymentMethodStr, String notes) {
        if (workerId == null) {
            throw new IllegalArgumentException("El trabajador es obligatorio.");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto a liquidar debe ser mayor a cero.");
        }

        // Advertencia si el monto no coincide con el pendiente exacto (puede pagarse parcial)
        BigDecimal pending = calculatePendingAmount(workerId);
        if (amount.compareTo(pending) > 0) {
            throw new IllegalArgumentException(
                "El monto ($" + amount + ") supera el total pendiente ($" + pending + ") del trabajador.");
        }

        PaymentMethod method = PaymentMethod.EFECTIVO;
        if (paymentMethodStr != null) {
            try {
                method = PaymentMethod.fromLabel(paymentMethodStr);
            } catch (IllegalArgumentException ignored) {
                // Metodo desconocido: se usa Efectivo por defecto
            }
        }

        // Crear el registro del pago
        WorkerPayout payout = new WorkerPayout(null, workerId, amount, method, null, null, notes);
        payoutRepository.save(payout);

        // Marcar todos los lavados pendientes como liquidados y vincularlos a este pago
        List<Wash> pendingWashes = washRepository.findByWorkerIdAndPayoutStatus(workerId, PayoutStatus.PENDIENTE);
        for (Wash wash : pendingWashes) {
            wash.setPayoutId(payout.getId()); // Tambien cambia el estado a LIQUIDADO internamente
            washRepository.update(wash);
        }

        return payout;
    }

    /**
     * Anula un pago ya registrado y revierte el estado de los lavados vinculados a PENDIENTE.
     */
    public WorkerPayout cancelPayout(Long payoutId) {
        WorkerPayout payout = payoutRepository.findById(payoutId);
        if (payout == null) {
            throw new IllegalArgumentException("No existe un pago con id: " + payoutId);
        }
        if (payout.getStatus() == PayoutPaymentStatus.ANULADO) {
            throw new IllegalArgumentException("El pago ya fue anulado anteriormente.");
        }

        // Revertir estado de lavados vinculados
        List<Wash> allWashes = washRepository.findAll();
        for (Wash wash : allWashes) {
            if (payoutId.equals(wash.getPayoutId())) {
                wash.setPayoutId(null);
                wash.setPayoutStatus(PayoutStatus.PENDIENTE);
                washRepository.update(wash);
            }
        }

        payout.setStatus(PayoutPaymentStatus.ANULADO);
        return payout;
    }

    // ---- Consultas ----

    public List<WorkerPayout> getAllPayouts() {
        return payoutRepository.findAll();
    }

    public List<WorkerPayout> getPayoutsByWorker(Long workerId) {
        return payoutRepository.findByWorkerId(workerId);
    }

    public List<WorkerPayout> getPayoutsByDateRange(LocalDate from, LocalDate to) {
        return payoutRepository.findByDateRange(from, to);
    }

    public List<WorkerPayout> getPayoutsToday() {
        return payoutRepository.findByDate(LocalDate.now());
    }
}
