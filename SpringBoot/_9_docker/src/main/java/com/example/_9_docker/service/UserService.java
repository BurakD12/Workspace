package com.example._9_docker.service;

import java.util.List;

import com.example._9_docker.dto.UserResponse;

public interface UserService {
    UserResponse getByUsername(String username);
    UserResponse getCurrentUser();
    List<UserResponse> getAllUsers();
}
