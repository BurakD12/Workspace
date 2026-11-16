package com.example._10_integration_testing.integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.jdbc.Sql;

import com.example._10_integration_testing.dto.PagedResponse;
import com.example._10_integration_testing.dto.ProductResponseDto;

/**
 * Lab 10 — paging, sorting and filtering asserted against a <em>known</em> catalogue seeded with
 * {@code @Sql}.
 *
 * <p>Both class-level scripts run around <b>every</b> test method: the seed before (default
 * {@code BEFORE_TEST_METHOD}), the cleanup after. Each test therefore sees exactly the five products
 * of {@code seed-catalogue.sql} and nothing else, whatever ran before it. The scripts execute on the
 * test thread in their own committed transaction, so the server thread sees the rows.
 *
 * <p>Reads are open to any authenticated user, so the clerk token is used throughout.
 */
@Sql(scripts = "/sql/seed-catalogue.sql")
@Sql(scripts = "/sql/cleanup-catalogue.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class ProductListingTest extends AbstractRestIntegrationTest {

    private static final ParameterizedTypeReference<PagedResponse<ProductResponseDto>> PAGE_OF_PRODUCTS =
            new ParameterizedTypeReference<>() {
            };

    @Test
    @DisplayName("the default listing is sorted by SKU, 20 per page")
    void defaultListingIsSortedBySku() {
        PagedResponse<ProductResponseDto> page = list("/api/products");

        assertThat(page.getContent()).extracting(ProductResponseDto::getSku)
                .containsExactly("SKU-001", "SKU-002", "SKU-003", "SKU-004", "SKU-005");
        assertThat(page.getPage()).isZero();
        assertThat(page.getSize()).isEqualTo(20);
        assertThat(page.getTotalElements()).isEqualTo(5);
        assertThat(page.getTotalPages()).isEqualTo(1);
        assertThat(page.isLast()).isTrue();
    }

    @Test
    @DisplayName("page and size select a window and report the page metadata")
    void secondPageOfTwo() {
        PagedResponse<ProductResponseDto> page = list("/api/products?page=1&size=2");

        assertThat(page.getContent()).extracting(ProductResponseDto::getSku)
                .containsExactly("SKU-003", "SKU-004");
        assertThat(page.getPage()).isEqualTo(1);
        assertThat(page.getSize()).isEqualTo(2);
        assertThat(page.getTotalElements()).isEqualTo(5);
        assertThat(page.getTotalPages()).isEqualTo(3);
        assertThat(page.isLast()).isFalse();
    }

    @Test
    @DisplayName("the last page holds the remainder and reports last=true")
    void lastPageReportsLast() {
        PagedResponse<ProductResponseDto> page = list("/api/products?page=2&size=2");

        assertThat(page.getContent()).extracting(ProductResponseDto::getSku).containsExactly("SKU-005");
        assertThat(page.isLast()).isTrue();
    }

    @Test
    @DisplayName("sort=unitPrice,desc overrides the default order")
    void sortParameterOverridesDefault() {
        PagedResponse<ProductResponseDto> page = list("/api/products?sort=unitPrice,desc");

        assertThat(page.getContent()).extracting(ProductResponseDto::getSku)
                .containsExactly("SKU-004", "SKU-003", "SKU-001", "SKU-002", "SKU-005");
    }

    @Test
    @DisplayName("lowStock=true returns only products at or below their reorder level")
    void lowStockFilter() {
        PagedResponse<ProductResponseDto> page = list("/api/products?lowStock=true");

        assertThat(page.getContent()).extracting(ProductResponseDto::getSku)
                .containsExactly("SKU-002", "SKU-003", "SKU-005");
        assertThat(page.getContent()).allSatisfy(product -> assertThat(product.isLowStock()).isTrue());
        assertThat(page.getTotalElements()).isEqualTo(3);
    }

    @Test
    @DisplayName("categoryId and lowStock combine")
    void categoryAndLowStockFiltersCombine() {
        PagedResponse<ProductResponseDto> page = list("/api/products?categoryId=9001&lowStock=true");

        assertThat(page.getContent()).extracting(ProductResponseDto::getSku)
                .containsExactly("SKU-002", "SKU-005");
        assertThat(page.getContent()).extracting(product -> product.getCategory().getName())
                .containsOnly("Fasteners");
    }

    @Test
    @DisplayName("seeded rows come back with their nested category and (optional) supplier")
    void nestedSummariesOfSeededRows() {
        client.get().uri("/api/products/9001")
                .header(HttpHeaders.AUTHORIZATION, clerkBearer())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.sku").isEqualTo("SKU-001")
                .jsonPath("$.category.name").isEqualTo("Fasteners")
                .jsonPath("$.supplier.name").isEqualTo("Acme Industrial")
                .jsonPath("$.lowStock").isEqualTo(false);

        client.get().uri("/api/products/9002")
                .header(HttpHeaders.AUTHORIZATION, clerkBearer())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.supplier").isEmpty()
                .jsonPath("$.lowStock").isEqualTo(true);
    }

    private PagedResponse<ProductResponseDto> list(String uri) {
        PagedResponse<ProductResponseDto> page = client.get().uri(uri)
                .header(HttpHeaders.AUTHORIZATION, clerkBearer())
                .exchange()
                .expectStatus().isOk()
                .expectBody(PAGE_OF_PRODUCTS)
                .returnResult().getResponseBody();
        assertThat(page).isNotNull();
        return page;
    }
}
