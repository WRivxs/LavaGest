package com.lavagest.repository;

import com.lavagest.model.AppUser;
import java.util.List;

public interface UserRepository {
    AppUser save(AppUser user);
    AppUser findByUsername(String username);
    AppUser findById(Long id);
    List<AppUser> findAll();
    boolean update(AppUser user);
    boolean delete(Long id);
    boolean activate(Long id);
}
