package com.lavagest.repository;

import com.lavagest.model.VehicleTypeCatalog;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * ImplementaciÃ³n en memoria de VehicleTypeCatalogRepository (modo sin base de datos).
 * Precarga datos de demostraciÃ³n.
 */
public class InMemoryVehicleTypeCatalogRepository implements VehicleTypeCatalogRepository {
    private final List<VehicleTypeCatalog> store = new ArrayList<VehicleTypeCatalog>();
    private final AtomicLong counter = new AtomicLong(3);

    public InMemoryVehicleTypeCatalogRepository() {
        store.add(new VehicleTypeCatalog(1L, "Moto", true));
        store.add(new VehicleTypeCatalog(2L, "Carro", true));
    }

    @Override
    public VehicleTypeCatalog save(VehicleTypeCatalog v) {
        v.setId(counter.getAndIncrement());
        store.add(v);
        return v;
    }

    @Override
    public List<VehicleTypeCatalog> findAll() {
        List<VehicleTypeCatalog> result = new ArrayList<VehicleTypeCatalog>();
        for (VehicleTypeCatalog v : store) {
            result.add(v);
        }
        return result;
    }

    @Override
    public List<VehicleTypeCatalog> findAllActive() {
        List<VehicleTypeCatalog> result = new ArrayList<VehicleTypeCatalog>();
        for (VehicleTypeCatalog v : store) {
            if (v.isActive()) {
                result.add(v);
            }
        }
        return result;
    }

    @Override
    public boolean delete(Long id) {
        for (VehicleTypeCatalog v : store) {
            if (v.getId().equals(id)) {
                v.setActive(false);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean update(Long id, String newName) {
        for (VehicleTypeCatalog v : store) {
            if (v.getId().equals(id)) {
                v.setName(newName.trim());
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean activate(Long id) {
        for (VehicleTypeCatalog v : store) {
            if (v.getId().equals(id)) {
                v.setActive(true);
                return true;
            }
        }
        return false;
    }
}