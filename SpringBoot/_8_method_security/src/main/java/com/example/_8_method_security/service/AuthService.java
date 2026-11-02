package com.example._8_method_security.service;

import com.example._8_method_security.dto.LoginRequest;
import com.example._8_method_security.dto.RefreshRequest;
import com.example._8_method_security.dto.RegisterRequest;
import com.example._8_method_security.dto.TokenResponse;
import com.example._8_method_security.dto.UserResponse;

public interface AuthService {
    UserResponse register(RegisterRequest request);

    TokenResponse login(LoginRequest request);

    TokenResponse refresh(RefreshRequest request);
}
