package com.example._3_advanced.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example._3_advanced.dto.BookResponseDto;
import com.example._3_advanced.dto.PagedResponse;
import com.example._3_advanced.dto.PublisherRequestDto;
import com.example._3_advanced.dto.PublisherResponseDto;
import com.example._3_advanced.exception.DuplicateResourceException;
import com.example._3_advanced.exception.RelationshipConflictException;
import com.example._3_advanced.service.BookService;
import com.example._3_advanced.service.PublisherService;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(PublisherController.class)
@DisplayName("PublisherController")
class PublisherControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockitoBean private PublisherService publisherService;
    @MockitoBean private BookService bookService;

    @Test
    @DisplayName("POST 201 + Location contains /api/publishers/1")
    void create() throws Exception {
        given(publisherService.createPublisher(any(PublisherRequestDto.class)))
                .willReturn(new PublisherResponseDto(1L, "Prentice Hall", "USA"));
        mockMvc.perform(post("/api/publishers").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new PublisherRequestDto("Prentice Hall", "USA"))))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/publishers/1")))
                .andExpect(jsonPath("$.name").value("Prentice Hall"));
    }

    @Test
    @DisplayName("POST 400 when name is blank")
    void createInvalid() throws Exception {
        mockMvc.perform(post("/api/publishers").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new PublisherRequestDto("", "USA"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").value("Name is required"));
    }

    @Test
    @DisplayName("POST 409 when name already exists")
    void createDuplicate() throws Exception {
        given(publisherService.createPublisher(any(PublisherRequestDto.class)))
                .willThrow(new DuplicateResourceException("name", "Prentice Hall"));
        mockMvc.perform(post("/api/publishers").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new PublisherRequestDto("Prentice Hall", "USA"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Duplicate value"));
    }

    @Test
    @DisplayName("DELETE 409 when publisher still has books")
    void deleteConflict() throws Exception {
        doThrow(new RelationshipConflictException("Cannot delete publisher 1 because they still have books"))
                .when(publisherService).deletePublisher(1L);
        mockMvc.perform(delete("/api/publishers/1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Relationship conflict"));
    }

    @Test
    @DisplayName("GET /api/publishers/1/books returns a paged response")
    void nestedBooks() throws Exception {
        PagedResponse<BookResponseDto> page = new PagedResponse<>(List.of(), 0, 20, 0, 0, true);
        given(bookService.getBooksByPublisher(any(Long.class), any(Pageable.class))).willReturn(page);
        mockMvc.perform(get("/api/publishers/1/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }
}
