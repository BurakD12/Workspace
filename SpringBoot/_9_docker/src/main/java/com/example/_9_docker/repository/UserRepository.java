package com.example._9_docker.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example._9_docker.model.User;

public interface  UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}
