package com.example._7_jwt.service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example._7_jwt.config.JwtProperties;
import com.example._7_jwt.exception.InvalidTokenException;
import com.example._7_jwt.model.RefreshToken;
import com.example._7_jwt.model.User;
import com.example._7_jwt.repository.RefreshTokenRepository;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProperties jwtProperties;
    
    @Override
    @Transactional
    public RefreshToken create(User user) {
        RefreshToken token = RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .user(user)
                .expiresAt(Instant.now().plus(jwtProperties.getRefreshTokenTtl()))
                .revoked(false)
                .build();
        
        return refreshTokenRepository.save(token);
    }

    @Override
    @Transactional(readOnly = true)
    public RefreshToken verify(String token) {
        RefreshToken stored = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new InvalidTokenException("Refresh token is not recognized"));

        if (stored.isRevoked()) {
            throw new InvalidTokenException("Refresh token has been revoked");
        }
        if (stored.getExpiresAt().isBefore(Instant.now())) {
            throw new InvalidTokenException("Refresh token has expired");
        }
        return stored;
    }

    @Override
    @Transactional
    public RefreshToken rotate(String token) {
        RefreshToken current = verify(token);
        current.setRevoked(true);
        refreshTokenRepository.save(current);
        return create(current.getUser());
    }
}
