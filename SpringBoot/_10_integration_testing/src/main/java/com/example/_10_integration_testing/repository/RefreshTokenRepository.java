package com.example._10_integration_testing.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example._10_integration_testing.model.RefreshToken;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);
}
