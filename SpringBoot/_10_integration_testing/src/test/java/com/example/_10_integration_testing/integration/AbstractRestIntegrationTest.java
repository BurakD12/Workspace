package com.example._10_integration_testing.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

import com.example._10_integration_testing.dto.CategoryRequestDto;
import com.example._10_integration_testing.dto.CategoryResponseDto;
import com.example._10_integration_testing.dto.LoginRequest;
import com.example._10_integration_testing.dto.ProductRequestDto;
import com.example._10_integration_testing.dto.ProductResponseDto;
import com.example._10_integration_testing.dto.RegisterRequest;
import com.example._10_integration_testing.dto.TokenResponse;

/**
 * Lab 10 — base class for every <em>real-socket</em> integration test.
 *
 * <p>{@code @SpringBootTest(webEnvironment = RANDOM_PORT)} boots the complete application — every
 * bean, the real {@code SecurityFilterChain}, JPA against an embedded H2 database (the test
 * {@code application.properties} replaces main's, so there is no PostgreSQL URL and Boot
 * auto-configures H2) — and starts an embedded Tomcat on a free port. {@code @AutoConfigureRestTestClient}
 * then registers a {@link RestTestClient} already bound to that port: Spring Boot 4 registers <b>no</b>
 * HTTP test client unless you ask for one.
 *
 * <p>Every request to a real socket crosses the real JWT filter, so every subclass needs tokens.
 * {@link #loginAs(String, String)} obtains one through the real {@code POST /api/auth/login} and caches
 * it per username for the life of the JVM — a BCrypt login costs ~100 ms and the token lives 15 minutes,
 * far longer than the suite.
 *
 * <p>All subclasses share <b>one</b> cached application context (same annotations, no
 * {@code @MockitoBean}, no property overrides), so the whole real-socket suite boots exactly once.
 */
@SpringBootTest (webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient 
public abstract class AbstractRestIntegrationTest {

    /** Seeded by {@code AdminUserSeeder} at startup (main code, Lab 9). */
    public static final String ADMIN_USERNAME = "admin";
    public static final String ADMIN_PASSWORD = "admin12345";

    /** Registered by {@link #ensureClerkExists()} through the public register endpoint. */
    public static final String CLERK_USERNAME = "clerk.it";
    public static final String CLERK_PASSWORD = "clerk12345";

    private static final Map<String, String> ACCESS_TOKENS = new ConcurrentHashMap<>();

    @Autowired
    protected RestTestClient client;

    /**
     * The suite needs a CLERK, and the only way to get one is the API itself. 201 the first time,
     * 409 ("username already taken") on every later test — both mean "the clerk exists".
     */
    @BeforeEach 
    void ensureClerkExists() {
        client.post().uri("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new RegisterRequest(CLERK_USERNAME, "clerk.it@stockflow.local", CLERK_PASSWORD))
                .exchange()
                .expectStatus().value(status -> assertThat(status).isIn(201, 409));
    }

    /** Logs in through the real endpoint and returns the raw access token (cached per username). */
    protected String loginAs(String username, String password) {
        return ACCESS_TOKENS.computeIfAbsent(username, u -> {
            TokenResponse tokens = client.post().uri("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new LoginRequest(u, password))
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(TokenResponse.class)
                    .returnResult().getResponseBody();

            assertThat(tokens).isNotNull();
            assertThat(tokens.getTokenType()).isEqualTo("Bearer");
            return tokens.getAccessToken();
        });
    }

    protected String adminBearer() {
        return "Bearer " + loginAs(ADMIN_USERNAME, ADMIN_PASSWORD);
    }

    protected String clerkBearer() {
        return "Bearer " + loginAs(CLERK_USERNAME, CLERK_PASSWORD);
    }

    protected CategoryResponseDto createCategoryAsAdmin(CategoryRequestDto request) {
        CategoryResponseDto category = client.post().uri("/api/categories")
                .header(HttpHeaders.AUTHORIZATION, adminBearer())
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(CategoryResponseDto.class)
                .returnResult().getResponseBody();
        assertThat(category).isNotNull();
        return category;
    }

    protected ProductResponseDto createProductAsAdmin(ProductRequestDto request) {
        ProductResponseDto product = client.post().uri("/api/products")
                .header(HttpHeaders.AUTHORIZATION, adminBearer())
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(ProductResponseDto.class)
                .returnResult().getResponseBody();
        assertThat(product).isNotNull();
        return product;
    }
}
