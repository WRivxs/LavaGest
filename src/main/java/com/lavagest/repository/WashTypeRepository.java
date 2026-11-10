package com.lavagest.repository;

import com.lavagest.model.WashType;
import java.math.BigDecimal;
import java.util.List;

public interface WashTypeRepository {
    WashType save(WashType wt);
    List<WashType> findAll();
    List<WashType> findAllActive();
    List<WashType> findByVehicleTypeCatalogId(Long vehicleTypeCatalogId);
    boolean delete(Long id);
    boolean update(Long id, String newName, BigDecimal newPrice, Long vehicleTypeCatalogId);
    boolean activate(Long id);
}
