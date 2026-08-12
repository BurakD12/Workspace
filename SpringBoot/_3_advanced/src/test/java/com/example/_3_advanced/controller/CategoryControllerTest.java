package com.example._3_advanced.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example._3_advanced.dto.CategoryRequestDto;
import com.example._3_advanced.dto.CategoryResponseDto;
import com.example._3_advanced.exception.DuplicateResourceException;
import com.example._3_advanced.exception.ResourceNotFoundException;
import com.example._3_advanced.service.CategoryService;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(CategoryController.class)
@DisplayName("CategoryController")
class CategoryControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockitoBean private CategoryService categoryService;

    @Test
    @DisplayName("POST 201 + Location contains /api/categories/1")
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
    @DisplayName("POST 400 when name is blank")
    void createInvalid() throws Exception {
        mockMvc.perform(post("/api/categories").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new CategoryRequestDto(""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").value("Name is required"));
    }

    @Test
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
    @DisplayName("DELETE /api/categories/1 204")
    void deleteOk() throws Exception {
        mockMvc.perform(delete("/api/categories/1"))
                .andExpect(status().isNoContent());
    }
}
