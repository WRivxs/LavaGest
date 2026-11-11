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
        v.setCommissionPct(new BigDecimal("40.00"));
        return vehicleTypeRepo.save(v);
    }

    public List<VehicleTypeCatalog> getAllVehicleTypes() {
        return vehicleTypeRepo.findAllActive();
    }

    public List<VehicleTypeCatalog> getAllVehicleTypesAdmin() {
        return vehicleTypeRepo.findAll();
    }

    public boolean updateVehicleType(Long id, String newName) {
        return vehicleTypeRepo.update(id, newName);
    }

    public boolean activateVehicleType(Long id) {
        return vehicleTypeRepo.activate(id);
    }

    public boolean deleteVehicleType(Long id) {
        return vehicleTypeRepo.delete(id);
    }

    public boolean updateVehicleCommission(Long id, BigDecimal commissionPct) {
        return vehicleTypeRepo.updateCommission(id, commissionPct);
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

    public List<WashType> getAllWashTypesAdmin() {
        return washTypeRepo.findAll();
    }

    public List<WashType> getWashTypesByVehicleType(Long vehicleTypeCatalogId) {
        return washTypeRepo.findByVehicleTypeCatalogId(vehicleTypeCatalogId);
    }

    public boolean updateWashType(Long id, String newName, BigDecimal price, Long vehicleTypeCatalogId) {
        return washTypeRepo.update(id, newName, price, vehicleTypeCatalogId);
    }

    public boolean activateWashType(Long id) {
        return washTypeRepo.activate(id);
    }

    public boolean deleteWashType(Long id) {
        return washTypeRepo.delete(id);
    }
}
