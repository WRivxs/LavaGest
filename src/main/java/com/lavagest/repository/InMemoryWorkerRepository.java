package com.lavagest.repository;

import com.lavagest.model.Worker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Repositorio en memoria para trabajadores.
 */
public class InMemoryWorkerRepository implements WorkerRepository {
    private final List<Worker> workers = new ArrayList<Worker>();
    private long nextId = 1L;

    @Override
    public Worker save(Worker worker) {
        if (worker.getId() == null) {
            worker.setId(nextId++);
        }
        workers.add(worker);
        return worker;
    }

    @Override
    public boolean update(Worker worker) {
        for (int i = 0; i < workers.size(); i++) {
            if (workers.get(i).getId().equals(worker.getId())) {
                workers.set(i, worker);
                return true;
            }
        }
        return false;
    }

    @Override
    public List<Worker> findAll() {
        return Collections.unmodifiableList(workers);
    }

    @Override
    public List<Worker> findAllActive() {
        List<Worker> result = new ArrayList<Worker>();
        for (Worker w : workers) {
            if (w.isActive()) result.add(w);
        }
        return result;
    }

    @Override
    public Worker findById(Long id) {
        for (Worker w : workers) {
            if (w.getId().equals(id)) return w;
        }
        return null;
    }

    @Override
    public Worker findByName(String name) {
        if (name == null) return null;
        for (Worker w : workers) {
            if (name.trim().equalsIgnoreCase(w.getName())) return w;
        }
        return null;
    }
}
