package com.example._9_docker.service;

import com.example._9_docker.dto.LoginRequest;
import com.example._9_docker.dto.RefreshRequest;
import com.example._9_docker.dto.RegisterRequest;
import com.example._9_docker.dto.TokenResponse;
import com.example._9_docker.dto.UserResponse;

public interface AuthService {
    UserResponse register(RegisterRequest registerRequest);
    TokenResponse login(LoginRequest loginRequest);
    TokenResponse refresh(RefreshRequest refreshRequest);
}
