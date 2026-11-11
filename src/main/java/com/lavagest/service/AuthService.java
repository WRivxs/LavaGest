package com.lavagest.service;

import com.lavagest.model.AppUser;
import com.lavagest.model.Role;
import com.lavagest.repository.UserRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

public class AuthService {
    private final UserRepository userRepository;

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /** Hashea la contraseña con SHA-256 */
    public static String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error hasheando contraseña", e);
        }
    }

    /** Intenta login. Retorna el AppUser si credenciales correctas, null si no. */
    public AppUser login(String username, String password) {
        AppUser user = userRepository.findByUsername(username);
        if (user == null) return null;
        if (!user.getPasswordHash().equals(hashPassword(password))) return null;
        if (!user.isActive()) return null;
        return user;
    }

    /** Crea un nuevo usuario básico */
    public AppUser createUser(String username, String password, Role role, Long workerId) {
        return createUser(username, password, role, workerId, null, null);
    }

    /** Crea un nuevo usuario completo con nombre y teléfono */
    public AppUser createUser(String username, String password, Role role, Long workerId, String fullName, String phone) {
        AppUser u = new AppUser();
        u.setUsername(username);
        u.setFullName(fullName != null && !fullName.trim().isEmpty() ? fullName.trim() : null);
        u.setPhone(phone != null && !phone.trim().isEmpty() ? phone.trim() : null);
        u.setPasswordHash(hashPassword(password));
        u.setRole(role);
        u.setWorkerId(workerId);
        u.setActive(true);
        return userRepository.save(u);
    }

    /** Actualiza datos de un usuario y opcionalmente su contraseña */
    public boolean updateUser(Long id, String username, String fullName, String phone, Role role, String newPassword) {
        AppUser existing = userRepository.findById(id);
        if (existing == null) return false;
        existing.setUsername(username);
        existing.setFullName(fullName != null && !fullName.trim().isEmpty() ? fullName.trim() : null);
        existing.setPhone(phone != null && !phone.trim().isEmpty() ? phone.trim() : null);
        if (role != null) existing.setRole(role);
        if (newPassword != null && !newPassword.trim().isEmpty()) {
            existing.setPasswordHash(hashPassword(newPassword.trim()));
        }
        return userRepository.update(existing);
    }

    /** Lista todos los usuarios */
    public List<AppUser> getAllUsers() {
        return userRepository.findAll();
    }

    /** Desactiva un usuario */
    public boolean deleteUser(Long id) {
        return userRepository.delete(id);
    }

    /** Activa un usuario */
    public boolean activateUser(Long id) {
        return userRepository.activate(id);
    }
}
