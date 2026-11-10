package com.lavagest.repository;

import com.lavagest.config.DatabaseConfig;
import com.lavagest.model.VehicleTypeCatalog;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * ImplementaciÃ³n JDBC de VehicleTypeCatalogRepository para Supabase PostgreSQL.
 */
public class JdbcVehicleTypeCatalogRepository implements VehicleTypeCatalogRepository {
    private final DatabaseConfig dbConfig;

    public JdbcVehicleTypeCatalogRepository(DatabaseConfig dbConfig) {
        this.dbConfig = dbConfig;
    }

    @Override
    public VehicleTypeCatalog save(VehicleTypeCatalog v) {
        String sql = "INSERT INTO vehicle_types_catalog (name, active) VALUES (?, true) RETURNING id";
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, v.getName());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    v.setId(rs.getLong("id"));
                }
            }
            v.setActive(true);
            return v;
        } catch (SQLException e) {
            throw new RuntimeException("Error guardando tipo de vehÃ­culo: " + e.getMessage(), e);
        }
    }

    @Override
    public List<VehicleTypeCatalog> findAll() {
        String sql = "SELECT * FROM vehicle_types_catalog ORDER BY name";
        List<VehicleTypeCatalog> result = new ArrayList<VehicleTypeCatalog>();
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(mapRow(rs));
            }
            return result;
        } catch (SQLException e) {
            throw new RuntimeException("Error listando tipos de vehÃ­culo: " + e.getMessage(), e);
        }
    }

    @Override
    public List<VehicleTypeCatalog> findAllActive() {
        String sql = "SELECT * FROM vehicle_types_catalog WHERE active = true ORDER BY name";
        List<VehicleTypeCatalog> result = new ArrayList<VehicleTypeCatalog>();
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(mapRow(rs));
            }
            return result;
        } catch (SQLException e) {
            throw new RuntimeException("Error listando tipos de vehÃ­culo activos: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean delete(Long id) {
        String sql = "UPDATE vehicle_types_catalog SET active = false WHERE id = ?";
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Error desactivando tipo de vehÃ­culo: " + e.getMessage(), e);
        }
    }


    @Override
    public boolean update(Long id, String newName) {
        String sql = "UPDATE vehicle_types_catalog SET name = ? WHERE id = ?";
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newName.trim());
            ps.setLong(2, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Error actualizando tipo de vehiculo: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean activate(Long id) {
        String sql = "UPDATE vehicle_types_catalog SET active = true WHERE id = ?";
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Error activando tipo de vehiculo: " + e.getMessage(), e);
        }
    }

    private VehicleTypeCatalog mapRow(ResultSet rs) throws SQLException {
        VehicleTypeCatalog v = new VehicleTypeCatalog();
        v.setId(rs.getLong("id"));
        v.setName(rs.getString("name"));
        v.setActive(rs.getBoolean("active"));
        return v;
    }
}
