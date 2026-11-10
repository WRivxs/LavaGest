package com.lavagest.repository;

import com.lavagest.model.VehicleTypeCatalog;
import java.util.List;

public interface VehicleTypeCatalogRepository {
    VehicleTypeCatalog save(VehicleTypeCatalog v);
    List<VehicleTypeCatalog> findAll();
    List<VehicleTypeCatalog> findAllActive();
    boolean delete(Long id);
    boolean update(Long id, String newName);
    boolean activate(Long id);
}
