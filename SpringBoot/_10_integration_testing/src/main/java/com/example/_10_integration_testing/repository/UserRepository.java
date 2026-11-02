package com.example._10_integration_testing.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example._10_integration_testing.model.User;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}
