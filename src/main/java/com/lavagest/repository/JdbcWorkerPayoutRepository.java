package com.lavagest.repository;

import com.lavagest.config.DatabaseConfig;
import com.lavagest.model.WorkerPayout;
import com.lavagest.model.WorkerPayout.PaymentMethod;
import com.lavagest.model.WorkerPayout.PayoutPaymentStatus;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación JDBC de WorkerPayoutRepository para Supabase PostgreSQL.
 */
public class JdbcWorkerPayoutRepository implements WorkerPayoutRepository {
    private final DatabaseConfig dbConfig;

    public JdbcWorkerPayoutRepository(DatabaseConfig dbConfig) {
        this.dbConfig = dbConfig;
    }

    public JdbcWorkerPayoutRepository() {
        this(DatabaseConfig.getInstance());
    }

    @Override
    public WorkerPayout save(WorkerPayout payout) {
        String sql = "INSERT INTO worker_payouts (worker_id, amount, payment_method, payment_date, payment_time, status, notes) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING id";
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, payout.getWorkerId());
            ps.setBigDecimal(2, payout.getAmount());
            ps.setString(3, payout.getPaymentMethod() != null ? payout.getPaymentMethod().getLabel() : "Efectivo");
            ps.setDate(4, payout.getPaymentDate() != null ? Date.valueOf(payout.getPaymentDate()) : Date.valueOf(LocalDate.now()));
            ps.setTime(5, payout.getPaymentTime() != null ? Time.valueOf(payout.getPaymentTime()) : new Time(System.currentTimeMillis()));
            ps.setString(6, payout.getStatus() != null ? payout.getStatus().getLabel() : "Pagado");
            ps.setString(7, payout.getNotes());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    payout.setId(rs.getLong("id"));
                }
            }
            return payout;
        } catch (SQLException e) {
            throw new RuntimeException("Error guardando liquidacion en base de datos: " + e.getMessage(), e);
        }
    }

    @Override
    public List<WorkerPayout> findAll() {
        return queryList("SELECT * FROM worker_payouts ORDER BY payment_date DESC, payment_time DESC");
    }

    @Override
    public WorkerPayout findById(Long id) {
        String sql = "SELECT * FROM worker_payouts WHERE id = ?";
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error buscando liquidacion por id: " + e.getMessage(), e);
        }
        return null;
    }

    @Override
    public List<WorkerPayout> findByWorkerId(Long workerId) {
        String sql = "SELECT * FROM worker_payouts WHERE worker_id = ? ORDER BY payment_date DESC, payment_time DESC";
        List<WorkerPayout> list = new ArrayList<WorkerPayout>();
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, workerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error consultando liquidaciones por trabajador: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public List<WorkerPayout> findByDate(LocalDate date) {
        String sql = "SELECT * FROM worker_payouts WHERE payment_date = ? ORDER BY payment_time DESC";
        List<WorkerPayout> list = new ArrayList<WorkerPayout>();
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(date));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error consultando liquidaciones por fecha: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public List<WorkerPayout> findByDateRange(LocalDate from, LocalDate to) {
        String sql = "SELECT * FROM worker_payouts WHERE payment_date BETWEEN ? AND ? ORDER BY payment_date DESC, payment_time DESC";
        List<WorkerPayout> list = new ArrayList<WorkerPayout>();
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(from));
            ps.setDate(2, Date.valueOf(to));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error consultando liquidaciones por rango de fecha: " + e.getMessage(), e);
        }
        return list;
    }

    private List<WorkerPayout> queryList(String sql) {
        List<WorkerPayout> list = new ArrayList<WorkerPayout>();
        try (Connection conn = dbConfig.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Error ejecutando consulta de liquidaciones: " + e.getMessage(), e);
        }
        return list;
    }

    private WorkerPayout mapRow(ResultSet rs) throws SQLException {
        PaymentMethod method = PaymentMethod.EFECTIVO;
        try {
            method = PaymentMethod.fromLabel(rs.getString("payment_method"));
        } catch (Exception ignored) {}

        WorkerPayout payout = new WorkerPayout(
                rs.getLong("id"),
                rs.getLong("worker_id"),
                rs.getBigDecimal("amount"),
                method,
                rs.getDate("payment_date") != null ? rs.getDate("payment_date").toLocalDate() : null,
                rs.getTime("payment_time") != null ? rs.getTime("payment_time").toLocalTime() : null,
                rs.getString("notes")
        );

        String st = rs.getString("status");
        if ("Anulado".equalsIgnoreCase(st)) {
            payout.setStatus(PayoutPaymentStatus.ANULADO);
        } else if ("Pendiente".equalsIgnoreCase(st)) {
            payout.setStatus(PayoutPaymentStatus.PENDIENTE);
        } else {
            payout.setStatus(PayoutPaymentStatus.PAGADO);
        }

        return payout;
    }
}
