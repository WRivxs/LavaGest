package com.lavagest.repository;

import com.lavagest.model.Worker;
import java.util.List;

/**
 * Contrato de acceso a datos para trabajadores.
 */
public interface WorkerRepository {
    Worker save(Worker worker);
    boolean update(Worker worker);
    List<Worker> findAll();
    List<Worker> findAllActive();
    Worker findById(Long id);
    Worker findByName(String name);
}
