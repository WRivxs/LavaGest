package com.lavagest.repository;

import com.lavagest.config.DatabaseConfig;
import com.lavagest.model.VehicleTypeCatalog;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación JDBC de VehicleTypeCatalogRepository para Supabase PostgreSQL.
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
            if (v.getCommissionPct() == null) v.setCommissionPct(new BigDecimal("40.00"));
            return v;
        } catch (SQLException e) {
            throw new RuntimeException("Error guardando tipo de vehículo: " + e.getMessage(), e);
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
            throw new RuntimeException("Error listando tipos de vehículo: " + e.getMessage(), e);
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
            throw new RuntimeException("Error listando tipos de vehículo activos: " + e.getMessage(), e);
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
            throw new RuntimeException("Error desactivando tipo de vehículo: " + e.getMessage(), e);
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

    @Override
    public boolean updateCommission(Long id, BigDecimal commissionPct) {
        String sql = "UPDATE vehicle_types_catalog SET commission_pct = ? WHERE id = ?";
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, commissionPct);
            ps.setLong(2, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Error actualizando comisión de tipo de vehiculo: " + e.getMessage(), e);
        }
    }

    private VehicleTypeCatalog mapRow(ResultSet rs) throws SQLException {
        VehicleTypeCatalog v = new VehicleTypeCatalog();
        v.setId(rs.getLong("id"));
        v.setName(rs.getString("name"));
        v.setActive(rs.getBoolean("active"));
        try {
            BigDecimal pct = rs.getBigDecimal("commission_pct");
            v.setCommissionPct(pct != null ? pct : new BigDecimal("40.00"));
        } catch (SQLException ex) {
            // Columna aún no existe (antes de migración)
            v.setCommissionPct(new BigDecimal("40.00"));
        }
        return v;
    }
}
