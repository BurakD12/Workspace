package com.example._10_integration_testing.service;

import com.example._10_integration_testing.model.RefreshToken;
import com.example._10_integration_testing.model.User;

public interface RefreshTokenService {
    RefreshToken create(User user);

    RefreshToken verify(String token);

    RefreshToken rotate(String token);
}