package com.example._7_jwt.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.example._7_jwt.dto.RegisterRequest;
import com.example._7_jwt.dto.UserResponse;
import com.example._7_jwt.exception.DuplicateResourceException;
import com.example._7_jwt.model.Role;
import com.example._7_jwt.security.SecurityConfig;
import com.example._7_jwt.service.AuthService;
import tools.jackson.databind.ObjectMapper;

// POST /api/auth/register is permitAll, so these run anonymously (no @WithMockUser).
@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
@DisplayName("AuthController")
class AuthControllerTest {

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
    @DisplayName("POST /register 201 returns UserResponse (id/username/email/roles) with no password field")
    void registerSuccess() throws Exception {
        given(authService.register(any(RegisterRequest.class)))
                .willReturn(new UserResponse(1L, "alice", "alice@taskflow.io", Set.of(Role.USER)));
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json(new RegisterRequest("alice", "alice@taskflow.io", "password123"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.email").value("alice@taskflow.io"))
                .andExpect(jsonPath("$.roles[0]").value("USER"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    @DisplayName("POST /register 409 when the username is already taken")
    void registerDuplicate() throws Exception {
        given(authService.register(any(RegisterRequest.class)))
                .willThrow(new DuplicateResourceException("username", "alice"));
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json(new RegisterRequest("alice", "alice@taskflow.io", "password123"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Duplicate value"));
    }

    @Test
    @DisplayName("POST /register 400 when the username is blank")
    void registerBlankUsername() throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json(new RegisterRequest("", "alice@taskflow.io", "password123"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.username").value("Username is required"));
    }

    @Test
    @DisplayName("POST /register 400 when the email is not a valid address")
    void registerInvalidEmail() throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json(new RegisterRequest("alice", "not-an-email", "password123"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").value("Email must be a valid address"));
    }

    @Test
    @DisplayName("POST /register 400 when the password is shorter than 8 characters")
    void registerShortPassword() throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json(new RegisterRequest("alice", "alice@taskflow.io", "short"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password").value("Password must be at least 8 characters"));
    }
}
