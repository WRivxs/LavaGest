package com.lavagest.service;

import com.lavagest.model.Worker;
import com.lavagest.repository.WorkerRepository;

import java.util.List;

/**
 * Servicio de logica de negocio para gestión de trabajadores.
 */
public class WorkerService {
    private final WorkerRepository workerRepository;

    public WorkerService(WorkerRepository workerRepository) {
        this.workerRepository = workerRepository;
    }

    /**
     * Crea un nuevo trabajador activo con los datos basicos.
     * La bahia y el turno se pueden actualizar despues.
     */
    public Worker createWorker(String name, String phone) {
        return createWorker(name, phone, "Bahia 1", "Turno Completo");
    }

    /**
     * Crea un nuevo trabajador con bahia y turno especificos.
     */
    public Worker createWorker(String name, String phone, String bay, String shift) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del trabajador es obligatorio.");
        }
        // Evitar duplicados por nombre (en el mismo local no puede haber dos trabajadores con el mismo nombre exacto)
        Worker existing = workerRepository.findByName(name.trim());
        if (existing != null) {
            throw new IllegalArgumentException("Ya existe un trabajador con el nombre: " + name.trim());
        }

        Worker worker = new Worker();
        worker.setName(name.trim());
        worker.setPhone(phone);
        worker.setBay(bay != null ? bay : "Bahia 1");
        worker.setShift(shift != null ? shift : "Turno Completo");
        worker.setActive(true);
        return workerRepository.save(worker);
    }

    /**
     * Actualiza los datos editables de un trabajador existente.
     */
    public Worker updateWorker(Long id, String name, String phone, String bay, String shift) {
        Worker worker = requireWorker(id);
        if (name != null && !name.trim().isEmpty()) worker.setName(name.trim());
        if (phone != null) worker.setPhone(phone);
        if (bay != null) worker.setBay(bay);
        if (shift != null) worker.setShift(shift);
        workerRepository.update(worker);
        return worker;
    }

    /**
     * Desactiva un trabajador (baja logica, no se borra de la BD).
     * Un trabajador inactivo no puede recibir nuevos lavados.
     */
    public Worker deactivateWorker(Long id) {
        Worker worker = requireWorker(id);
        worker.setActive(false);
        workerRepository.update(worker);
        return worker;
    }

    /**
     * Reactiva un trabajador previamente dado de baja.
     */
    public Worker reactivateWorker(Long id) {
        Worker worker = requireWorker(id);
        worker.setActive(true);
        workerRepository.update(worker);
        return worker;
    }

    public List<Worker> getAllWorkers() {
        return workerRepository.findAll();
    }

    public List<Worker> getActiveWorkers() {
        return workerRepository.findAllActive();
    }

    public Worker getWorkerById(Long id) {
        return requireWorker(id);
    }

    // ---- Validaciones internas ----

    private Worker requireWorker(Long id) {
        Worker worker = workerRepository.findById(id);
        if (worker == null) {
            throw new IllegalArgumentException("No existe un trabajador con id: " + id);
        }
        return worker;
    }
}
