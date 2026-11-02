package com.example._10_integration_testing.controller;

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

import com.example._10_integration_testing.dto.CategoryRequestDto;
import com.example._10_integration_testing.dto.CategoryResponseDto;
import com.example._10_integration_testing.dto.PagedResponse;
import com.example._10_integration_testing.exception.DuplicateResourceException;
import com.example._10_integration_testing.exception.ResourceInUseException;
import com.example._10_integration_testing.exception.ResourceNotFoundException;
import com.example._10_integration_testing.security.SecurityConfig;
import com.example._10_integration_testing.service.CategoryService;
import tools.jackson.databind.ObjectMapper;

/**
 * Lab 9 (StockFlow) — the web contract of CategoryController: status codes,
 * JSON, Location, the URL
 * layer's 401/403, and the rendering of each 409. The service is a mock of the
 * interface, so the
 * {@code @PreAuthorize} rules on the implementation are NOT exercised here (see
 * MethodSecurityTest).
 */
@WebMvcTest(CategoryController.class)
@Import(SecurityConfig.class)
@DisplayName("CategoryController")
class CategoryControllerTest {

    @Autowired
    private WebApplicationContext context;
    @Autowired
    private ObjectMapper objectMapper;
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

    private CategoryResponseDto fasteners() {
        return new CategoryResponseDto(1L, "Fasteners", "Bolts, nuts, screws");
    }

    @Test
    @DisplayName("GET 401 when anonymous")
    void anonymousUnauthorized() throws Exception {
        mockMvc.perform(get("/api/categories")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST 201 + Location as ADMIN")
    void createAsAdmin() throws Exception {
        given(categoryService.create(any(CategoryRequestDto.class))).willReturn(fasteners());

        mockMvc.perform(post("/api/categories").contentType(MediaType.APPLICATION_JSON)
                .content(json(new CategoryRequestDto("Fasteners", "Bolts, nuts, screws"))))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/categories/1")))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Fasteners"));
    }

    @Test
    @WithMockUser(roles = "CLERK")
    @DisplayName("POST 403 as CLERK — a catalogue write is ADMIN-only (URL rule)")
    void createAsClerkForbidden() throws Exception {
        mockMvc.perform(post("/api/categories").contentType(MediaType.APPLICATION_JSON)
                .content(json(new CategoryRequestDto("Fasteners", null))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Access denied"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST 400 when the name is blank")
    void createBlankName() throws Exception {
        mockMvc.perform(post("/api/categories").contentType(MediaType.APPLICATION_JSON)
                .content(json(new CategoryRequestDto("  ", null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.errors.name").value("Name is required"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST 409 on a duplicate name")
    void createDuplicate() throws Exception {
        given(categoryService.create(any(CategoryRequestDto.class)))
                .willThrow(new DuplicateResourceException("category name", "Fasteners"));

        mockMvc.perform(post("/api/categories").contentType(MediaType.APPLICATION_JSON)
                .content(json(new CategoryRequestDto("Fasteners", null))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Duplicate value"));
    }

    @Test
    @WithMockUser(roles = "CLERK")
    @DisplayName("GET 200 paged as CLERK — reads are open to any authenticated user")
    void listAsClerk() throws Exception {
        Pageable pageable = PageRequest.of(0, 20);
        given(categoryService.getAll(any(Pageable.class))).willReturn(
                PagedResponse.from(new PageImpl<>(List.of(fasteners()), pageable, 1), List.of(fasteners())));

        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Fasteners"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @WithMockUser(roles = "CLERK")
    @DisplayName("GET /{id} 404 for an unknown id")
    void getByIdUnknown() throws Exception {
        given(categoryService.getById(99L)).willThrow(new ResourceNotFoundException("Category", 99L));

        mockMvc.perform(get("/api/categories/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource not found"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("PUT 200 as ADMIN")
    void updateAsAdmin() throws Exception {
        given(categoryService.update(eq(1L), any(CategoryRequestDto.class))).willReturn(fasteners());

        mockMvc.perform(put("/api/categories/1").contentType(MediaType.APPLICATION_JSON)
                .content(json(new CategoryRequestDto("Fasteners", "Bolts, nuts, screws"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @WithMockUser(roles = "CLERK")
    @DisplayName("PUT 403 as CLERK")
    void updateAsClerkForbidden() throws Exception {
        mockMvc.perform(put("/api/categories/1").contentType(MediaType.APPLICATION_JSON)
                .content(json(new CategoryRequestDto("Fasteners", null))))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("DELETE 204 as ADMIN")
    void deleteAsAdmin() throws Exception {
        mockMvc.perform(delete("/api/categories/1")).andExpect(status().isNoContent());
        verify(categoryService).delete(1L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("DELETE 409 when products still reference the category")
    void deleteInUse() throws Exception {
        willThrow(new ResourceInUseException("Category", 1L, "products")).given(categoryService).delete(1L);

        mockMvc.perform(delete("/api/categories/1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Resource in use"))
                .andExpect(jsonPath("$.detail").value(containsString("still referenced by products")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST 400 'Malformed request' for a body that is not JSON (not a 500)")
    void createMalformedBody() throws Exception {
        mockMvc.perform(post("/api/categories").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Malformed request"));
    }
}
