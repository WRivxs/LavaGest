package com.lavagest.repository;

import com.lavagest.model.AppUser;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Implementación en memoria de UserRepository (modo sin base de datos).
 */
public class InMemoryUserRepository implements UserRepository {
    private final List<AppUser> store = new ArrayList<AppUser>();
    private final AtomicLong counter = new AtomicLong(1);

    @Override
    public AppUser save(AppUser user) {
        user.setId(counter.getAndIncrement());
        store.add(user);
        return user;
    }

    @Override
    public AppUser findByUsername(String username) {
        for (AppUser u : store) {
            if (u.isActive() && u.getUsername().equalsIgnoreCase(username)) {
                return u;
            }
        }
        return null;
    }

    @Override
    public AppUser findById(Long id) {
        for (AppUser u : store) {
            if (u.getId().equals(id)) {
                return u;
            }
        }
        return null;
    }

    @Override
    public List<AppUser> findAll() {
        List<AppUser> result = new ArrayList<AppUser>();
        for (AppUser u : store) {
            result.add(u);
        }
        return result;
    }

    @Override
    public boolean update(AppUser user) {
        for (int i = 0; i < store.size(); i++) {
            if (store.get(i).getId().equals(user.getId())) {
                store.set(i, user);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean delete(Long id) {
        for (AppUser u : store) {
            if (u.getId().equals(id)) {
                u.setActive(false);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean activate(Long id) {
        for (AppUser u : store) {
            if (u.getId().equals(id)) {
                u.setActive(true);
                return true;
            }
        }
        return false;
    }
}
