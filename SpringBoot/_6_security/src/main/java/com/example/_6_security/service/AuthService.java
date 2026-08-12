package com.example._6_security.service;

import com.example._6_security.dto.RegisterRequest;
import com.example._6_security.dto.UserResponse;

public interface AuthService {
    UserResponse register(RegisterRequest request);
}
