package com.lavagest.repository;

import com.lavagest.config.DatabaseConfig;
import com.lavagest.model.AppUser;
import com.lavagest.model.Role;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación JDBC de UserRepository para Supabase PostgreSQL.
 */
public class JdbcUserRepository implements UserRepository {
    private final DatabaseConfig dbConfig;

    public JdbcUserRepository(DatabaseConfig dbConfig) {
        this.dbConfig = dbConfig;
    }

    @Override
    public AppUser save(AppUser user) {
        String sql = "INSERT INTO app_users (username, password_hash, role, worker_id, active, full_name, phone) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING id";
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPasswordHash());
            ps.setString(3, user.getRole().getLabel());
            if (user.getWorkerId() != null) {
                ps.setLong(4, user.getWorkerId());
            } else {
                ps.setNull(4, Types.BIGINT);
            }
            ps.setBoolean(5, user.isActive());
            ps.setString(6, user.getFullName());
            ps.setString(7, user.getPhone());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    user.setId(rs.getLong("id"));
                }
            }
            return user;
        } catch (SQLException e) {
            throw new RuntimeException("Error guardando usuario en base de datos: " + e.getMessage(), e);
        }
    }

    @Override
    public AppUser findByUsername(String username) {
        String sql = "SELECT * FROM app_users WHERE username = ? AND active = true";
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
            return null;
        } catch (SQLException e) {
            throw new RuntimeException("Error buscando usuario por username: " + e.getMessage(), e);
        }
    }

    @Override
    public AppUser findById(Long id) {
        String sql = "SELECT * FROM app_users WHERE id = ?";
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
            return null;
        } catch (SQLException e) {
            throw new RuntimeException("Error buscando usuario por id: " + e.getMessage(), e);
        }
    }

    @Override
    public List<AppUser> findAll() {
        String sql = "SELECT * FROM app_users ORDER BY username";
        List<AppUser> result = new ArrayList<AppUser>();
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(mapRow(rs));
            }
            return result;
        } catch (SQLException e) {
            throw new RuntimeException("Error listando usuarios: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean update(AppUser user) {
        String sql = "UPDATE app_users SET username=?, role=?, worker_id=?, active=?, full_name=?, phone=?, password_hash=? WHERE id=?";
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getRole().getLabel());
            if (user.getWorkerId() != null) {
                ps.setLong(3, user.getWorkerId());
            } else {
                ps.setNull(3, Types.BIGINT);
            }
            ps.setBoolean(4, user.isActive());
            ps.setString(5, user.getFullName());
            ps.setString(6, user.getPhone());
            ps.setString(7, user.getPasswordHash());
            ps.setLong(8, user.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Error actualizando usuario: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean delete(Long id) {
        String sql = "UPDATE app_users SET active = false WHERE id = ?";
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Error desactivando usuario: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean activate(Long id) {
        String sql = "UPDATE app_users SET active = true WHERE id = ?";
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Error activando usuario: " + e.getMessage(), e);
        }
    }

    private AppUser mapRow(ResultSet rs) throws SQLException {
        AppUser u = new AppUser();
        u.setId(rs.getLong("id"));
        u.setUsername(rs.getString("username"));
        u.setFullName(rs.getString("full_name"));
        u.setPhone(rs.getString("phone"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setRole(Role.fromLabel(rs.getString("role")));
        long workerId = rs.getLong("worker_id");
        u.setWorkerId(rs.wasNull() ? null : workerId);
        u.setActive(rs.getBoolean("active"));
        return u;
    }
}
