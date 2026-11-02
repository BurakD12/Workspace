package com.example._9_docker.service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.example._9_docker.config.JwtProperties;
import com.example._9_docker.exception.InvalidTokenException;
import com.example._9_docker.exception.ResourceNotFoundException;
import com.example._9_docker.model.RefreshToken;
import com.example._9_docker.model.User;
import com.example._9_docker.repository.RefreshTokenRepository;
import com.example._9_docker.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;

@Service 
@RequiredArgsConstructor 
public class RefreshTokenServiceImpl implements RefreshTokenService{
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
