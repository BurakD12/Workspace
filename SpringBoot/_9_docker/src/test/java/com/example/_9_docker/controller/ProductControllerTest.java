package com.example._9_docker.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.example._9_docker.dto.CategorySummaryDto;
import com.example._9_docker.dto.PagedResponse;
import com.example._9_docker.dto.ProductRequestDto;
import com.example._9_docker.dto.ProductResponseDto;
import com.example._9_docker.dto.ProductSummaryDto;
import com.example._9_docker.dto.StockMovementRequestDto;
import com.example._9_docker.dto.StockMovementResponseDto;
import com.example._9_docker.dto.SupplierSummaryDto;
import com.example._9_docker.dto.UserSummaryDto;
import com.example._9_docker.exception.InsufficientStockException;
import com.example._9_docker.exception.ResourceInUseException;
import com.example._9_docker.exception.ResourceNotFoundException;
import com.example._9_docker.model.MovementType;
import com.example._9_docker.security.SecurityConfig;
import com.example._9_docker.service.ProductService;
import com.example._9_docker.service.StockMovementService;
import tools.jackson.databind.ObjectMapper;

/**
 * Lab 9 (StockFlow) — the web contract of ProductController, including the nested ledger endpoints.
 * Both services are mocked interfaces, so the service-level {@code @PreAuthorize} rules are not
 * exercised here; the URL-level rules (ADMIN for catalogue writes, any token for movements) ARE.
 */
@WebMvcTest(ProductController.class)
@Import(SecurityConfig.class)
@DisplayName("ProductController")
class ProductControllerTest {

    @Autowired private WebApplicationContext context;
    @Autowired private ObjectMapper objectMapper;
    @MockitoBean private ProductService productService;
    @MockitoBean private StockMovementService stockMovementService;
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

    private ProductResponseDto bolt() {
        return new ProductResponseDto(10L, "BOLT-M8-50", "M8 x 50 bolt", "Zinc plated", new BigDecimal("0.35"),
                250, 100, false, new CategorySummaryDto(1L, "Fasteners"), new SupplierSummaryDto(5L, "Acme Metals"));
    }

    private ProductRequestDto validRequest() {
        return ProductRequestDto.builder().sku("BOLT-M8-50").name("M8 x 50 bolt").unitPrice(new BigDecimal("0.35"))
                .reorderLevel(100).categoryId(1L).supplierId(5L).build();
    }

    private StockMovementResponseDto delivery() {
        return new StockMovementResponseDto(500L, MovementType.IN, 250, "Initial delivery", 250,
                Instant.parse("2026-09-04T12:00:00Z"), new ProductSummaryDto(10L, "BOLT-M8-50", "M8 x 50 bolt"),
                new UserSummaryDto(2L, "bob"));
    }

    // --- catalogue ------------------------------------------------------------------------------

    @Test
    @DisplayName("GET 401 when anonymous")
    void anonymousUnauthorized() throws Exception {
        mockMvc.perform(get("/api/products")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST 201 + Location as ADMIN; the response carries the derived lowStock flag")
    void createAsAdmin() throws Exception {
        given(productService.create(any(ProductRequestDto.class))).willReturn(bolt());

        mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content(json(validRequest())))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/products/10")))
                .andExpect(jsonPath("$.sku").value("BOLT-M8-50"))
                .andExpect(jsonPath("$.unitPrice").value(0.35))
                .andExpect(jsonPath("$.lowStock").value(false))
                .andExpect(jsonPath("$.category.name").value("Fasteners"))
                .andExpect(jsonPath("$.supplier.name").value("Acme Metals"));
    }

    @Test
    @WithMockUser(roles = "CLERK")
    @DisplayName("POST 403 as CLERK — a clerk does not edit the catalogue")
    void createAsClerkForbidden() throws Exception {
        mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content(json(validRequest())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Access denied"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST 400 lists every failing field: blank sku, missing categoryId, 3-decimal price")
    void createValidation() throws Exception {
        ProductRequestDto bad = ProductRequestDto.builder().sku(" ").name("x")
                .unitPrice(new BigDecimal("1.005")).categoryId(null).build();

        mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content(json(bad)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.sku").value("SKU is required"))
                .andExpect(jsonPath("$.errors.categoryId").value("Category id is required"))
                .andExpect(jsonPath("$.errors.unitPrice").value(containsString("2 decimal digits")));
    }

    @Test
    @WithMockUser(roles = "CLERK")
    @DisplayName("GET passes categoryId and lowStock through to the service")
    void listWithFilters() throws Exception {
        Pageable pageable = PageRequest.of(0, 20);
        given(productService.getAll(eq(1L), eq(true), any(Pageable.class))).willReturn(
                PagedResponse.from(new PageImpl<>(List.of(bolt()), pageable, 1), List.of(bolt())));

        mockMvc.perform(get("/api/products").param("categoryId", "1").param("lowStock", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].sku").value("BOLT-M8-50"));
        verify(productService).getAll(eq(1L), eq(true), any(Pageable.class));
    }

    @Test
    @WithMockUser(roles = "CLERK")
    @DisplayName("GET ?lowStock=maybe → 400 Invalid parameter")
    void listBadBoolean() throws Exception {
        mockMvc.perform(get("/api/products").param("lowStock", "maybe"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid parameter"));
    }

    @Test
    @WithMockUser(roles = "CLERK")
    @DisplayName("GET /{id} 200 as CLERK")
    void getById() throws Exception {
        given(productService.getById(10L)).willReturn(bolt());

        mockMvc.perform(get("/api/products/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantityOnHand").value(250));
    }

    @Test
    @WithMockUser(roles = "CLERK")
    @DisplayName("GET /abc → 400 (non-numeric id)")
    void getByIdNonNumeric() throws Exception {
        mockMvc.perform(get("/api/products/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid parameter"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("PUT 200 as ADMIN")
    void updateAsAdmin() throws Exception {
        given(productService.update(eq(10L), any(ProductRequestDto.class))).willReturn(bolt());

        mockMvc.perform(put("/api/products/10").contentType(MediaType.APPLICATION_JSON).content(json(validRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("DELETE 204 as ADMIN")
    void deleteAsAdmin() throws Exception {
        mockMvc.perform(delete("/api/products/10")).andExpect(status().isNoContent());
        verify(productService).delete(10L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("DELETE 409 when the product has ledger history")
    void deleteWithHistory() throws Exception {
        willThrow(new ResourceInUseException("Product", 10L, "stock movements")).given(productService).delete(10L);

        mockMvc.perform(delete("/api/products/10"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Resource in use"));
    }

    // --- the nested ledger ----------------------------------------------------------------------

    @Test
    @WithMockUser(username = "bob", roles = "CLERK")
    @DisplayName("POST /{id}/movements 201 as CLERK — the recorder is the principal; no Location header")
    void recordMovementAsClerk() throws Exception {
        given(stockMovementService.record(eq(10L), any(StockMovementRequestDto.class), eq("bob"))).willReturn(delivery());

        mockMvc.perform(post("/api/products/10/movements").contentType(MediaType.APPLICATION_JSON)
                        .content(json(StockMovementRequestDto.builder().type(MovementType.IN).quantity(250)
                                .reason("Initial delivery").build())))
                .andExpect(status().isCreated())
                .andExpect(header().doesNotExist("Location"))
                .andExpect(jsonPath("$.type").value("IN"))
                .andExpect(jsonPath("$.quantityAfter").value(250))
                .andExpect(jsonPath("$.recordedBy.username").value("bob"))
                .andExpect(jsonPath("$.occurredAt").value("2026-09-04T12:00:00Z"));
    }

    @Test
    @DisplayName("POST /{id}/movements 401 when anonymous")
    void recordMovementAnonymous() throws Exception {
        mockMvc.perform(post("/api/products/10/movements").contentType(MediaType.APPLICATION_JSON)
                        .content(json(StockMovementRequestDto.builder().type(MovementType.IN).quantity(1).build())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "bob", roles = "CLERK")
    @DisplayName("POST /{id}/movements 400 when quantity is zero or type missing")
    void recordMovementValidation() throws Exception {
        mockMvc.perform(post("/api/products/10/movements").contentType(MediaType.APPLICATION_JSON)
                        .content(json(StockMovementRequestDto.builder().type(null).quantity(0).build())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.type").value("Type is required"))
                .andExpect(jsonPath("$.errors.quantity").value("Quantity must be greater than zero"));
    }

    @Test
    @WithMockUser(username = "bob", roles = "CLERK")
    @DisplayName("POST /{id}/movements 409 when OUT exceeds the stock on hand")
    void recordMovementInsufficient() throws Exception {
        given(stockMovementService.record(eq(10L), any(StockMovementRequestDto.class), eq("bob")))
                .willThrow(new InsufficientStockException("BOLT-M8-50", 250, 300));

        mockMvc.perform(post("/api/products/10/movements").contentType(MediaType.APPLICATION_JSON)
                        .content(json(StockMovementRequestDto.builder().type(MovementType.OUT).quantity(300).build())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Insufficient stock"))
                .andExpect(jsonPath("$.detail").value("Insufficient stock for BOLT-M8-50: 250 on hand, 300 requested"));
    }

    @Test
    @WithMockUser(roles = "CLERK")
    @DisplayName("GET /{id}/movements 200 paged as CLERK")
    void ledger() throws Exception {
        Pageable pageable = PageRequest.of(0, 20);
        given(stockMovementService.getByProduct(eq(10L), any(Pageable.class))).willReturn(
                PagedResponse.from(new PageImpl<>(List.of(delivery()), pageable, 1), List.of(delivery())));

        mockMvc.perform(get("/api/products/10/movements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].quantityAfter").value(250))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @WithMockUser(roles = "CLERK")
    @DisplayName("GET /{id}/movements 404 for an unknown product")
    void ledgerUnknownProduct() throws Exception {
        given(stockMovementService.getByProduct(eq(99L), any(Pageable.class)))
                .willThrow(new ResourceNotFoundException("Product", 99L));

        mockMvc.perform(get("/api/products/99/movements")).andExpect(status().isNotFound());
    }
}
