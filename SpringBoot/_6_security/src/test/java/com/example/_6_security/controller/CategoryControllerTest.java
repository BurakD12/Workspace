package com.example._6_security.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.example._6_security.dto.CategoryRequestDto;
import com.example._6_security.dto.CategoryResponseDto;
import com.example._6_security.exception.DuplicateResourceException;
import com.example._6_security.exception.ResourceNotFoundException;
import com.example._6_security.security.SecurityConfig;
import com.example._6_security.service.CategoryService;
import tools.jackson.databind.ObjectMapper;

// Lab 6: category mutations require ROLE_ADMIN; reads stay public. SecurityConfig is imported
// so the slice enforces the real rules, and UserDetailsService is mocked for the auth manager.
@WebMvcTest(CategoryController.class)
@Import(SecurityConfig.class)
@DisplayName("CategoryController")
class CategoryControllerTest {

    @Autowired private WebApplicationContext context;
    @Autowired private ObjectMapper objectMapper;
    @MockitoBean private CategoryService categoryService;
    @MockitoBean private UserDetailsService userDetailsService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUpMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST 201 + Location contains /api/categories/1 (ADMIN)")
    void create() throws Exception {
        given(categoryService.createCategory(any(CategoryRequestDto.class)))
                .willReturn(new CategoryResponseDto(1L, "Software"));
        mockMvc.perform(post("/api/categories").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new CategoryRequestDto("Software"))))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/categories/1")))
                .andExpect(jsonPath("$.name").value("Software"));
    }

    @Test
    @DisplayName("POST 401 when anonymous")
    void createAnonymousUnauthorized() throws Exception {
        mockMvc.perform(post("/api/categories").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new CategoryRequestDto("Software"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("POST 403 when a plain USER attempts a catalog write")
    void createForbiddenForUser() throws Exception {
        mockMvc.perform(post("/api/categories").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new CategoryRequestDto("Software"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST 400 when name is blank")
    void createInvalid() throws Exception {
        mockMvc.perform(post("/api/categories").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new CategoryRequestDto(""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").value("Name is required"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST 409 when name already exists")
    void createDuplicate() throws Exception {
        given(categoryService.createCategory(any(CategoryRequestDto.class)))
                .willThrow(new DuplicateResourceException("name", "Software"));
        mockMvc.perform(post("/api/categories").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new CategoryRequestDto("Software"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Duplicate value"));
    }

    @Test
    @DisplayName("GET /api/categories/9999 404 when missing")
    void getByIdMissing() throws Exception {
        given(categoryService.getCategoryById(9999L)).willThrow(new ResourceNotFoundException("Category", 9999L));
        mockMvc.perform(get("/api/categories/9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource not found"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("DELETE /api/categories/1 204")
    void deleteOk() throws Exception {
        mockMvc.perform(delete("/api/categories/1"))
                .andExpect(status().isNoContent());
    }
}
