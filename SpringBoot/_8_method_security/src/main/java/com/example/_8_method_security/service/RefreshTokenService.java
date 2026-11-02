package com.example._8_method_security.service;

import com.example._8_method_security.model.RefreshToken;
import com.example._8_method_security.model.User;

public interface RefreshTokenService {
    RefreshToken create(User user);

    RefreshToken verify(String token);

    RefreshToken rotate(String token);
}
