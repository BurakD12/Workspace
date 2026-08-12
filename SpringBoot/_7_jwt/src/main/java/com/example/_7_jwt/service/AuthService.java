package com.example._7_jwt.service;

import com.example._7_jwt.dto.RegisterRequest;
import com.example._7_jwt.dto.UserResponse;

public interface AuthService {
    UserResponse register(RegisterRequest request);
    TokenResponse login(LoginRequest request);
    RefreshResponse refresh(RefreshRequest request);
} 
