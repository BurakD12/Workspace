package com.example._8_method_security.service;

import java.util.List;

import com.example._8_method_security.dto.UserResponse;

public interface UserService {
    UserResponse getByUsername(String username);

    UserResponse getCurrentUser();

    List<UserResponse> getAllUsers();
}