package com.lavagest.repository;

import com.lavagest.model.PayoutStatus;
import com.lavagest.model.VehicleType;
import com.lavagest.model.Wash;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Repositorio en memoria para lavados. Se usa durante el prototipo y en tests.
 * Cuando se conecte Supabase se reemplazará por SupabaseWashRepository.
 */
public class InMemoryWashRepository implements WashRepository {
    private final List<Wash> washes = new ArrayList<Wash>();
    private long nextId = 1L;

    @Override
    public Wash save(Wash wash) {
        if (wash.getId() == null) {
            wash.setId(nextId++);
        }
        washes.add(wash);
        return wash;
    }

    @Override
    public boolean update(Wash wash) {
        for (int i = 0; i < washes.size(); i++) {
            if (washes.get(i).getId().equals(wash.getId())) {
                washes.set(i, wash);
                return true;
            }
        }
        return false;
    }

    @Override
    public List<Wash> findAll() {
        return Collections.unmodifiableList(washes);
    }

    @Override
    public Wash findById(Long id) {
        for (Wash w : washes) {
            if (w.getId().equals(id)) return w;
        }
        return null;
    }

    @Override
    public List<Wash> findByDate(LocalDate date) {
        List<Wash> result = new ArrayList<Wash>();
        for (Wash w : washes) {
            if (date.equals(w.getDate())) result.add(w);
        }
        return result;
    }

    @Override
    public List<Wash> findByDateRange(LocalDate from, LocalDate to) {
        List<Wash> result = new ArrayList<Wash>();
        for (Wash w : washes) {
            LocalDate d = w.getDate();
            if (d != null && !d.isBefore(from) && !d.isAfter(to)) {
                result.add(w);
            }
        }
        return result;
    }

    @Override
    public List<Wash> findByWorkerId(Long workerId) {
        List<Wash> result = new ArrayList<Wash>();
        for (Wash w : washes) {
            if (workerId.equals(w.getWorkerId())) result.add(w);
        }
        return result;
    }

    @Override
    public List<Wash> findByWorkerIdAndDateRange(Long workerId, LocalDate from, LocalDate to) {
        List<Wash> result = new ArrayList<Wash>();
        for (Wash w : washes) {
            LocalDate d = w.getDate();
            if (workerId.equals(w.getWorkerId()) && d != null && !d.isBefore(from) && !d.isAfter(to)) {
                result.add(w);
            }
        }
        return result;
    }

    @Override
    public List<Wash> findByVehicleType(VehicleType vehicleType) {
        List<Wash> result = new ArrayList<Wash>();
        for (Wash w : washes) {
            if (vehicleType == w.getVehicleType()) result.add(w);
        }
        return result;
    }

    @Override
    public List<Wash> findByPayoutStatus(PayoutStatus status) {
        List<Wash> result = new ArrayList<Wash>();
        for (Wash w : washes) {
            if (status == w.getPayoutStatus()) result.add(w);
        }
        return result;
    }

    @Override
    public List<Wash> findByWorkerIdAndPayoutStatus(Long workerId, PayoutStatus status) {
        List<Wash> result = new ArrayList<Wash>();
        for (Wash w : washes) {
            if (workerId.equals(w.getWorkerId()) && status == w.getPayoutStatus()) {
                result.add(w);
            }
        }
        return result;
    }

    @Override
    public List<Wash> findByPlate(String plate) {
        List<Wash> result = new ArrayList<Wash>();
        if (plate == null) return result;
        String upper = plate.toUpperCase().trim();
        for (Wash w : washes) {
            if (upper.equals(w.getPlate())) result.add(w);
        }
        return result;
    }
}
