package com.lavagest.repository;

import com.lavagest.model.WorkerPayout;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Repositorio en memoria para liquidaciones de nómina a trabajadores.
 */
public class InMemoryWorkerPayoutRepository implements WorkerPayoutRepository {
    private final List<WorkerPayout> payouts = new ArrayList<WorkerPayout>();
    private long nextId = 1L;

    @Override
    public WorkerPayout save(WorkerPayout payout) {
        if (payout.getId() == null) {
            payout.setId(nextId++);
        }
        payouts.add(payout);
        return payout;
    }

    @Override
    public List<WorkerPayout> findAll() {
        return Collections.unmodifiableList(payouts);
    }

    @Override
    public WorkerPayout findById(Long id) {
        for (WorkerPayout p : payouts) {
            if (p.getId().equals(id)) return p;
        }
        return null;
    }

    @Override
    public List<WorkerPayout> findByWorkerId(Long workerId) {
        List<WorkerPayout> result = new ArrayList<WorkerPayout>();
        for (WorkerPayout p : payouts) {
            if (workerId.equals(p.getWorkerId())) result.add(p);
        }
        return result;
    }

    @Override
    public List<WorkerPayout> findByDate(LocalDate date) {
        List<WorkerPayout> result = new ArrayList<WorkerPayout>();
        for (WorkerPayout p : payouts) {
            if (date.equals(p.getPaymentDate())) result.add(p);
        }
        return result;
    }

    @Override
    public List<WorkerPayout> findByDateRange(LocalDate from, LocalDate to) {
        List<WorkerPayout> result = new ArrayList<WorkerPayout>();
        for (WorkerPayout p : payouts) {
            LocalDate d = p.getPaymentDate();
            if (d != null && !d.isBefore(from) && !d.isAfter(to)) {
                result.add(p);
            }
        }
        return result;
    }
}
