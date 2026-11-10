package com.lavagest.repository;

import com.lavagest.model.WashType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * ImplementaciÃ³n en memoria de WashTypeRepository (modo sin base de datos).
 * Precarga tipos de lavado reales.
 */
public class InMemoryWashTypeRepository implements WashTypeRepository {
    private final List<WashType> store = new ArrayList<WashType>();
    private final AtomicLong counter = new AtomicLong(8);

    public InMemoryWashTypeRepository() {
        // Moto (vehicleTypeCatalogId=1)
        store.add(new WashType(1L, "Lavado normal",   new BigDecimal("7000"),  1L, "Moto",  true));
        store.add(new WashType(2L, "Brillado",        new BigDecimal("10000"), 1L, "Moto",  true));
        // Carro (vehicleTypeCatalogId=2)
        store.add(new WashType(3L, "Lavado general",    new BigDecimal("30000"), 2L, "Carro", true));
        store.add(new WashType(4L, "Lavado con motor",  new BigDecimal("45000"), 2L, "Carro", true));
        store.add(new WashType(5L, "Lavado + brillada", new BigDecimal("45000"), 2L, "Carro", true));
        store.add(new WashType(6L, "Lavado + polichada",new BigDecimal("50000"), 2L, "Carro", true));
        store.add(new WashType(7L, "Completo",          new BigDecimal("70000"), 2L, "Carro", true));
    }

    @Override
    public WashType save(WashType wt) {
        wt.setId(counter.getAndIncrement());
        store.add(wt);
        return wt;
    }

    @Override
    public List<WashType> findAll() {
        List<WashType> result = new ArrayList<WashType>();
        for (WashType wt : store) {
            result.add(wt);
        }
        return result;
    }

    @Override
    public List<WashType> findAllActive() {
        List<WashType> result = new ArrayList<WashType>();
        for (WashType wt : store) {
            if (wt.isActive()) {
                result.add(wt);
            }
        }
        return result;
    }

    @Override
    public List<WashType> findByVehicleTypeCatalogId(Long vehicleTypeCatalogId) {
        List<WashType> result = new ArrayList<WashType>();
        for (WashType wt : store) {
            if (wt.isActive() && vehicleTypeCatalogId.equals(wt.getVehicleTypeCatalogId())) {
                result.add(wt);
            }
        }
        return result;
    }

    @Override
    public boolean delete(Long id) {
        for (WashType wt : store) {
            if (wt.getId().equals(id)) {
                wt.setActive(false);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean update(Long id, String newName, java.math.BigDecimal newPrice, Long vehicleTypeCatalogId) {
        for (WashType wt : store) {
            if (wt.getId().equals(id)) {
                wt.setName(newName.trim());
                wt.setPrice(newPrice);
                wt.setVehicleTypeCatalogId(vehicleTypeCatalogId);
                return true;
            }
        }
        return false;
    }

    public boolean update(Long id, String newName, java.math.BigDecimal newPrice) {
        return update(id, newName, newPrice, null);
    }

    @Override
    public boolean activate(Long id) {
        for (WashType wt : store) {
            if (wt.getId().equals(id)) {
                wt.setActive(true);
                return true;
            }
        }
        return false;
    }
}