package com.lavagest.repository;

import com.lavagest.config.DatabaseConfig;
import com.lavagest.model.PayoutStatus;
import com.lavagest.model.VehicleType;
import com.lavagest.model.Wash;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación JDBC de WashRepository para Supabase PostgreSQL.
 */
public class JdbcWashRepository implements WashRepository {
    private static final String BASE_SELECT = "SELECT w.*, wt.name AS wash_type_name FROM washes w LEFT JOIN wash_types wt ON w.wash_type_id = wt.id";
    private final DatabaseConfig dbConfig;

    public JdbcWashRepository(DatabaseConfig dbConfig) {
        this.dbConfig = dbConfig;
    }

    public JdbcWashRepository() {
        this(DatabaseConfig.getInstance());
    }

    @Override
    public Wash save(Wash wash) {
        String sql = "INSERT INTO washes (plate, vehicle_type, vehicle_model, worker_id, price, commission_pct, " +
                "worker_payment, wash_date, wash_time, payout_status, notes, wash_type_id, client_name, client_phone, created_by_user_id) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id";
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, wash.getPlate());
            ps.setString(2, wash.getVehicleType().getLabel());
            ps.setString(3, wash.getVehicleModel());
            ps.setLong(4, wash.getWorkerId());
            ps.setBigDecimal(5, wash.getPrice());
            ps.setBigDecimal(6, wash.getCommissionPct());
            ps.setBigDecimal(7, wash.getWorkerPayment());
            ps.setDate(8, wash.getDate() != null ? Date.valueOf(wash.getDate()) : Date.valueOf(LocalDate.now()));
            ps.setTime(9, wash.getTime() != null ? Time.valueOf(wash.getTime()) : new Time(System.currentTimeMillis()));
            ps.setString(10, wash.getPayoutStatus() != null ? wash.getPayoutStatus().getLabel() : PayoutStatus.PENDIENTE.getLabel());
            ps.setString(11, wash.getNotes());
            if (wash.getWashTypeId() != null) {
                ps.setLong(12, wash.getWashTypeId());
            } else {
                ps.setNull(12, Types.BIGINT);
            }
            ps.setString(13, wash.getClientName());
            ps.setString(14, wash.getClientPhone());
            if (wash.getCreatedByUserId() != null) {
                ps.setLong(15, wash.getCreatedByUserId());
            } else {
                ps.setNull(15, Types.BIGINT);
            }

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    wash.setId(rs.getLong("id"));
                }
            }
            return wash;
        } catch (SQLException e) {
            throw new RuntimeException("Error guardando lavado en base de datos: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean update(Wash wash) {
        String sql = "UPDATE washes SET payout_id = ?, payout_status = ?, notes = ? WHERE id = ?";
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (wash.getPayoutId() != null) {
                ps.setLong(1, wash.getPayoutId());
            } else {
                ps.setNull(1, Types.BIGINT);
            }
            ps.setString(2, wash.getPayoutStatus() != null ? wash.getPayoutStatus().getLabel() : PayoutStatus.PENDIENTE.getLabel());
            ps.setString(3, wash.getNotes());
            ps.setLong(4, wash.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Error actualizando lavado en base de datos: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Wash> findAll() {
        return queryList(BASE_SELECT + " ORDER BY w.wash_date DESC, w.wash_time DESC");
    }

    @Override
    public Wash findById(Long id) {
        String sql = BASE_SELECT + " WHERE w.id = ?";
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error buscando lavado por id: " + e.getMessage(), e);
        }
        return null;
    }

    @Override
    public List<Wash> findByDate(LocalDate date) {
        return queryListParamDate(BASE_SELECT + " WHERE w.wash_date = ? ORDER BY w.wash_time DESC", date);
    }

    @Override
    public List<Wash> findByDateRange(LocalDate from, LocalDate to) {
        String sql = BASE_SELECT + " WHERE w.wash_date BETWEEN ? AND ? ORDER BY w.wash_date DESC, w.wash_time DESC";
        List<Wash> list = new ArrayList<Wash>();
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(from));
            ps.setDate(2, Date.valueOf(to));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error consultando lavados por rango de fecha: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public List<Wash> findByWorkerId(Long workerId) {
        return queryListParamLong(BASE_SELECT + " WHERE w.worker_id = ? ORDER BY w.wash_date DESC, w.wash_time DESC", workerId);
    }

    @Override
    public List<Wash> findByWorkerIdAndDateRange(Long workerId, LocalDate from, LocalDate to) {
        String sql = BASE_SELECT + " WHERE w.worker_id = ? AND w.wash_date BETWEEN ? AND ? ORDER BY w.wash_date DESC, w.wash_time DESC";
        List<Wash> list = new ArrayList<Wash>();
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, workerId);
            ps.setDate(2, Date.valueOf(from));
            ps.setDate(3, Date.valueOf(to));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error consultando lavados por trabajador y rango: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public List<Wash> findByVehicleType(VehicleType vehicleType) {
        String sql = BASE_SELECT + " WHERE w.vehicle_type = ? ORDER BY w.wash_date DESC, w.wash_time DESC";
        List<Wash> list = new ArrayList<Wash>();
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, vehicleType.getLabel());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error consultando lavados por tipo: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public List<Wash> findByPayoutStatus(PayoutStatus status) {
        String sql = BASE_SELECT + " WHERE w.payout_status = ? ORDER BY w.wash_date DESC, w.wash_time DESC";
        List<Wash> list = new ArrayList<Wash>();
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.getLabel());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error consultando lavados por estado de pago: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public List<Wash> findByWorkerIdAndPayoutStatus(Long workerId, PayoutStatus status) {
        String sql = BASE_SELECT + " WHERE w.worker_id = ? AND w.payout_status = ? ORDER BY w.wash_date DESC, w.wash_time DESC";
        List<Wash> list = new ArrayList<Wash>();
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, workerId);
            ps.setString(2, status.getLabel());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error consultando lavados por trabajador y estado de pago: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public List<Wash> findByPlate(String plate) {
        String sql = BASE_SELECT + " WHERE UPPER(w.plate) LIKE ? ORDER BY w.wash_date DESC, w.wash_time DESC";
        List<Wash> list = new ArrayList<Wash>();
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "%" + (plate != null ? plate.toUpperCase().trim() : "") + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error buscando lavados por placa: " + e.getMessage(), e);
        }
        return list;
    }

    // ---- Helpers de mapeo y consulta ----

    private List<Wash> queryList(String sql) {
        List<Wash> list = new ArrayList<Wash>();
        try (Connection conn = dbConfig.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Error ejecutando consulta de lavados: " + e.getMessage(), e);
        }
        return list;
    }

    private List<Wash> queryListParamDate(String sql, LocalDate date) {
        List<Wash> list = new ArrayList<Wash>();
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(date));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error ejecutando consulta de lavados por fecha: " + e.getMessage(), e);
        }
        return list;
    }

    private List<Wash> queryListParamLong(String sql, Long val) {
        List<Wash> list = new ArrayList<Wash>();
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, val);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error ejecutando consulta de lavados por id: " + e.getMessage(), e);
        }
        return list;
    }

    private Wash mapRow(ResultSet rs) throws SQLException {
        Wash wash = new Wash(
                rs.getLong("id"),
                rs.getString("plate"),
                rs.getString("vehicle_model"),
                rs.getDate("wash_date") != null ? rs.getDate("wash_date").toLocalDate() : null,
                rs.getTime("wash_time") != null ? rs.getTime("wash_time").toLocalTime() : null,
                VehicleType.fromLabel(rs.getString("vehicle_type")),
                rs.getLong("worker_id"),
                rs.getBigDecimal("price"),
                rs.getBigDecimal("commission_pct"),
                rs.getBigDecimal("worker_payment")
        );
        String statusStr = rs.getString("payout_status");
        if (statusStr != null) {
            try {
                wash.setPayoutStatus(PayoutStatus.fromLabel(statusStr));
            } catch (Exception ignored) {}
        }
        long pId = rs.getLong("payout_id");
        if (!rs.wasNull()) {
            wash.setPayoutId(pId);
        }
        wash.setNotes(rs.getString("notes"));

        long wtId = rs.getLong("wash_type_id");
        if (!rs.wasNull()) {
            wash.setWashTypeId(wtId);
        }
        try {
            wash.setWashTypeName(rs.getString("wash_type_name"));
        } catch (SQLException ignored) {}
        try {
            wash.setClientName(rs.getString("client_name"));
        } catch (SQLException ignored) {}
        try {
            wash.setClientPhone(rs.getString("client_phone"));
        } catch (SQLException ignored) {}
        long cBy = rs.getLong("created_by_user_id");
        if (!rs.wasNull()) {
            wash.setCreatedByUserId(cBy);
        }
        return wash;
    }
}
