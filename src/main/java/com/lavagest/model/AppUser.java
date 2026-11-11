package com.lavagest.model;

public class AppUser {
    private Long id;
    private String username;
    private String fullName;
    private String phone;
    private String passwordHash; // SHA-256 hex
    private Role role;
    private Long workerId; // FK a workers (nullable, solo para ENCARGADO)
    private boolean active;

    public AppUser() {}

    public AppUser(Long id, String username, String passwordHash, Role role,
                   Long workerId, boolean active) {
        this(id, username, null, null, passwordHash, role, workerId, active);
    }

    public AppUser(Long id, String username, String fullName, String phone,
                   String passwordHash, Role role, Long workerId, boolean active) {
        this.id = id;
        this.username = username;
        this.fullName = fullName;
        this.phone = phone;
        this.passwordHash = passwordHash;
        this.role = role;
        this.workerId = workerId;
        this.active = active;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public Long getWorkerId() { return workerId; }
    public void setWorkerId(Long workerId) { this.workerId = workerId; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
