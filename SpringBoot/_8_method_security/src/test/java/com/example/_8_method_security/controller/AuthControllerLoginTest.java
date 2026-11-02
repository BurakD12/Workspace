package com.example._8_method_security.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.example._8_method_security.dto.LoginRequest;
import com.example._8_method_security.dto.RefreshRequest;
import com.example._8_method_security.dto.TokenResponse;
import com.example._8_method_security.exception.InvalidTokenException;
import com.example._8_method_security.security.SecurityConfig;
import com.example._8_method_security.service.AuthService;
import tools.jackson.databind.ObjectMapper;

// POST /api/auth/login and /api/auth/refresh are permitAll, so these run anonymously. AuthService is
// mocked (the orchestration is unit-tested in AuthServiceImplTest); here we assert the HTTP contract.
@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
@DisplayName("AuthController (login/refresh)")
class AuthControllerLoginTest {

    @Autowired private WebApplicationContext context;
    @Autowired private ObjectMapper objectMapper;
    @MockitoBean private AuthService authService;
    @MockitoBean private UserDetailsService userDetailsService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUpMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    private byte[] json(Object o) throws Exception {
        return objectMapper.writeValueAsBytes(o);
    }

    @Test
    @DisplayName("POST /login 200 returns an access + refresh token (Bearer, expiresIn)")
    void loginSuccess() throws Exception {
        given(authService.login(any(LoginRequest.class)))
                .willReturn(new TokenResponse("access.jwt.token", "refresh-uuid", "Bearer", 900L));

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(new LoginRequest("alice", "password123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access.jwt.token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-uuid"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900));
    }

    @Test
    @DisplayName("POST /login 401 on bad credentials (ProblemDetail)")
    void loginBadCredentials() throws Exception {
        given(authService.login(any(LoginRequest.class)))
                .willThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(new LoginRequest("alice", "wrong-password"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Authentication failed"));
    }

    @Test
    @DisplayName("POST /login 400 when the username is blank")
    void loginBlankUsername() throws Exception {
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(new LoginRequest("", "password123"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.username").value("Username is required"));
    }

    @Test
    @DisplayName("POST /refresh 200 returns a new access token and the rotated refresh token")
    void refreshSuccess() throws Exception {
        given(authService.refresh(any(RefreshRequest.class)))
                .willReturn(new TokenResponse("new.access.token", "rotated-uuid", "Bearer", 900L));

        mockMvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content(json(new RefreshRequest("old-refresh-uuid"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new.access.token"))
                .andExpect(jsonPath("$.refreshToken").value("rotated-uuid"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    @DisplayName("POST /refresh 401 when the refresh token is invalid/expired/revoked (ProblemDetail)")
    void refreshInvalid() throws Exception {
        given(authService.refresh(any(RefreshRequest.class)))
                .willThrow(new InvalidTokenException("Refresh token has expired"));

        mockMvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content(json(new RefreshRequest("expired-uuid"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Invalid token"));
    }

    @Test
    @DisplayName("POST /refresh 400 when the refresh token is blank")
    void refreshBlank() throws Exception {
        mockMvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content(json(new RefreshRequest(""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.refreshToken").value("Refresh token is required"));
    }
}
