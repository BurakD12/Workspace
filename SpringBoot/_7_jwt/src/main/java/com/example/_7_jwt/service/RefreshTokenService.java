package com.example._7_jwt.service;

import com.example._7_jwt.model.RefreshToken;
import com.example._7_jwt.model.User;

public interface RefreshTokenService {
    RefreshToken create(User user);
    RefreshToken verify(String token);
    RefreshToken rotate(String token);
}
