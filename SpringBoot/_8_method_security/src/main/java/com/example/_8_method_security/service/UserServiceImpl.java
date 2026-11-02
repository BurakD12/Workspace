package com.example._8_method_security.service;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example._8_method_security.dto.UserResponse;
import com.example._8_method_security.exception.ResourceNotFoundException;
import com.example._8_method_security.model.User;
import com.example._8_method_security.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;

    @Override
    @PreAuthorize("hasRole('ADMIN') or #username == authentication.name")
    public UserResponse getByUsername(String username) {
        return toResponse(load(username));
    }

    @Override
    public UserResponse getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResourceNotFoundException("User", "current");
        }
        return toResponse(load(authentication.getName()));
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream().map(this::toResponse).toList();
    }

    private UserResponse toResponse(User user) {
        UserResponse userResponse = new UserResponse(user.getId(), user.getUsername(), user.getEmail(),
                user.getRoles());
        return userResponse;
    }

    private User load(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", username));
    }
}
