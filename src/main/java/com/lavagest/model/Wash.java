package com.lavagest.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;

public class Wash {
    private Long id;
    private String plate;            // Placa del vehículo (ej: ABC-492)
    private String vehicleModel;     // Modelo descriptivo (ej: Mazda 3 Blanco)
    private LocalDate date;
    private LocalTime time;
    private VehicleType vehicleType;
    private Long workerId;
    private BigDecimal price;
    private BigDecimal commissionPct; // Porcentaje de comisión (ej: 40.00)
    private BigDecimal workerPayment;
    private PayoutStatus payoutStatus; // Si la comisión ya fue liquidada en caja
    private Long payoutId;           // FK al registro de pago en worker_payouts
    private String notes;

    public Wash() {
    }

    /**
     * Constructor completo. workerPayment se calcula automáticamente si es null
     * usando el commissionPct aplicado sobre price.
     */
    public Wash(Long id, String plate, String vehicleModel, LocalDate date, LocalTime time,
                VehicleType vehicleType, Long workerId, BigDecimal price, BigDecimal commissionPct,
                BigDecimal workerPayment) {
        if (price == null) {
            throw new IllegalArgumentException("El precio es obligatorio.");
        }
        if (price.signum() < 0) {
            throw new IllegalArgumentException("El precio no puede ser negativo.");
        }
        if (plate == null || plate.trim().isEmpty()) {
            throw new IllegalArgumentException("La placa del vehiculo es obligatoria.");
        }

        this.id = id;
        this.plate = plate.toUpperCase().trim();
        this.vehicleModel = vehicleModel;
        this.date = date;
        this.time = time;
        this.vehicleType = vehicleType;
        this.workerId = workerId;
        this.price = price;
        this.commissionPct = commissionPct != null ? commissionPct : new BigDecimal("40.00");

        // Auto-calcula pago si no se especifica explicitamente
        if (workerPayment == null) {
            this.workerPayment = price
                    .multiply(this.commissionPct)
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        } else {
            if (workerPayment.signum() < 0) {
                throw new IllegalArgumentException("El pago al trabajador no puede ser negativo.");
            }
            if (workerPayment.compareTo(price) > 0) {
                throw new IllegalArgumentException("El pago no puede superar el precio del lavado.");
            }
            this.workerPayment = workerPayment;
        }

        this.payoutStatus = PayoutStatus.PENDIENTE;
    }

    // ---------- Getters y Setters ----------

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPlate() { return plate; }
    public void setPlate(String plate) {
        this.plate = plate != null ? plate.toUpperCase().trim() : null;
    }

    public String getVehicleModel() { return vehicleModel; }
    public void setVehicleModel(String vehicleModel) { this.vehicleModel = vehicleModel; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public LocalTime getTime() { return time; }
    public void setTime(LocalTime time) { this.time = time; }

    public VehicleType getVehicleType() { return vehicleType; }
    public void setVehicleType(VehicleType vehicleType) { this.vehicleType = vehicleType; }

    public Long getWorkerId() { return workerId; }
    public void setWorkerId(Long workerId) { this.workerId = workerId; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public BigDecimal getCommissionPct() { return commissionPct; }
    public void setCommissionPct(BigDecimal commissionPct) { this.commissionPct = commissionPct; }

    public BigDecimal getWorkerPayment() { return workerPayment; }
    public void setWorkerPayment(BigDecimal workerPayment) { this.workerPayment = workerPayment; }

    public PayoutStatus getPayoutStatus() { return payoutStatus; }
    public void setPayoutStatus(PayoutStatus payoutStatus) { this.payoutStatus = payoutStatus; }

    public Long getPayoutId() { return payoutId; }
    public void setPayoutId(Long payoutId) {
        this.payoutId = payoutId;
        if (payoutId != null) {
            this.payoutStatus = PayoutStatus.LIQUIDADO;
        }
    }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    /** Ganancia neta del negocio = precio cobrado - comision del trabajador */
    public BigDecimal getProfit() {
        return price.subtract(workerPayment);
    }

    /** Margen de ganancia sobre el precio total (0-100) */
    public BigDecimal getProfitMarginPct() {
        if (price.signum() == 0) return BigDecimal.ZERO;
        return getProfit()
                .multiply(new BigDecimal("100"))
                .divide(price, 1, RoundingMode.HALF_UP);
    }
}
