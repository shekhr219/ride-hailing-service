package com.ridehailing.repository;

import com.ridehailing.model.User;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryUserRepository implements UserRepository {
    private final Map<String, User> userMap = new ConcurrentHashMap<>();

    @Override
    public User save(User user) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("User and User ID must not be null");
        }
        userMap.put(user.getId(), user);
        return user;
    }

    @Override
    public Optional<User> findById(String id) {
        if (id == null) return Optional.empty();
        return Optional.ofNullable(userMap.get(id));
    }

    @Override
    public Optional<User> findByPhone(String phone) {
        if (phone == null) return Optional.empty();
        return userMap.values().stream()
                .filter(u -> phone.equals(u.getPhone()))
                .findFirst();
    }

    @Override
    public List<User> findAll() {
        return new ArrayList<>(userMap.values());
    }

    @Override
    public boolean existsById(String id) {
        return id != null && userMap.containsKey(id);
    }

    @Override
    public void clear() {
        userMap.clear();
    }
}
