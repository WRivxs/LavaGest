package com.lavagest.repository;

import com.lavagest.model.PayoutStatus;
import com.lavagest.model.VehicleType;
import com.lavagest.model.Wash;

import java.time.LocalDate;
import java.util.List;

/**
 * Contrato de acceso a datos para lavados.
 * Todas las consultas con filtros deben pasar por aqui.
 */
public interface WashRepository {
    Wash save(Wash wash);
    boolean update(Wash wash);
    List<Wash> findAll();
    Wash findById(Long id);
    List<Wash> findByDateRange(LocalDate from, LocalDate to);
    List<Wash> findByDate(LocalDate date);
    List<Wash> findByWorkerId(Long workerId);
    List<Wash> findByWorkerIdAndDateRange(Long workerId, LocalDate from, LocalDate to);
    List<Wash> findByVehicleType(VehicleType vehicleType);
    List<Wash> findByPayoutStatus(PayoutStatus status);
    List<Wash> findByWorkerIdAndPayoutStatus(Long workerId, PayoutStatus status);
    List<Wash> findByPlate(String plate);
}
