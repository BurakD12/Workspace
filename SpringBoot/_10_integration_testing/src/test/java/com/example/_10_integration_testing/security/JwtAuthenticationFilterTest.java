package com.example._10_integration_testing.security;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.example._10_integration_testing.config.JwtProperties;
import com.example._10_integration_testing.controller.CategoryController;
import com.example._10_integration_testing.dto.CategoryRequestDto;
import com.example._10_integration_testing.dto.CategoryResponseDto;
import com.example._10_integration_testing.dto.PagedResponse;
import com.example._10_integration_testing.service.CategoryService;
import tools.jackson.databind.ObjectMapper;

/**
 * Lab 9 (StockFlow) — end-to-end proof of the Bearer filter against a real
 * protected endpoint, using
 * the REAL {@code JwtService} / filter / entry point from
 * {@code SecurityConfig} (only the service
 * layer and the {@code UserDetailsService} are mocked).
 *
 * <p>
 * Unlike the {@code @WithMockUser} slices, these tests put an actual signed
 * token on the wire, so
 * they also prove that the {@code roles} claim inside the token is what drives
 * {@code hasRole}: a
 * CLERK token can read the catalogue but is refused a catalogue write, an ADMIN
 * token is not.
 */
@WebMvcTest(CategoryController.class)
@Import(SecurityConfig.class)
@DisplayName("JwtAuthenticationFilter (end to end)")
class JwtAuthenticationFilterTest {

    @Autowired
    private WebApplicationContext context;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private JwtService jwtService; // real bean from SecurityConfig
    @Autowired
    private JwtProperties jwtProperties; // real bean (same signing secret)
    @MockitoBean
    private CategoryService categoryService;
    @MockitoBean
    private UserDetailsService userDetailsService;
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

    private String clerkToken() {
        return jwtService.generateAccessToken("alice", List.of("CLERK"));
    }

    private String adminToken() {
        return jwtService.generateAccessToken("admin", List.of("ADMIN"));
    }

    private String expiredToken() {
        // Same signing secret as the real bean, but an already-elapsed TTL.
        JwtProperties expiredProps = new JwtProperties();
        expiredProps.setSecret(jwtProperties.getSecret());
        expiredProps.setAccessTokenTtl(Duration.ofSeconds(-60));
        return new JwtService(expiredProps).generateAccessToken("alice", List.of("CLERK"));
    }

    /**
     * Corrupt a token's signature so verification must fail. Changes the FIRST
     * character of the
     * signature segment (6 significant bits) rather than the last: a 32-byte HS256
     * signature
     * base64url-encodes to 43 unpadded characters, so the final character's low 2
     * bits are ignored
     * padding and flipping it can decode to the identical signature, leaving the
     * token valid.
     */
    private String tamper(String token) {
        int lastDot = token.lastIndexOf('.');
        char first = token.charAt(lastDot + 1);
        char replacement = (first == 'a') ? 'b' : 'a';
        return token.substring(0, lastDot + 1) + replacement + token.substring(lastDot + 2);
    }

    private CategoryResponseDto fasteners() {
        return new CategoryResponseDto(1L, "Fasteners", "Bolts, nuts, screws");
    }

    @Test
    @DisplayName("a valid CLERK token reaches a protected read (200)")
    void validTokenReachesProtectedRead() throws Exception {
        Pageable pageable = PageRequest.of(0, 20);
        given(categoryService.getAll(any(Pageable.class))).willReturn(
                PagedResponse.from(new PageImpl<>(List.of(fasteners()), pageable, 1), List.of(fasteners())));

        mockMvc.perform(get("/api/categories").header("Authorization", "Bearer " + clerkToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Fasteners"));
    }

    @Test
    @DisplayName("a valid ADMIN token reaches a protected write (201): the roles claim drives hasRole")
    void adminTokenReachesProtectedWrite() throws Exception {
        given(categoryService.create(any(CategoryRequestDto.class))).willReturn(fasteners());

        mockMvc.perform(post("/api/categories").contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + adminToken())
                .content(json(new CategoryRequestDto("Fasteners", "Bolts, nuts, screws"))))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/categories/1")))
                .andExpect(jsonPath("$.name").value("Fasteners"));
    }

    @Test
    @DisplayName("a valid CLERK token is refused a catalogue write (403 from the URL rule)")
    void clerkTokenRefusedProtectedWrite() throws Exception {
        mockMvc.perform(post("/api/categories").contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + clerkToken())
                .content(json(new CategoryRequestDto("Fasteners", null))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Access denied"));
    }

    @Test
    @DisplayName("missing token is rejected (401)")
    void missingTokenRejected() throws Exception {
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("tampered token is rejected (401)")
    void tamperedTokenRejected() throws Exception {
        mockMvc.perform(get("/api/categories").header("Authorization", "Bearer " + tamper(clerkToken())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("expired token is rejected (401)")
    void expiredTokenRejected() throws Exception {
        mockMvc.perform(get("/api/categories").header("Authorization", "Bearer " + expiredToken()))
                .andExpect(status().isUnauthorized());
    }
}
