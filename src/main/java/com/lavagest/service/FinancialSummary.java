package com.lavagest.service;

import com.lavagest.model.Wash;
import java.math.BigDecimal;
import java.util.List;

public class FinancialSummary {
    private final List<Wash> washes;

    public FinancialSummary(List<Wash> washes) {
        this.washes = washes;
    }

    public int getWashCount() {
        return washes.size();
    }

    public BigDecimal getIncome() {
        return sumPrices();
    }

    public BigDecimal getWorkerPayments() {
        BigDecimal total = BigDecimal.ZERO;
        for (Wash wash : washes) {
            total = total.add(wash.getWorkerPayment());
        }
        return total;
    }

    public BigDecimal getProfit() {
        return getIncome().subtract(getWorkerPayments());
    }

    private BigDecimal sumPrices() {
        BigDecimal total = BigDecimal.ZERO;
        for (Wash wash : washes) {
            total = total.add(wash.getPrice());
        }
        return total;
    }
}
