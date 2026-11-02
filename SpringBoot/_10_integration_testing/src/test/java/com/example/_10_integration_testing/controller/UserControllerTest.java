package com.example._10_integration_testing.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.example._10_integration_testing.dto.UserResponse;
import com.example._10_integration_testing.model.Role;
import com.example._10_integration_testing.security.SecurityConfig;
import com.example._10_integration_testing.service.UserService;

// GET /api/users/me requires authentication: anonymous → 401, any authenticated user → 200.
@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
@DisplayName("UserController")
class UserControllerTest {

        @Autowired
        private WebApplicationContext context;
        @MockitoBean
        private UserService userService;
        @MockitoBean
        private UserDetailsService userDetailsService;
        private MockMvc mockMvc;

        @BeforeEach
        void setUpMockMvc() {
                mockMvc = MockMvcBuilders.webAppContextSetup(context)
                                .apply(SecurityMockMvcConfigurers.springSecurity())
                                .build();
        }

        @Test
        @DisplayName("GET /api/users/me 401 when anonymous")
        void meAnonymousUnauthorized() throws Exception {
                mockMvc.perform(get("/api/users/me"))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(username = "alice", roles = "CLERK")
        @DisplayName("GET /api/users/me 200 returns the current principal's profile")
        void meAuthenticated() throws Exception {
                given(userService.getCurrentUser())
                                .willReturn(new UserResponse(1L, "alice", "alice@stockflow.io", Set.of(Role.CLERK)));
                mockMvc.perform(get("/api/users/me"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.username").value("alice"))
                                .andExpect(jsonPath("$.email").value("alice@stockflow.io"))
                                .andExpect(jsonPath("$.roles[0]").value("CLERK"));
        }

        // --- Lab 8: GET /api/users/{username}
        // ------------------------------------------------------

        @Test
        @WithMockUser(username = "alice", roles = "CLERK")
        @DisplayName("GET /api/users/{username} 200 for your own profile")
        void getOwnProfileByName() throws Exception {
                given(userService.getByUsername("alice"))
                                .willReturn(new UserResponse(1L, "alice", "alice@stockflow.io", Set.of(Role.CLERK)));

                mockMvc.perform(get("/api/users/alice"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.username").value("alice"));
        }

        @Test
        @WithMockUser(username = "alice", roles = "CLERK")
        @DisplayName("GET /api/users/{username} 403 ProblemDetail for someone else's profile")
        void getForeignProfileForbidden() throws Exception {
                // The real rule (#username == authentication.name) lives on UserServiceImpl and
                // is proven in
                // MethodSecurityTest; here the mock stands in for it so the 403 rendering can
                // be asserted.
                given(userService.getByUsername("bob")).willThrow(new AuthorizationDeniedException("Access Denied"));

                mockMvc.perform(get("/api/users/bob"))
                                .andExpect(status().isForbidden())
                                .andExpect(jsonPath("$.status").value(403))
                                .andExpect(jsonPath("$.title").value("Access denied"));
        }

        @Test
        @DisplayName("GET /api/users/{username} 401 when anonymous")
        void getProfileByNameAnonymous() throws Exception {
                mockMvc.perform(get("/api/users/alice"))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(username = "alice", roles = "CLERK")
        @DisplayName("GET /api/users/me still routes to /me, never binding username=\"me\"")
        void literalMeOutranksTheTemplate() throws Exception {
                given(userService.getCurrentUser())
                                .willReturn(new UserResponse(1L, "alice", "alice@stockflow.io", Set.of(Role.CLERK)));

                mockMvc.perform(get("/api/users/me"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.username").value("alice"));

                // If the template had won, getByUsername("me") would have been called instead.
                verify(userService, never()).getByUsername("me");
        }
}
