package com.example._8_method_security.security;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.example._8_method_security.config.JwtProperties;
import com.example._8_method_security.controller.ProjectController;
import com.example._8_method_security.dto.PagedResponse;
import com.example._8_method_security.dto.ProjectRequestDto;
import com.example._8_method_security.dto.ProjectResponseDto;
import com.example._8_method_security.dto.UserSummaryDto;
import com.example._8_method_security.service.ProjectService;
import com.example._8_method_security.service.TaskService;
import tools.jackson.databind.ObjectMapper;

/**
 * Proves the {@link JwtAuthenticationFilter} end to end against a real protected endpoint: a valid
 * Bearer token authenticates the request (reaching the controller), while a missing, tampered, or
 * expired token is rejected as 401 by the entry point. Real {@link SecurityConfig} (hence the real
 * JwtService/filter/entry point) is imported; only the service layer is mocked.
 */
@WebMvcTest(ProjectController.class)
@Import(SecurityConfig.class)
@DisplayName("JwtAuthenticationFilter (end to end)")
class JwtAuthenticationFilterTest {

    @Autowired private WebApplicationContext context;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtService jwtService;       // real bean from SecurityConfig
    @Autowired private JwtProperties jwtProperties; // real bean (same signing secret)
    @MockitoBean private ProjectService projectService;
    @MockitoBean private TaskService taskService;
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

    private String validToken() {
        return jwtService.generateAccessToken("alice", List.of("USER"));
    }

    /**
     * Corrupt a token's signature so verification must fail. Changes the FIRST character of the
     * signature segment (6 significant bits) rather than the last: a 32-byte HS256 signature
     * base64url-encodes to 43 unpadded characters, so the final character's low 2 bits are ignored
     * padding and flipping it can decode to the identical signature, leaving the token valid.
     */
    private String tamper(String token) {
        int lastDot = token.lastIndexOf('.');
        char first = token.charAt(lastDot + 1);
        char replacement = (first == 'a') ? 'b' : 'a';
        return token.substring(0, lastDot + 1) + replacement + token.substring(lastDot + 2);
    }

    private String expiredToken() {
        // Same signing secret as the real bean, but an already-elapsed TTL.
        JwtProperties expiredProps = new JwtProperties();
        expiredProps.setSecret(jwtProperties.getSecret());
        expiredProps.setAccessTokenTtl(Duration.ofSeconds(-60));
        return new JwtService(expiredProps).generateAccessToken("alice", List.of("USER"));
    }

    @Test
    @DisplayName("valid Bearer token reaches the protected GET (200)")
    void validTokenReachesProtectedRead() throws Exception {
        given(projectService.getAllProjects(any(Pageable.class)))
                .willReturn(new PagedResponse<>(List.of(), 0, 20, 0, 0, true));

        mockMvc.perform(get("/api/projects").header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("valid Bearer token reaches a protected WRITE, principal = the token subject")
    void validTokenReachesProtectedWrite() throws Exception {
        given(projectService.createProject(any(ProjectRequestDto.class), eq("alice")))
                .willReturn(new ProjectResponseDto(7L, "Apollo", "Moon landing", new UserSummaryDto(1L, "alice")));

        mockMvc.perform(post("/api/projects")
                        .header("Authorization", "Bearer " + validToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new ProjectRequestDto("Apollo", "Moon landing"))))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/projects/7")))
                .andExpect(jsonPath("$.owner.username").value("alice"));
    }

    @Test
    @DisplayName("missing token is rejected (401)")
    void missingTokenRejected() throws Exception {
        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("tampered token is rejected (401)")
    void tamperedTokenRejected() throws Exception {
        mockMvc.perform(get("/api/projects").header("Authorization", "Bearer " + tamper(validToken())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("expired token is rejected (401)")
    void expiredTokenRejected() throws Exception {
        mockMvc.perform(get("/api/projects").header("Authorization", "Bearer " + expiredToken()))
                .andExpect(status().isUnauthorized());
    }
}
