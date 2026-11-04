package com.lavagest.repository;

import com.lavagest.model.WorkerPayout;
import java.time.LocalDate;
import java.util.List;

/**
 * Contrato de acceso a datos para liquidaciones de nomina.
 */
public interface WorkerPayoutRepository {
    WorkerPayout save(WorkerPayout payout);
    List<WorkerPayout> findAll();
    WorkerPayout findById(Long id);
    List<WorkerPayout> findByWorkerId(Long workerId);
    List<WorkerPayout> findByDateRange(LocalDate from, LocalDate to);
    List<WorkerPayout> findByDate(LocalDate date);
}
