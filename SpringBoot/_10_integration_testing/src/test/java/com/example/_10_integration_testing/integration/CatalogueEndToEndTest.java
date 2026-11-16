package com.example._10_integration_testing.integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.client.EntityExchangeResult;

import com.example._10_integration_testing.dto.CategoryResponseDto;
import com.example._10_integration_testing.dto.ProductResponseDto;
import com.example._10_integration_testing.dto.StockMovementResponseDto;
import com.example._10_integration_testing.integration.support.RequestMother;
import com.example._10_integration_testing.model.MovementType;

/**
 * Lab 10 — the happy path, end to end, over a real socket: JSON in, real Tomcat, real filter chain,
 * real controllers, services, JPA and H2, JSON out. Nothing is mocked.
 *
 * <p>Each test writes through the server's own transactions, which commit. The class-level
 * {@code @Sql(... AFTER_TEST_METHOD)} empties the catalogue tables after every test so the next one
 * starts clean.
 */
@Sql (scripts = "/sql/cleanup-catalogue.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
public class CatalogueEndToEndTest extends AbstractRestIntegrationTest {
    @Test
    @DisplayName("admin creates a category and a product; clerk records a movement; the product reflects it")
    void adminCreatesCatalogueThenClerkRecordsMovement() {
        // 1. ADMIN creates the category
        CategoryResponseDto bearings = createCategoryAsAdmin(RequestMother.category().build());
        assertThat(bearings.getId()).isNotNull();
        assertThat(bearings.getName()).isEqualTo("Bearings");

        // 2. ADMIN creates a product that references it (201 + Location + nested category summary)
        EntityExchangeResult<ProductResponseDto> created = client.post().uri("/api/products")
                .header(HttpHeaders.AUTHORIZATION, adminBearer())
                .contentType(MediaType.APPLICATION_JSON)
                .body(RequestMother.product(bearings.getId()).build())
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().exists(HttpHeaders.LOCATION)
                .expectBody(ProductResponseDto.class)
                .returnResult();

        ProductResponseDto product = created.getResponseBody();
        assertThat(product).isNotNull();
        assertThat(product.getSku()).isEqualTo("BRG-6204");
        assertThat(product.getUnitPrice()).isEqualByComparingTo("4.75");
        assertThat(product.getQuantityOnHand()).isZero();
        assertThat(product.getReorderLevel()).isEqualTo(5);
        assertThat(product.isLowStock()).as("0 on hand <= reorder level 5").isTrue();
        assertThat(product.getCategory().getId()).isEqualTo(bearings.getId());
        assertThat(product.getCategory().getName()).isEqualTo("Bearings");
        assertThat(product.getSupplier()).isNull();
        assertThat(created.getResponseHeaders().getLocation())
                .hasPath("/api/products/" + product.getId());

        // 3. CLERK records goods received (201, no Location — a ledger line is not addressable)
        client.post().uri("/api/products/{id}/movements", product.getId())
                .header(HttpHeaders.AUTHORIZATION, clerkBearer())
                .contentType(MediaType.APPLICATION_JSON)
                .body(RequestMother.movementIn(40).build())
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().doesNotExist(HttpHeaders.LOCATION)
                .expectBody(StockMovementResponseDto.class)
                .value(movement -> {
                    assertThat(movement.getType()).isEqualTo(MovementType.IN);
                    assertThat(movement.getQuantity()).isEqualTo(40);
                    assertThat(movement.getQuantityAfter()).isEqualTo(40);
                    assertThat(movement.getOccurredAt()).isNotNull();
                    assertThat(movement.getProduct().getSku()).isEqualTo("BRG-6204");
                    assertThat(movement.getRecordedBy().getUsername()).isEqualTo(CLERK_USERNAME);
                });

        // 4. CLERK issues 15 (a successful OUT: the balance goes down, never below zero)
        client.post().uri("/api/products/{id}/movements", product.getId())
                .header(HttpHeaders.AUTHORIZATION, clerkBearer())
                .contentType(MediaType.APPLICATION_JSON)
                .body(RequestMother.movementOut(15).build())
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.type").isEqualTo("OUT")
                .jsonPath("$.quantityAfter").isEqualTo(25);

        // 5. the product now reports the new balance, and is no longer low on stock
        client.get().uri("/api/products/{id}", product.getId())
                .header(HttpHeaders.AUTHORIZATION, clerkBearer())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.quantityOnHand").isEqualTo(25)
                .jsonPath("$.lowStock").isEqualTo(false)
                .jsonPath("$.category.name").isEqualTo("Bearings");

        // 6. and the ledger has both lines, newest first (the controller's default sort: occurredAt desc)
        client.get().uri("/api/products/{id}/movements", product.getId())
                .header(HttpHeaders.AUTHORIZATION, clerkBearer())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.totalElements").isEqualTo(2)
                .jsonPath("$.content[0].type").isEqualTo("OUT")
                .jsonPath("$.content[0].quantityAfter").isEqualTo(25)
                .jsonPath("$.content[1].quantityAfter").isEqualTo(40);
    }
}
