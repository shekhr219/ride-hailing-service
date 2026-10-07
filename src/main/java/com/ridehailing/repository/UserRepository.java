package com.ridehailing.repository;

import com.ridehailing.model.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository {
    User save(User user);
    Optional<User> findById(String id);
    Optional<User> findByPhone(String phone);
    List<User> findAll();
    boolean existsById(String id);
    void clear();
}
