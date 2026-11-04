package com.lavagest.repository;

import com.lavagest.model.PayoutStatus;
import com.lavagest.model.VehicleType;
import com.lavagest.model.Wash;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Stub de repositorio Supabase para lavados.
 * En la Fase 3 se implementarán los métodos con HTTP a la API REST de Supabase.
 */
public class SupabaseWashRepository implements WashRepository {
    private final List<Wash> washes = new ArrayList<Wash>();

    @Override
    public Wash save(Wash wash) {
        washes.add(wash);
        return wash;
    }

    @Override
    public boolean update(Wash wash) {
        // TODO Fase 3: PATCH /washes?id=eq.{id}
        throw new UnsupportedOperationException("Pendiente implementacion Supabase - Fase 3");
    }

    @Override
    public List<Wash> findAll() {
        // TODO Fase 3: GET /washes
        return Collections.unmodifiableList(washes);
    }

    @Override
    public Wash findById(Long id) {
        // TODO Fase 3: GET /washes?id=eq.{id}
        throw new UnsupportedOperationException("Pendiente implementacion Supabase - Fase 3");
    }

    @Override
    public List<Wash> findByDate(LocalDate date) {
        // TODO Fase 3: GET /washes?wash_date=eq.{date}
        throw new UnsupportedOperationException("Pendiente implementacion Supabase - Fase 3");
    }

    @Override
    public List<Wash> findByDateRange(LocalDate from, LocalDate to) {
        // TODO Fase 3: GET /washes?wash_date=gte.{from}&wash_date=lte.{to}
        throw new UnsupportedOperationException("Pendiente implementacion Supabase - Fase 3");
    }

    @Override
    public List<Wash> findByWorkerId(Long workerId) {
        // TODO Fase 3: GET /washes?worker_id=eq.{workerId}
        throw new UnsupportedOperationException("Pendiente implementacion Supabase - Fase 3");
    }

    @Override
    public List<Wash> findByWorkerIdAndDateRange(Long workerId, LocalDate from, LocalDate to) {
        // TODO Fase 3: GET /washes?worker_id=eq.{id}&wash_date=gte.{from}&wash_date=lte.{to}
        throw new UnsupportedOperationException("Pendiente implementacion Supabase - Fase 3");
    }

    @Override
    public List<Wash> findByVehicleType(VehicleType vehicleType) {
        // TODO Fase 3: GET /washes?vehicle_type=eq.{type}
        throw new UnsupportedOperationException("Pendiente implementacion Supabase - Fase 3");
    }

    @Override
    public List<Wash> findByPayoutStatus(PayoutStatus status) {
        // TODO Fase 3: GET /washes?payout_status=eq.{status}
        throw new UnsupportedOperationException("Pendiente implementacion Supabase - Fase 3");
    }

    @Override
    public List<Wash> findByWorkerIdAndPayoutStatus(Long workerId, PayoutStatus status) {
        // TODO Fase 3: GET /washes?worker_id=eq.{id}&payout_status=eq.{status}
        throw new UnsupportedOperationException("Pendiente implementacion Supabase - Fase 3");
    }

    @Override
    public List<Wash> findByPlate(String plate) {
        // TODO Fase 3: GET /washes?plate=eq.{plate}
        throw new UnsupportedOperationException("Pendiente implementacion Supabase - Fase 3");
    }
}
