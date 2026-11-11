package com.lavagest.controller;

import com.lavagest.model.AppUser;
import com.lavagest.model.Role;
import com.lavagest.service.AuthService;

import java.util.List;

public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    public AppUser login(String username, String password) {
        return authService.login(username, password);
    }

    public AppUser createUser(String username, String password, Role role, Long workerId) {
        return authService.createUser(username, password, role, workerId);
    }

    public AppUser createUser(String username, String password, Role role, Long workerId, String fullName, String phone) {
        return authService.createUser(username, password, role, workerId, fullName, phone);
    }

    public boolean updateUser(Long id, String username, String fullName, String phone, Role role, String newPassword) {
        return authService.updateUser(id, username, fullName, phone, role, newPassword);
    }

    public List<AppUser> getAllUsers() {
        return authService.getAllUsers();
    }

    public boolean deleteUser(Long id) {
        return authService.deleteUser(id);
    }

    public boolean activateUser(Long id) {
        return authService.activateUser(id);
    }
}
