package com.ridehailing.service;

import com.ridehailing.dto.UserRegistrationRequest;
import com.ridehailing.dto.UserResponse;
import com.ridehailing.exception.InvalidOperationException;
import com.ridehailing.exception.ResourceNotFoundException;
import com.ridehailing.model.User;
import com.ridehailing.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserResponse registerUser(UserRegistrationRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Registration request cannot be null");
        }
        if (userRepository.findByPhone(request.getPhone()).isPresent()) {
            throw new InvalidOperationException("User with phone " + request.getPhone() + " is already registered");
        }

        String userId = "USER-" + UUID.randomUUID().toString().substring(0, 8);
        User user = new User(userId, request.getName().trim(), request.getPhone().trim());
        userRepository.save(user);
        return UserResponse.fromModel(user);
    }

    public UserResponse getUserById(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        return UserResponse.fromModel(user);
    }

    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(UserResponse::fromModel)
                .collect(Collectors.toList());
    }
}
