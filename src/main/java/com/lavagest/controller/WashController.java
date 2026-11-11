package com.lavagest.controller;

import com.lavagest.dto.CreateWashRequest;
import com.lavagest.dto.WashTableRowDTO;
import com.lavagest.model.Wash;
import com.lavagest.model.Worker;
import com.lavagest.service.LavagestService;
import com.lavagest.service.WorkerService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controlador de lavados optimizado para evitar consultas N+1 a la base de datos.
 */
public class WashController {
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final LavagestService lavagestService;
    private final WorkerService workerService;

    public WashController(LavagestService lavagestService, WorkerService workerService) {
        this.lavagestService = lavagestService;
        this.workerService = workerService;
    }

    public WashTableRowDTO registerWash(CreateWashRequest request) {
        if (request.getPlate() == null || request.getPlate().trim().isEmpty()) {
            throw new IllegalArgumentException("La placa del vehículo es obligatoria.");
        }
        if (request.getWorkerId() == null) {
            throw new IllegalArgumentException("Selecciona el trabajador asignado.");
        }
        if (request.getPrice() == null || request.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El precio del lavado debe ser mayor a cero.");
        }

        Worker worker = workerService.getWorkerById(request.getWorkerId());

        // Determinar VehicleType legacy para compatibilidad (si no viene, usar CARRO por defecto)
        com.lavagest.model.VehicleType vt = request.getVehicleType();
        if (vt == null) {
            String vtName = request.getVehicleTypeName();
            if (vtName != null && vtName.toLowerCase().contains("moto")) {
                vt = com.lavagest.model.VehicleType.MOTO;
            } else {
                vt = com.lavagest.model.VehicleType.CARRO;
            }
        }

        Wash wash = lavagestService.registerWash(
                request.getPlate(),
                request.getVehicleModel() != null ? request.getVehicleModel() : request.getVehicleTypeName(),
                vt,
                worker,
                request.getPrice(),
                request.getCommissionPct(),
                request.getWorkerPayment(),
                request.getWashTypeId(),
                request.getWashTypeName(),
                request.getClientName(),
                request.getClientPhone(),
                request.getCreatedByUserId()
        );

        return toTableRow(wash, worker);
    }

    /** Retorna lavados del día como lista Wash (para EncargadoFrame) */
    public List<Wash> getWashesByDate(java.time.LocalDate date) {
        return lavagestService.getWashesByDate(date);
    }

    public List<Wash> getWashesByDateRange(LocalDate from, LocalDate to) {
        return lavagestService.getWashesByDateRange(from, to);
    }

    public List<Wash> getAllWashes() {
        return lavagestService.getAllWashes();
    }

    public List<WashTableRowDTO> getWashesTableToday() {
        return toTableRows(lavagestService.getWashesToday());
    }

    public List<WashTableRowDTO> getWashesTableByRange(LocalDate from, LocalDate to) {
        return toTableRows(lavagestService.getWashesByDateRange(from, to));
    }

    public List<WashTableRowDTO> getAllWashesTable() {
        return toTableRows(lavagestService.getAllWashes());
    }

    public BigDecimal[] previewTicket(BigDecimal price, BigDecimal commissionPct) {
        BigDecimal pct = commissionPct != null ? commissionPct : new BigDecimal("40.00");
        BigDecimal workerPayment = lavagestService.calculatePreviewWorkerPayment(price, pct);
        BigDecimal profit = lavagestService.calculatePreviewProfit(price, pct);
        return new BigDecimal[]{ price, workerPayment, profit };
    }

    public List<WashTableRowDTO> searchByPlate(String plate) {
        return toTableRows(lavagestService.searchByPlate(plate));
    }

    // ---- Conversores internos optimizados con caché local de trabajadores ----

    public List<WashTableRowDTO> toTableRows(List<Wash> washes) {
        List<WashTableRowDTO> rows = new ArrayList<WashTableRowDTO>();
        if (washes.isEmpty()) return rows;

        List<Worker> allWorkers = workerService.getAllWorkers();
        Map<Long, Worker> workerMap = new HashMap<Long, Worker>();
        for (Worker w : allWorkers) {
            workerMap.put(w.getId(), w);
        }

        for (Wash w : washes) {
            Worker worker = workerMap.get(w.getWorkerId());
            rows.add(toTableRow(w, worker));
        }
        return rows;
    }

    private WashTableRowDTO toTableRow(Wash wash, Worker worker) {
        WashTableRowDTO row = new WashTableRowDTO();
        row.setId(wash.getId());
        row.setWashDate(wash.getDate() != null ? wash.getDate().format(DATE_FMT) : "");
        row.setWashTime(wash.getTime() != null ? wash.getTime().format(TIME_FMT) : "");
        row.setPlate(wash.getPlate());
        row.setVehicleModel(wash.getVehicleModel() != null ? wash.getVehicleModel() : "");
        row.setVehicleTypeLabel(wash.getVehicleType() != null ? wash.getVehicleType().getLabel() : "");
        row.setWashTypeName(wash.getWashTypeName() != null ? wash.getWashTypeName() : "--");
        row.setClientName(wash.getClientName() != null ? wash.getClientName() : "--");
        row.setClientPhone(wash.getClientPhone() != null ? wash.getClientPhone() : "");
        row.setPayoutStatus(wash.getPayoutStatus() != null ? wash.getPayoutStatus().getLabel() : "Pendiente");

        if (worker != null) {
            row.setWorkerName(worker.getName());
            row.setWorkerBay(worker.getBay());
        }

        row.setPrice(wash.getPrice());
        row.setWorkerPayment(wash.getWorkerPayment());
        row.setProfit(wash.getProfit());
        row.setProfitMarginPct(wash.getProfitMarginPct());
        return row;
    }
}
