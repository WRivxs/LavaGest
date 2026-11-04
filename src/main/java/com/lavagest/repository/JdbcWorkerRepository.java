package com.lavagest.repository;

import com.lavagest.config.DatabaseConfig;
import com.lavagest.model.Worker;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación JDBC de WorkerRepository para Supabase PostgreSQL.
 */
public class JdbcWorkerRepository implements WorkerRepository {
    private final DatabaseConfig dbConfig;
    private volatile List<Worker> activeWorkersCache = null;

    public JdbcWorkerRepository(DatabaseConfig dbConfig) {
        this.dbConfig = dbConfig;
    }

    public JdbcWorkerRepository() {
        this(DatabaseConfig.getInstance());
    }

    @Override
    public Worker save(Worker worker) {
        String sql = "INSERT INTO workers (name, phone, bay, shift, active) VALUES (?, ?, ?, ?, ?) RETURNING id";
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, worker.getName());
            ps.setString(2, worker.getPhone());
            ps.setString(3, worker.getBay());
            ps.setString(4, worker.getShift());
            ps.setBoolean(5, worker.isActive());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    worker.setId(rs.getLong("id"));
                }
            }
            activeWorkersCache = null;
            return worker;
        } catch (SQLException e) {
            throw new RuntimeException("Error guardando trabajador en base de datos: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean update(Worker worker) {
        String sql = "UPDATE workers SET name = ?, phone = ?, bay = ?, shift = ?, active = ?, updated_at = NOW() WHERE id = ?";
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, worker.getName());
            ps.setString(2, worker.getPhone());
            ps.setString(3, worker.getBay());
            ps.setString(4, worker.getShift());
            ps.setBoolean(5, worker.isActive());
            ps.setLong(6, worker.getId());
            boolean ok = ps.executeUpdate() > 0;
            if (ok) {
                activeWorkersCache = null;
            }
            return ok;
        } catch (SQLException e) {
            throw new RuntimeException("Error actualizando trabajador en base de datos: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Worker> findAll() {
        List<Worker> list = new ArrayList<Worker>();
        String sql = "SELECT id, name, phone, bay, shift, active FROM workers ORDER BY id ASC";
        try (Connection conn = dbConfig.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error consultando trabajadores: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public List<Worker> findAllActive() {
        if (activeWorkersCache != null && !activeWorkersCache.isEmpty()) {
            return new ArrayList<Worker>(activeWorkersCache);
        }
        List<Worker> list = new ArrayList<Worker>();
        String sql = "SELECT id, name, phone, bay, shift, active FROM workers WHERE active = true ORDER BY name ASC";
        try (Connection conn = dbConfig.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            activeWorkersCache = list;
        } catch (SQLException e) {
            throw new RuntimeException("Error consultando trabajadores activos: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public Worker findById(Long id) {
        String sql = "SELECT id, name, phone, bay, shift, active FROM workers WHERE id = ?";
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error buscando trabajador por id: " + e.getMessage(), e);
        }
        return null;
    }

    @Override
    public Worker findByName(String name) {
        if (name == null) return null;
        String sql = "SELECT id, name, phone, bay, shift, active FROM workers WHERE LOWER(TRIM(name)) = LOWER(TRIM(?))";
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error buscando trabajador por nombre: " + e.getMessage(), e);
        }
        return null;
    }

    private Worker mapRow(ResultSet rs) throws SQLException {
        return new Worker(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("phone"),
                rs.getString("bay"),
                rs.getString("shift"),
                rs.getBoolean("active")
        );
    }
}
