package com.example._10_integration_testing.service;

import com.example._10_integration_testing.dto.LoginRequest;
import com.example._10_integration_testing.dto.RefreshRequest;
import com.example._10_integration_testing.dto.RegisterRequest;
import com.example._10_integration_testing.dto.TokenResponse;
import com.example._10_integration_testing.dto.UserResponse;

public interface AuthService {
    UserResponse register(RegisterRequest registerRequest);

    TokenResponse login(LoginRequest loginRequest);

    TokenResponse refresh(RefreshRequest refreshRequest);
}
