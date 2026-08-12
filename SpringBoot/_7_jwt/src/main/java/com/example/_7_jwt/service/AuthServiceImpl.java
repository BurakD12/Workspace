package com.example._7_jwt.service;

import java.util.HashSet;
import java.util.Set;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example._7_jwt.dto.RegisterRequest;
import com.example._7_jwt.dto.UserResponse;
import com.example._7_jwt.exception.DuplicateResourceException;
import com.example._7_jwt.model.Role;
import com.example._7_jwt.model.User;
import com.example._7_jwt.repository.UserRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthServiceImpl {
    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("username", request.getUsername());
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("email", request.getEmail());
        }
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                // Never store the raw password: BCrypt-hash it on the way in.
                .password(passwordEncoder.encode(request.getPassword()))
                // Self-registered users always start with the plain USER role.
                .roles(new HashSet<>(Set.of(Role.USER)))
                .build();

        User saved = userRepository.save(user);
        return new UserResponse(saved.getId(), saved.getUsername(), saved.getEmail(), saved.getRoles());
    }
}
