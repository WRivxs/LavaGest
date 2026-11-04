package com.lavagest.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Representa un pago fisico de comisiones al trabajador (egreso real de caja).
 * Corresponde a la tabla worker_payouts en la base de datos.
 */
public class WorkerPayout {
    private Long id;
    private Long workerId;
    private BigDecimal amount;          // Monto total liquidado y entregado en caja
    private PaymentMethod paymentMethod;
    private LocalDate paymentDate;
    private LocalTime paymentTime;
    private PayoutPaymentStatus status;
    private String notes;              // Ej: "Liquidacion turno tarde 6 lavados"

    public WorkerPayout() {
    }

    public WorkerPayout(Long id, Long workerId, BigDecimal amount, PaymentMethod paymentMethod,
                        LocalDate paymentDate, LocalTime paymentTime, String notes) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("El monto a liquidar debe ser mayor a cero.");
        }
        if (workerId == null) {
            throw new IllegalArgumentException("El trabajador es obligatorio.");
        }
        this.id = id;
        this.workerId = workerId;
        this.amount = amount;
        this.paymentMethod = paymentMethod != null ? paymentMethod : PaymentMethod.EFECTIVO;
        this.paymentDate = paymentDate != null ? paymentDate : LocalDate.now();
        this.paymentTime = paymentTime != null ? paymentTime : LocalTime.now().withNano(0);
        this.status = PayoutPaymentStatus.PAGADO;
        this.notes = notes;
    }

    // ---------- Getters y Setters ----------

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getWorkerId() { return workerId; }
    public void setWorkerId(Long workerId) { this.workerId = workerId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }

    public LocalDate getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDate paymentDate) { this.paymentDate = paymentDate; }

    public LocalTime getPaymentTime() { return paymentTime; }
    public void setPaymentTime(LocalTime paymentTime) { this.paymentTime = paymentTime; }

    public PayoutPaymentStatus getStatus() { return status; }
    public void setStatus(PayoutPaymentStatus status) { this.status = status; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    // ---------- Enums anidados del modelo ----------

    public enum PaymentMethod {
        EFECTIVO("Efectivo"),
        NEQUI("Nequi"),
        DAVIPLATA("Daviplata"),
        TRANSFERENCIA("Transferencia");

        private final String label;
        PaymentMethod(String label) { this.label = label; }
        public String getLabel() { return label; }

        public static PaymentMethod fromLabel(String value) {
            for (PaymentMethod m : values()) {
                if (m.label.equalsIgnoreCase(value)) return m;
            }
            throw new IllegalArgumentException("Metodo de pago no reconocido: " + value);
        }
    }

    public enum PayoutPaymentStatus {
        PENDIENTE("Pendiente"),
        PAGADO("Pagado"),
        ANULADO("Anulado");

        private final String label;
        PayoutPaymentStatus(String label) { this.label = label; }
        public String getLabel() { return label; }
    }
}
