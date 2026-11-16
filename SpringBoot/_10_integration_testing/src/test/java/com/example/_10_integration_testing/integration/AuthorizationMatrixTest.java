package com.example._10_integration_testing.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.micrometer.observation.autoconfigure.ObservationProperties.Http;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;

import com.example._10_integration_testing.dto.CategoryRequestDto;
import com.example._10_integration_testing.dto.CategoryResponseDto;
import com.example._10_integration_testing.dto.LoginRequest;
import com.example._10_integration_testing.dto.ProductResponseDto;
import com.example._10_integration_testing.dto.RegisterRequest;
import com.example._10_integration_testing.dto.UserResponse;
import com.example._10_integration_testing.integration.support.RequestMother;
import com.example._10_integration_testing.model.Role;

/**
 * Lab 10 — the authorization matrix of Lab 9 §8.2, proven end to end over a
 * real socket.
 *
 * <p>
 * Until now each layer was tested alone: the JWT filter in a unit test, the URL
 * rules in
 * {@code @WebMvcTest} slices with {@code @WithMockUser}, the
 * {@code @PreAuthorize} rules in
 * {@code MethodSecurityTest}. This class sends real tokens through the real
 * chain and checks the
 * status a client would actually see — 401 from the entry point, 403 from the
 * URL rules
 * ({@code RestAccessDeniedHandler}) or from method security
 * ({@code GlobalExceptionHandler}), both
 * rendered with the same {@code "Access denied"} body, and 2xx where the role
 * allows.
 */
@Sql(scripts = "/sql/cleanup-catalogue.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class AuthorizationMatrixTest extends AbstractRestIntegrationTest {

    private static final ParameterizedTypeReference<List<UserResponse>> USER_LIST = new ParameterizedTypeReference<>() {
    };

    // ---- public endpoints
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("anonymous: GET /actuator/health is open (200 UP)")
    void healthIsPublic() {
        client.get().uri("/actuator/health").exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.status").isEqualTo("UP");
    }

    @Test
    @DisplayName("anonymous: POST /api/auth/register is open and creates a CLERK (201)")
    void registerIsPublic() {
        String username = "matrix." + System.nanoTime();
        client.post().uri("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new RegisterRequest(username, username + "@stockflow.local", "matrix12345"))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(UserResponse.class)
                .value(user -> {
                    assertThat(user.getUsername()).isEqualTo(username);
                    assertThat(user.getRoles()).containsExactly(Role.CLERK);
                });
    }

    @Test
    @DisplayName("anonymous: a wrong password is 401 'Authentication failed' (rendered by the handler)")
    void wrongPasswordIs401() {
        client.post().uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new LoginRequest(ADMIN_USERNAME, "not-the-password"))
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.title").isEqualTo("Authentication failed")
                .jsonPath("$.status").isEqualTo(401);
    }

    // ---- no or bad credentials on protected endpoints
    // -------------------------------------------

    @Test
    @DisplayName("anonymous: a protected read is 401 'Unauthorized' (rendered by the entry point)")
    void anonymousReadIs401() {
        client.get().uri("/api/products").exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .jsonPath("$.title").isEqualTo("Unauthorized")
                .jsonPath("$.detail").value(detail -> assertThat((String) detail).contains("Bearer"));
    }

    @Test
    @DisplayName("a token whose signature was tampered with is 401")
    void tamperedTokenIs401() {
        String token = loginAs(ADMIN_USERNAME, ADMIN_PASSWORD);
        int signatureStart = token.lastIndexOf('.') + 1;
        char first = token.charAt(signatureStart);
        char flipped = first == 'A' ? 'B' : 'A';
        String tampered = token.substring(0, signatureStart) + flipped + token.substring(signatureStart + 1);

        client.get().uri("/api/products")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tampered)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    // ---- CLERK
    // --------------------------------------------------------------------------------

    @Test
    @DisplayName("clerk: can read the catalogue (200)")
    void clerkCanRead() {
        client.get().uri("/api/products")
                .header(HttpHeaders.AUTHORIZATION, clerkBearer())
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.content").isArray();
    }

    @Test
    @DisplayName("clerk: POST /api/categories is 403 'Access denied' (URL rule, before the controller)")
    void clerkCannotCreateCategory() {
        client.post().uri("/api/categories")
                .header(HttpHeaders.AUTHORIZATION, clerkBearer())
                .contentType(MediaType.APPLICATION_JSON)
                .body(RequestMother.category().build())
                .exchange()
                .expectStatus().isForbidden()
                .expectBody()
                .jsonPath("$.title").isEqualTo("Access denied")
                .jsonPath("$.status").isEqualTo(403);
    }

    @Test
    @DisplayName("clerk: PUT /api/products/{id} is 403 even for an id that does not exist")
    void clerkCannotUpdateProduct() {
        client.put().uri("/api/products/{id}", 999999)
                .header(HttpHeaders.AUTHORIZATION, clerkBearer())
                .contentType(MediaType.APPLICATION_JSON)
                .body(RequestMother.product(1L).build())
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("clerk: DELETE /api/categories/{id} is 403")
    void clerkCannotDeleteCategory() {
        client.delete().uri("/api/categories/999999")
                .header(HttpHeaders.AUTHORIZATION, clerkBearer())
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("clerk: can record a movement on an admin-created product (201, recordedBy = clerk)")
    void clerkCanRecordMovement() {
        CategoryResponseDto category = createCategoryAsAdmin(RequestMother.category().build());
        ProductResponseDto product = createProductAsAdmin(RequestMother.product(category.getId()).build());

        client.post().uri("/api/products/{id}/movements", product.getId())
                .header(HttpHeaders.AUTHORIZATION, clerkBearer())
                .contentType(MediaType.APPLICATION_JSON)
                .body(RequestMother.movementIn(12).build())
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.quantityAfter").isEqualTo(12)
                .jsonPath("$.recordedBy.username").isEqualTo(CLERK_USERNAME);
    }

    @Test
    @DisplayName("clerk: GET /api/admin/users is 403")
    void clerkCannotListUsers() {
        client.get().uri("/api/admin/users")
                .header(HttpHeaders.AUTHORIZATION, clerkBearer())
                .exchange()
                .expectStatus().isForbidden()
                .expectBody().jsonPath("$.title").isEqualTo("Access denied");
    }

    @Test
    @DisplayName("clerk: GET /api/users/me returns the clerk's own profile")
    void clerkSeesOwnProfileAtMe() {
        client.get().uri("/api/users/me")
                .header(HttpHeaders.AUTHORIZATION, clerkBearer())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.username").isEqualTo(CLERK_USERNAME)
                .jsonPath("$.roles[0]").isEqualTo("CLERK");

    }

    @Test
    @DisplayName("clerk: GET /api/users/{own username} is 200 (method security: #username == authentication.name)")
    void clerkCanReadOwnProfileByUsername() {
        client.get().uri("/api/users/{username}", CLERK_USERNAME)
                .header(HttpHeaders.AUTHORIZATION, clerkBearer())
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.username").isEqualTo(CLERK_USERNAME);
    }

    @Test
    @DisplayName("clerk: GET /api/users/admin is 403 'Access denied' (method security, rendered by the handler)")
    void clerkCannotReadAnotherProfile() {
        client.get().uri("/api/users/admin")
                .header(HttpHeaders.AUTHORIZATION, clerkBearer())
                .exchange()
                .expectStatus().isForbidden()
                .expectBody()
                .jsonPath("$.title").isEqualTo("Access denied")
                .jsonPath("$.status").isEqualTo(403);
    }

    // ---- ADMIN
    // --------------------------------------------------------------------------------
    @Test
    @DisplayName("admin: GET /api/admin/users lists every user, including the clerk")
    void adminCanListUsers() {
        List<UserResponse> users = client.get().uri("/api/admin/users")
                .header(HttpHeaders.AUTHORIZATION, adminBearer())
                .exchange()
                .expectStatus().isOk()
                .expectBody(USER_LIST)
                .returnResult().getResponseBody();

        assertThat(users).extracting(UserResponse::getUsername).contains(ADMIN_USERNAME, CLERK_USERNAME);
    }

    @Test
    @DisplayName("admin: GET /api/users/{any username} is 200")
    void adminCanReadAnyProfile() {
        client.get().uri("/api/users/{username}", CLERK_USERNAME)
                .header(HttpHeaders.AUTHORIZATION, adminBearer())
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.username").isEqualTo(CLERK_USERNAME);
    }

    @Test
    @DisplayName("admin: can create and delete a category (201 then 204)")
    void adminCanWriteTheCatalogue() {
        CategoryResponseDto category = createCategoryAsAdmin(RequestMother.category().build());
        client.delete().uri("/api/categories/{id}", category.getId())
                .header(HttpHeaders.AUTHORIZATION, adminBearer())
                .exchange()
                .expectStatus().isNoContent();
    }

}
