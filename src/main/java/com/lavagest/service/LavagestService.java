package com.lavagest.service;

import com.lavagest.model.PayoutStatus;
import com.lavagest.model.VehicleType;
import com.lavagest.model.Wash;
import com.lavagest.model.Worker;
import com.lavagest.repository.WashRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Servicio de logica de negocio para el registro y consulta de lavados.
 * Toda regla del negocio relacionada con lavados vive aqui.
 */
public class LavagestService {
    private static final BigDecimal DEFAULT_COMMISSION_PCT = new BigDecimal("40.00");

    private final WashRepository washRepository;

    public LavagestService(WashRepository washRepository) {
        this.washRepository = washRepository;
    }

    /**
     * Registra un nuevo lavado aplicando todas las reglas de negocio:
     * - Trabajador debe estar activo.
     * - Precio debe ser mayor a cero.
     * - Si no se especifica pago al trabajador, se calcula al porcentaje indicado.
     * - Si no se especifica porcentaje, se usa el estandar del 40%.
     */
    public Wash registerWash(String plate, String vehicleModel, VehicleType vehicleType,
                             Worker worker, BigDecimal price, BigDecimal commissionPct,
                             BigDecimal workerPayment) {
        validateWorker(worker);
        if (vehicleType == null) {
            throw new IllegalArgumentException("El tipo de vehiculo es obligatorio.");
        }
        if (plate == null || plate.trim().isEmpty()) {
            throw new IllegalArgumentException("La placa del vehiculo es obligatoria.");
        }
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El precio del lavado debe ser mayor a cero.");
        }

        BigDecimal pct = commissionPct != null ? commissionPct : DEFAULT_COMMISSION_PCT;

        Wash wash = new Wash(
                null,
                plate,
                vehicleModel,
                LocalDate.now(),
                LocalTime.now().withNano(0),
                vehicleType,
                worker.getId(),
                price,
                pct,
                workerPayment  // null → el constructor calcula automaticamente
        );
        return washRepository.save(wash);
    }

    /**
     * Compatibilidad con codigo anterior (sin placa).
     * @deprecated Usar registerWash con placa y modelo.
     */
    public Wash registerWash(VehicleType vehicleType, Worker worker,
                             BigDecimal price, BigDecimal workerPayment) {
        return registerWash("SIN-PLACA", null, vehicleType, worker,
                price, DEFAULT_COMMISSION_PCT, workerPayment);
    }

    // ---- Consultas ----

    public Wash findById(Long id) {
        Wash wash = washRepository.findById(id);
        if (wash == null) throw new IllegalArgumentException("No existe un lavado con id: " + id);
        return wash;
    }

    public List<Wash> getAllWashes() {
        return washRepository.findAll();
    }

    public List<Wash> getWashesToday() {
        return washRepository.findByDate(LocalDate.now());
    }

    public List<Wash> getWashesByDateRange(LocalDate from, LocalDate to) {
        if (from == null || to == null) throw new IllegalArgumentException("El rango de fechas es obligatorio.");
        if (from.isAfter(to)) throw new IllegalArgumentException("La fecha de inicio no puede ser posterior al fin.");
        return washRepository.findByDateRange(from, to);
    }

    public List<Wash> getWashesByWorker(Long workerId) {
        return washRepository.findByWorkerId(workerId);
    }

    public List<Wash> getWashesByWorkerAndDateRange(Long workerId, LocalDate from, LocalDate to) {
        return washRepository.findByWorkerIdAndDateRange(workerId, from, to);
    }

    public List<Wash> getPendingPayoutWashesByWorker(Long workerId) {
        return washRepository.findByWorkerIdAndPayoutStatus(workerId, PayoutStatus.PENDIENTE);
    }

    public List<Wash> searchByPlate(String plate) {
        return washRepository.findByPlate(plate);
    }

    /**
     * Calcula en tiempo real la ganancia antes de guardar (para la preview del formulario).
     * Ganancia = Precio - (Precio x Comision%)
     */
    public BigDecimal calculatePreviewProfit(BigDecimal price, BigDecimal commissionPct) {
        if (price == null || price.signum() <= 0) return BigDecimal.ZERO;
        BigDecimal pct = commissionPct != null ? commissionPct : DEFAULT_COMMISSION_PCT;
        BigDecimal payment = price.multiply(pct)
                .divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);
        return price.subtract(payment);
    }

    /**
     * Calcula en tiempo real el pago al trabajador antes de guardar.
     */
    public BigDecimal calculatePreviewWorkerPayment(BigDecimal price, BigDecimal commissionPct) {
        if (price == null || price.signum() <= 0) return BigDecimal.ZERO;
        BigDecimal pct = commissionPct != null ? commissionPct : DEFAULT_COMMISSION_PCT;
        return price.multiply(pct)
                .divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);
    }

    public FinancialSummary getSummary() {
        return new FinancialSummary(washRepository.findAll());
    }

    public FinancialSummary getSummaryByDateRange(LocalDate from, LocalDate to) {
        return new FinancialSummary(washRepository.findByDateRange(from, to));
    }

    // ---- Validaciones internas ----

    private void validateWorker(Worker worker) {
        if (worker == null) {
            throw new IllegalArgumentException("El trabajador es obligatorio.");
        }
        if (!worker.isActive()) {
            throw new IllegalArgumentException("El trabajador '" + worker.getName() + "' no esta activo.");
        }
    }
}
