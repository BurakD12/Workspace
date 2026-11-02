package com.example._10_integration_testing.service;

import java.util.List;

import com.example._10_integration_testing.dto.UserResponse;

public interface UserService {
    UserResponse getByUsername(String username);

    UserResponse getCurrentUser();

    List<UserResponse> getAllUsers();
}
