package com.lavagest.repository;

import com.lavagest.model.ServiceRate;
import com.lavagest.model.VehicleType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class InMemoryServiceRateRepository implements ServiceRateRepository {
    private final List<ServiceRate> rates = new ArrayList<ServiceRate>();

    public InMemoryServiceRateRepository() {
        rates.add(new ServiceRate(1L, VehicleType.CARRO, "Lavado General + Aspirado", new BigDecimal("30000"), new BigDecimal("40.00"), true));
        rates.add(new ServiceRate(2L, VehicleType.MOTO, "Lavado Detallado + Cadena", new BigDecimal("20000"), new BigDecimal("40.00"), true));
    }

    @Override
    public List<ServiceRate> findAll() {
        return Collections.unmodifiableList(rates);
    }

    @Override
    public ServiceRate findByVehicleType(VehicleType vehicleType) {
        for (ServiceRate r : rates) {
            if (r.getVehicleType() == vehicleType && r.isActive()) {
                return r;
            }
        }
        return null;
    }

    @Override
    public boolean update(ServiceRate rate) {
        for (int i = 0; i < rates.size(); i++) {
            if (rates.get(i).getId().equals(rate.getId())) {
                rates.set(i, rate);
                return true;
            }
        }
        return false;
    }
}
