package com.lavagest.repository;

import com.lavagest.config.DatabaseConfig;
import com.lavagest.model.WashType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación JDBC de WashTypeRepository para Supabase PostgreSQL.
 */
public class JdbcWashTypeRepository implements WashTypeRepository {
    private final DatabaseConfig dbConfig;

    public JdbcWashTypeRepository(DatabaseConfig dbConfig) {
        this.dbConfig = dbConfig;
    }

    @Override
    public WashType save(WashType wt) {
        String sql = "INSERT INTO wash_types (name, price, vehicle_type_catalog_id, active) " +
                     "VALUES (?, ?, ?, true) RETURNING id";
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, wt.getName());
            ps.setBigDecimal(2, wt.getPrice());
            ps.setLong(3, wt.getVehicleTypeCatalogId());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    wt.setId(rs.getLong("id"));
                }
            }
            wt.setActive(true);
            return wt;
        } catch (SQLException e) {
            throw new RuntimeException("Error guardando tipo de lavado: " + e.getMessage(), e);
        }
    }

    @Override
    public List<WashType> findAll() {
        String sql = "SELECT wt.*, vtc.name as vehicle_type_name " +
                     "FROM wash_types wt " +
                     "LEFT JOIN vehicle_types_catalog vtc ON wt.vehicle_type_catalog_id = vtc.id " +
                     "ORDER BY vtc.name, wt.name";
        List<WashType> result = new ArrayList<WashType>();
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(mapRow(rs));
            }
            return result;
        } catch (SQLException e) {
            throw new RuntimeException("Error listando tipos de lavado: " + e.getMessage(), e);
        }
    }

    @Override
    public List<WashType> findAllActive() {
        String sql = "SELECT wt.*, vtc.name as vehicle_type_name " +
                     "FROM wash_types wt " +
                     "LEFT JOIN vehicle_types_catalog vtc ON wt.vehicle_type_catalog_id = vtc.id " +
                     "WHERE wt.active = true " +
                     "ORDER BY vtc.name, wt.name";
        List<WashType> result = new ArrayList<WashType>();
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(mapRow(rs));
            }
            return result;
        } catch (SQLException e) {
            throw new RuntimeException("Error listando tipos de lavado activos: " + e.getMessage(), e);
        }
    }

    @Override
    public List<WashType> findByVehicleTypeCatalogId(Long vehicleTypeCatalogId) {
        String sql = "SELECT wt.*, vtc.name as vehicle_type_name " +
                     "FROM wash_types wt " +
                     "LEFT JOIN vehicle_types_catalog vtc ON wt.vehicle_type_catalog_id = vtc.id " +
                     "WHERE wt.vehicle_type_catalog_id = ? AND wt.active = true " +
                     "ORDER BY vtc.name, wt.name";
        List<WashType> result = new ArrayList<WashType>();
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, vehicleTypeCatalogId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
            return result;
        } catch (SQLException e) {
            throw new RuntimeException("Error listando tipos de lavado por tipo de vehículo: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean delete(Long id) {
        String sql = "UPDATE wash_types SET active = false WHERE id = ?";
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Error desactivando tipo de lavado: " + e.getMessage(), e);
        }
    }

    private WashType mapRow(ResultSet rs) throws SQLException {
        WashType wt = new WashType();
        wt.setId(rs.getLong("id"));
        wt.setName(rs.getString("name"));
        wt.setPrice(rs.getBigDecimal("price"));
        wt.setVehicleTypeCatalogId(rs.getLong("vehicle_type_catalog_id"));
        wt.setVehicleTypeName(rs.getString("vehicle_type_name"));
        wt.setActive(rs.getBoolean("active"));
        return wt;
    }
}
