package com.lavagest.controller;

import com.lavagest.model.VehicleTypeCatalog;
import com.lavagest.model.WashType;
import com.lavagest.repository.VehicleTypeCatalogRepository;
import com.lavagest.repository.WashTypeRepository;

import java.math.BigDecimal;
import java.util.List;

public class CatalogController {
    private final VehicleTypeCatalogRepository vehicleTypeRepo;
    private final WashTypeRepository washTypeRepo;

    public CatalogController(VehicleTypeCatalogRepository vehicleTypeRepo,
                             WashTypeRepository washTypeRepo) {
        this.vehicleTypeRepo = vehicleTypeRepo;
        this.washTypeRepo = washTypeRepo;
    }

    // --- Tipos de Vehículo ---
    public VehicleTypeCatalog createVehicleType(String name) {
        VehicleTypeCatalog v = new VehicleTypeCatalog();
        v.setName(name.trim());
        v.setActive(true);
        return vehicleTypeRepo.save(v);
    }

    public List<VehicleTypeCatalog> getAllVehicleTypes() {
        return vehicleTypeRepo.findAllActive();
    }

    public boolean deleteVehicleType(Long id) {
        return vehicleTypeRepo.delete(id);
    }

    // --- Tipos de Lavado ---
    public WashType createWashType(String name, BigDecimal price, Long vehicleTypeCatalogId) {
        WashType wt = new WashType();
        wt.setName(name.trim());
        wt.setPrice(price);
        wt.setVehicleTypeCatalogId(vehicleTypeCatalogId);
        wt.setActive(true);
        return washTypeRepo.save(wt);
    }

    public List<WashType> getAllWashTypes() {
        return washTypeRepo.findAllActive();
    }

    public List<WashType> getWashTypesByVehicleType(Long vehicleTypeCatalogId) {
        return washTypeRepo.findByVehicleTypeCatalogId(vehicleTypeCatalogId);
    }

    public boolean deleteWashType(Long id) {
        return washTypeRepo.delete(id);
    }
}
