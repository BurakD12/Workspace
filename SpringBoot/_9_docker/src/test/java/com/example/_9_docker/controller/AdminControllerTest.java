package com.example._9_docker.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.example._9_docker.dto.UserResponse;
import com.example._9_docker.model.Role;
import com.example._9_docker.security.SecurityConfig;
import com.example._9_docker.service.UserService;

// The whole /api/admin/** area is ADMIN-only: anonymous → 401, plain CLERK → 403, ADMIN → 200.
@WebMvcTest(AdminController.class)
@Import(SecurityConfig.class)
@DisplayName("AdminController")
class AdminControllerTest {

    @Autowired private WebApplicationContext context;
    @MockitoBean private UserService userService;
    @MockitoBean private UserDetailsService userDetailsService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUpMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    @Test
    @DisplayName("GET /api/admin/users 401 when anonymous")
    void anonymousUnauthorized() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "CLERK")
    @DisplayName("GET /api/admin/users 403 for a plain CLERK")
    void userForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "CLERK")
    @DisplayName("Lab 8: the URL-rule 403 body matches a method-security 403 (RestAccessDeniedHandler)")
    void urlRuleForbiddenRendersProblemDetail() throws Exception {
        // This 403 is born in the AuthorizationFilter, long before any controller runs, so it never
        // reaches GlobalExceptionHandler — RestAccessDeniedHandler writes it. The title/status/detail
        // must match the method-security 403 from the advice, or clients see two different bodies
        // depending on which layer said no.
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.title").value("Access denied"))
                .andExpect(jsonPath("$.detail").value("You do not have permission to perform this action."));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /api/admin/users 200 for an ADMIN, returning the user list")
    void adminAllowed() throws Exception {
        given(userService.getAllUsers()).willReturn(List.of(
                new UserResponse(1L, "admin", "admin@stockflow.local", Set.of(Role.ADMIN)),
                new UserResponse(2L, "alice", "alice@stockflow.io", Set.of(Role.CLERK))));
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("admin"))
                .andExpect(jsonPath("$[1].username").value("alice"))
                .andExpect(jsonPath("$[0].password").doesNotExist());
    }
}
