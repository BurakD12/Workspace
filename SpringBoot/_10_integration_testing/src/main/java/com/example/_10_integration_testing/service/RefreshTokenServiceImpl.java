package com.example._10_integration_testing.service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.example._10_integration_testing.config.JwtProperties;
import com.example._10_integration_testing.exception.InvalidTokenException;
import com.example._10_integration_testing.model.RefreshToken;
import com.example._10_integration_testing.model.User;
import com.example._10_integration_testing.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProperties jwtProperties;

    @Override
    public RefreshToken create(User user) {
        RefreshToken token = RefreshToken.builder()
                .user(user)
                .revoked(false)
                .expiresAt(Instant.now().plus(jwtProperties.getRefreshTokenTtl()))
                .token(UUID.randomUUID().toString())
                .build();

        RefreshToken savedToken = refreshTokenRepository.save(token);

        return savedToken;
    }

    @Override
    public RefreshToken verify(String token) {
        RefreshToken refToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new InvalidTokenException("Refresh token is not recognized"));

        if (refToken.isRevoked()) {
            throw new InvalidTokenException("Refresh token has been revoked");
        }
        if (refToken.getExpiresAt().isBefore(Instant.now())) {
            throw new InvalidTokenException("Refresh token has expired");
        }
        return refToken;
    }

    @Override
    public RefreshToken rotate(String token) {
        RefreshToken refToken = verify(token);
        refToken.setRevoked(true);
        refreshTokenRepository.save(refToken);
        return create(refToken.getUser());
    }

}
