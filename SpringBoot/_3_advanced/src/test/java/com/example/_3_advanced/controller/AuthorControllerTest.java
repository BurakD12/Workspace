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

import com.example._3_advanced.dto.AuthorRequestDto;
import com.example._3_advanced.dto.AuthorResponseDto;
import com.example._3_advanced.dto.BookResponseDto;
import com.example._3_advanced.dto.PagedResponse;
import com.example._3_advanced.exception.RelationshipConflictException;
import com.example._3_advanced.exception.ResourceNotFoundException;
import com.example._3_advanced.service.AuthorService;
import com.example._3_advanced.service.BookService;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(AuthorController.class)
@DisplayName("AuthorController")
class AuthorControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockitoBean private AuthorService authorService;
    @MockitoBean private BookService bookService;

    @Test
    @DisplayName("POST 201 + Location header")
    void create() throws Exception {
        given(authorService.createAuthor(any(AuthorRequestDto.class)))
                .willReturn(new AuthorResponseDto(1L, "Robert", "Martin", "bio"));
        mockMvc.perform(post("/api/authors").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new AuthorRequestDto("Robert", "Martin", "bio"))))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/authors/1")))
                .andExpect(jsonPath("$.lastName").value("Martin"));
    }

    @Test
    @DisplayName("POST 400 when first name is blank")
    void createInvalid() throws Exception {
        mockMvc.perform(post("/api/authors").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new AuthorRequestDto("", "Martin", null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.firstName").value("First name is required"));
    }

    @Test
    @DisplayName("GET /api/authors/{id} 404 when missing")
    void getByIdMissing() throws Exception {
        given(authorService.getAuthorById(9999L)).willThrow(new ResourceNotFoundException("Author", 9999L));
        mockMvc.perform(get("/api/authors/9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value(containsString("9999")));
    }

    @Test
    @DisplayName("DELETE 409 when the author still has books")
    void deleteConflict() throws Exception {
        doThrow(new RelationshipConflictException("Cannot delete author 1 because they still have books"))
                .when(authorService).deleteAuthor(1L);
        mockMvc.perform(delete("/api/authors/1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Relationship conflict"));
    }

    @Test
    @DisplayName("GET /api/authors/{id}/books returns a paged response")
    void nestedBooks() throws Exception {
        PagedResponse<BookResponseDto> page = new PagedResponse<>(List.of(), 0, 20, 0, 0, true);
        given(bookService.getBooksByAuthor(any(Long.class), any(Pageable.class))).willReturn(page);
        mockMvc.perform(get("/api/authors/1/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }
}
