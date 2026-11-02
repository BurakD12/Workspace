package com.example._9_docker.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example._9_docker.model.RefreshToken;

public interface  RefreshTokenRepository extends JpaRepository<RefreshToken, Long>{
    Optional<RefreshToken> findByToken(String token);
}
