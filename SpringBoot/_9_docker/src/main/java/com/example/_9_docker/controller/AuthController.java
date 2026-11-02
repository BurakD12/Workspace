package com.example._9_docker.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example._9_docker.dto.LoginRequest;
import com.example._9_docker.dto.RefreshRequest;
import com.example._9_docker.dto.RegisterRequest;
import com.example._9_docker.dto.TokenResponse;
import com.example._9_docker.dto.UserResponse;
import com.example._9_docker.service.AuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RequestMapping ("/api/auth")
@RestController 
@RequiredArgsConstructor 
public class AuthController {
    private final AuthService authService;
    
    @PostMapping ("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping ("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping ("/refresh")
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }
}
