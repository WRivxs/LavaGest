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

    public List<AppUser> getAllUsers() {
        return authService.getAllUsers();
    }

    public boolean deleteUser(Long id) {
        return authService.deleteUser(id);
    }
}
