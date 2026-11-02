package com.example._7_jwt.service;

import java.util.List;

import com.example._7_jwt.dto.UserResponse;

public interface UserService {
    UserResponse getByUsername(String username);
    UserResponse getCurrentUser();
    List<UserResponse> getAllUsers();
}