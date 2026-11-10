package com.lavagest.repository;

import com.lavagest.model.WashType;
import java.util.List;

public interface WashTypeRepository {
    WashType save(WashType wt);
    List<WashType> findAll();
    List<WashType> findAllActive();
    List<WashType> findByVehicleTypeCatalogId(Long vehicleTypeCatalogId);
    boolean delete(Long id);
}
