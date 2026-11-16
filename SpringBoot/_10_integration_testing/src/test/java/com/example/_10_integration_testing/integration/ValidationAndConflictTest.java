package com.example._10_integration_testing.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.example._10_integration_testing.integration.support.RequestMother;

/**
 * Lab 10 — the error contract, asserted on the full context with {@code MockMvcTester} + AssertJ.
 *
 * <p>These are <em>integration</em> tests (real validator, real services, real repositories, real
 * {@code GlobalExceptionHandler}) but they do not need a socket: what we assert is the RFC 7807 body
 * the whole stack produces, and a mock servlet request shows it just as faithfully as Tomcat would,
 * faster. {@code MockMvcTester} gives us one {@code assertThat(result)} entry point with fluent
 * status/header/JSON assertions instead of MockMvc's {@code andExpect(...)} matchers.
 */
@Sql(scripts = "/sql/cleanup-catalogue.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class ValidationAndConflictTest extends AbstractMockMvcIntegrationTest {

    @Test
    @DisplayName("400 Validation failed: a blank category name reports the field error")
    void blankCategoryNameIs400() {
        MvcTestResult result = mvc.post().uri("/api/categories")
                .header(HttpHeaders.AUTHORIZATION, adminBearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(RequestMother.category().name("   ").build()))
                .exchange();

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST)
                .hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson()
                .extractingPath("$.title").isEqualTo("Validation failed");
        assertThat(result).bodyJson().extractingPath("$.status").asNumber().isEqualTo(400);
        assertThat(result).bodyJson().extractingPath("$.errors.name").isEqualTo("Name is required");
    }

    @Test
    @DisplayName("400 Validation failed: every invalid product field is reported at once")
    void invalidProductReportsAllFieldErrors() {
        MvcTestResult result = mvc.post().uri("/api/products")
                .header(HttpHeaders.AUTHORIZATION, adminBearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(RequestMother.product(null)
                        .sku("")
                        .unitPrice(new BigDecimal("-1"))
                        .reorderLevel(-3)
                        .build()))
                .exchange();

        assertThat(result).hasStatus(400)
                .bodyJson()
                .extractingPath("$.errors").asMap()
                .containsEntry("sku", "SKU is required")
                .containsEntry("unitPrice", "Unit price cannot be negative")
                .containsEntry("reorderLevel", "Reorder level cannot be negative")
                .containsEntry("categoryId", "Category id is required")
                .hasSize(4);
    }

    @Test
    @DisplayName("400 Malformed request: a body that is not JSON never reaches validation")
    void malformedJsonIs400() {
        MvcTestResult result = mvc.post().uri("/api/categories")
                .header(HttpHeaders.AUTHORIZATION, adminBearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": ")
                .exchange();

        assertThat(result).hasStatus(400)
                .bodyJson()
                .extractingPath("$.title").isEqualTo("Malformed request");
        assertThat(result).bodyJson().doesNotHavePath("$.errors");
    }

    @Test
    @DisplayName("409 Duplicate value: the same category name twice")
    void duplicateCategoryNameIs409() {
        String bearings = json(RequestMother.category().build());
        assertThat(postAsAdmin("/api/categories", bearings)).hasStatus(HttpStatus.CREATED);

        MvcTestResult second = postAsAdmin("/api/categories", bearings);

        assertThat(second).hasStatus(HttpStatus.CONFLICT)
                .bodyJson()
                .extractingPath("$.title").isEqualTo("Duplicate value");
        assertThat(second).bodyJson().extractingPath("$.detail").asString().contains("Bearings");
    }

    @Test
    @DisplayName("409 Resource in use: a category with products cannot be deleted, and survives")
    void deletingCategoryWithProductsIs409() {
        long categoryId = idOf(postAsAdmin("/api/categories", json(RequestMother.category().build())));
        assertThat(postAsAdmin("/api/products", json(RequestMother.product(categoryId).build())))
                .hasStatus(HttpStatus.CREATED);

        MvcTestResult delete = mvc.delete().uri("/api/categories/{id}", categoryId)
                .header(HttpHeaders.AUTHORIZATION, adminBearer())
                .exchange();

        assertThat(delete).hasStatus(HttpStatus.CONFLICT)
                .bodyJson()
                .extractingPath("$.title").isEqualTo("Resource in use");
        assertThat(delete).bodyJson().extractingPath("$.detail").asString().contains("products");

        // the refusal happened in the service, before any SQL: the category is intact
        assertThat(mvc.get().uri("/api/categories/{id}", categoryId)
                .header(HttpHeaders.AUTHORIZATION, adminBearer())
                .exchange())
                .hasStatusOk()
                .bodyJson().extractingPath("$.name").isEqualTo("Bearings");
    }

    @Test
    @DisplayName("409 Insufficient stock: an OUT larger than the balance is refused and the balance is unchanged")
    void outLargerThanStockIs409AndStockUnchanged() {
        long categoryId = idOf(postAsAdmin("/api/categories", json(RequestMother.category().build())));
        long productId = idOf(postAsAdmin("/api/products", json(RequestMother.product(categoryId).build())));
        assertThat(postAsAdmin("/api/products/" + productId + "/movements", json(RequestMother.movementIn(10).build())))
                .hasStatus(HttpStatus.CREATED);

        MvcTestResult out = postAsAdmin("/api/products/" + productId + "/movements",
                json(RequestMother.movementOut(25).build()));

        assertThat(out).hasStatus(HttpStatus.CONFLICT)
                .bodyJson()
                .extractingPath("$.title").isEqualTo("Insufficient stock");

        assertThat(mvc.get().uri("/api/products/{id}", productId)
                .header(HttpHeaders.AUTHORIZATION, adminBearer())
                .exchange())
                .hasStatusOk()
                .bodyJson().extractingPath("$.quantityOnHand").asNumber().isEqualTo(10);
    }

    @Test
    @DisplayName("404 Resource not found: a movement on an unknown product")
    void movementOnUnknownProductIs404() {
        MvcTestResult result = postAsAdmin("/api/products/999999/movements",
                json(RequestMother.movementIn(1).build()));

        assertThat(result).hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson()
                .extractingPath("$.title").isEqualTo("Resource not found");
    }

    private MvcTestResult postAsAdmin(String uri, String jsonBody) {
        return mvc.post().uri(uri)
                .header(HttpHeaders.AUTHORIZATION, adminBearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonBody)
                .exchange();
    }
}
