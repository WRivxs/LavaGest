package com.lavagest.repository;

import com.lavagest.model.ServiceRate;
import com.lavagest.model.VehicleType;

import java.util.List;

public interface ServiceRateRepository {
    List<ServiceRate> findAll();
    ServiceRate findByVehicleType(VehicleType vehicleType);
    boolean update(ServiceRate rate);
}
