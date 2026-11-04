package com.lavagest.dto;

import java.math.BigDecimal;

/**
 * DTO de ENTRADA para registrar una liquidacion de comisiones a un trabajador.
 * El dueño usa este DTO cuando le entrega el dinero a un lavador al final del turno.
 */
public class CreatePayoutRequest {
    private Long workerId;          // Trabajador a liquidar (obligatorio)
    private BigDecimal amount;      // Monto total a entregar (obligatorio)
    private String paymentMethod;   // Efectivo | Nequi | Daviplata | Transferencia
    private String notes;           // Ej: "Liquidacion turno tarde - 6 lavados"

    public CreatePayoutRequest() {
    }

    public CreatePayoutRequest(Long workerId, BigDecimal amount, String paymentMethod, String notes) {
        this.workerId = workerId;
        this.amount = amount;
        this.paymentMethod = paymentMethod != null ? paymentMethod : "Efectivo";
        this.notes = notes;
    }

    // ---------- Getters y Setters ----------

    public Long getWorkerId() { return workerId; }
    public void setWorkerId(Long workerId) { this.workerId = workerId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
