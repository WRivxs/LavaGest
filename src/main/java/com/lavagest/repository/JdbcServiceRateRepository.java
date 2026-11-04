package com.lavagest.repository;

import com.lavagest.config.DatabaseConfig;
import com.lavagest.model.ServiceRate;
import com.lavagest.model.VehicleType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class JdbcServiceRateRepository implements ServiceRateRepository {
    private final DatabaseConfig dbConfig;
    private final Map<VehicleType, ServiceRate> cacheByVehicle = new ConcurrentHashMap<VehicleType, ServiceRate>();
    private volatile List<ServiceRate> allCache = null;

    public JdbcServiceRateRepository(DatabaseConfig dbConfig) {
        this.dbConfig = dbConfig;
    }

    public JdbcServiceRateRepository() {
        this(DatabaseConfig.getInstance());
    }

    @Override
    public List<ServiceRate> findAll() {
        if (allCache != null && !allCache.isEmpty()) {
            return new ArrayList<ServiceRate>(allCache);
        }
        List<ServiceRate> list = new ArrayList<ServiceRate>();
        String sql = "SELECT id, vehicle_type, service_name, default_price, commission_pct, active FROM service_rates ORDER BY id ASC";
        try (Connection conn = dbConfig.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                ServiceRate rate = mapRow(rs);
                list.add(rate);
                if (rate.isActive()) {
                    cacheByVehicle.put(rate.getVehicleType(), rate);
                }
            }
            allCache = list;
        } catch (SQLException e) {
            throw new RuntimeException("Error consultando tarifas de servicios: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public ServiceRate findByVehicleType(VehicleType vehicleType) {
        if (cacheByVehicle.containsKey(vehicleType)) {
            return cacheByVehicle.get(vehicleType);
        }
        String sql = "SELECT id, vehicle_type, service_name, default_price, commission_pct, active FROM service_rates WHERE vehicle_type = ? AND active = true";
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, vehicleType.getLabel());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    ServiceRate rate = mapRow(rs);
                    cacheByVehicle.put(vehicleType, rate);
                    return rate;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error buscando tarifa por vehiculo: " + e.getMessage(), e);
        }
        return null;
    }

    @Override
    public boolean update(ServiceRate rate) {
        String sql = "UPDATE service_rates SET service_name = ?, default_price = ?, commission_pct = ?, active = ? WHERE id = ?";
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, rate.getServiceName());
            ps.setBigDecimal(2, rate.getDefaultPrice());
            ps.setBigDecimal(3, rate.getCommissionPct());
            ps.setBoolean(4, rate.isActive());
            ps.setLong(5, rate.getId());
            boolean ok = ps.executeUpdate() > 0;
            if (ok) {
                cacheByVehicle.put(rate.getVehicleType(), rate);
                allCache = null;
            }
            return ok;
        } catch (SQLException e) {
            throw new RuntimeException("Error actualizando tarifa de servicio: " + e.getMessage(), e);
        }
    }

    private ServiceRate mapRow(ResultSet rs) throws SQLException {
        return new ServiceRate(
                rs.getLong("id"),
                VehicleType.fromLabel(rs.getString("vehicle_type")),
                rs.getString("service_name"),
                rs.getBigDecimal("default_price"),
                rs.getBigDecimal("commission_pct"),
                rs.getBoolean("active")
        );
    }
}
