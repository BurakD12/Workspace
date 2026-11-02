package com.example._9_docker.service;

import com.example._9_docker.dto.RegisterRequest;
import com.example._9_docker.dto.UserResponse;
import com.example._9_docker.model.RefreshToken;
import com.example._9_docker.model.User;

public interface RefreshTokenService {
    RefreshToken create(User user);
    RefreshToken verify(String token);
    RefreshToken rotate(String token);
}