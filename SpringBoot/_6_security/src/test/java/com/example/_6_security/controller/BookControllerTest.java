package com.example._6_security.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.example._6_security.dto.AuthorSummaryDto;
import com.example._6_security.dto.BookRequestDto;
import com.example._6_security.dto.BookResponseDto;
import com.example._6_security.dto.BookSearchCriteria;
import com.example._6_security.dto.CategorySummaryDto;
import com.example._6_security.dto.PagedResponse;
import com.example._6_security.dto.PublisherSummaryDto;
import com.example._6_security.exception.DuplicateResourceException;
import com.example._6_security.exception.ResourceNotFoundException;
import com.example._6_security.security.SecurityConfig;
import com.example._6_security.service.BookService;
import tools.jackson.databind.ObjectMapper;

// Lab 6: import the real security rules into the slice so authorization is exercised.
// UserDetailsService is mocked because SecurityConfig's DaoAuthenticationProvider requires
// it to build the AuthenticationManager (the slice does not load the @Service impl).
@WebMvcTest(BookController.class)
@Import(SecurityConfig.class)
@DisplayName("BookController")
class BookControllerTest {

    // Build MockMvc from the context with springSecurity() applied so the security filter
    // chain runs AND @WithMockUser's SecurityContext is bridged into each request.
    @Autowired private WebApplicationContext context;
    @Autowired private ObjectMapper objectMapper;
    @MockitoBean private BookService bookService;
    @MockitoBean private UserDetailsService userDetailsService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUpMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    private BookRequestDto validRequest() {
        return BookRequestDto.builder()
                .title("Clean Code").isbn("9780132350884").publicationYear(2008)
                .price(new BigDecimal("42.50")).authorId(1L).publisherId(2L)
                .categoryIds(List.of(3L)).build();
    }

    private BookResponseDto sampleResponse() {
        return new BookResponseDto(
                10L, "Clean Code", "9780132350884", 2008, new BigDecimal("42.50"),
                new AuthorSummaryDto(1L, "Robert", "Martin"),
                new PublisherSummaryDto(2L, "Prentice Hall"),
                List.of(new CategorySummaryDto(3L, "Software")));
    }

    private byte[] json(Object o) throws Exception {
        return objectMapper.writeValueAsBytes(o);
    }

    @Test
    @DisplayName("POST 401 when anonymous (book writes require authentication)")
    void createAnonymousUnauthorized() throws Exception {
        mockMvc.perform(post("/api/books").contentType(MediaType.APPLICATION_JSON).content(json(validRequest())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("POST 201 + Location + nested body (authenticated USER)")
    void create() throws Exception {
        given(bookService.createBook(any(BookRequestDto.class))).willReturn(sampleResponse());
        mockMvc.perform(post("/api/books").contentType(MediaType.APPLICATION_JSON).content(json(validRequest())))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/books/10")))
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.author.lastName").value("Martin"))
                .andExpect(jsonPath("$.publisher.name").value("Prentice Hall"))
                .andExpect(jsonPath("$.categories[0].name").value("Software"));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("POST 400 when ISBN is not 13 digits")
    void createInvalidIsbn() throws Exception {
        BookRequestDto bad = validRequest();
        bad.setIsbn("123");
        mockMvc.perform(post("/api/books").contentType(MediaType.APPLICATION_JSON).content(json(bad)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.isbn").value("ISBN must be 13 digits"));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("POST 409 when ISBN already exists")
    void createDuplicate() throws Exception {
        given(bookService.createBook(any(BookRequestDto.class)))
                .willThrow(new DuplicateResourceException("isbn", "9780132350884"));
        mockMvc.perform(post("/api/books").contentType(MediaType.APPLICATION_JSON).content(json(validRequest())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Duplicate value"));
    }

    @Test
    @DisplayName("GET /api/books returns a paged response")
    void getAllPaged() throws Exception {
        PagedResponse<BookResponseDto> page = new PagedResponse<>(List.of(sampleResponse()), 0, 20, 1, 1, true);
        given(bookService.getAllBooks(any(Pageable.class))).willReturn(page);
        mockMvc.perform(get("/api/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(10))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.last").value(true));
    }

    @Test
    @DisplayName("GET /api/books?categoryId=3 delegates to the category filter")
    void getByCategory() throws Exception {
        PagedResponse<BookResponseDto> page = new PagedResponse<>(List.of(sampleResponse()), 0, 20, 1, 1, true);
        given(bookService.getBooksByCategory(eq(3L), any(Pageable.class))).willReturn(page);
        mockMvc.perform(get("/api/books").param("categoryId", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(10));
    }

    @Test
    @DisplayName("GET /api/books/search binds every optional filter into a BookSearchCriteria and returns a paged response")
    void search() throws Exception {
        PagedResponse<BookResponseDto> page = new PagedResponse<>(List.of(sampleResponse()), 0, 20, 1, 1, true);
        given(bookService.searchBooks(any(BookSearchCriteria.class), any(Pageable.class))).willReturn(page);

        mockMvc.perform(get("/api/books/search")
                        .param("title", "clean")
                        .param("author", "martin")
                        .param("category", "Software")
                        .param("minPrice", "40")
                        .param("maxPrice", "50")
                        .param("minYear", "2000")
                        .param("maxYear", "2020"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(10))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.last").value(true));

        ArgumentCaptor<BookSearchCriteria> captor = ArgumentCaptor.forClass(BookSearchCriteria.class);
        verify(bookService).searchBooks(captor.capture(), any(Pageable.class));
        BookSearchCriteria bound = captor.getValue();
        assertThat(bound.title()).isEqualTo("clean");
        assertThat(bound.author()).isEqualTo("martin");
        assertThat(bound.category()).isEqualTo("Software");
        assertThat(bound.minPrice()).isEqualByComparingTo(new BigDecimal("40"));
        assertThat(bound.maxPrice()).isEqualByComparingTo(new BigDecimal("50"));
        assertThat(bound.minYear()).isEqualTo(2000);
        assertThat(bound.maxYear()).isEqualTo(2020);
    }

    @Test
    @DisplayName("GET /api/books/search with no params returns a paged response (all books)")
    void searchNoParams() throws Exception {
        PagedResponse<BookResponseDto> page = new PagedResponse<>(List.of(sampleResponse()), 0, 20, 1, 1, true);
        given(bookService.searchBooks(any(BookSearchCriteria.class), any(Pageable.class))).willReturn(page);
        mockMvc.perform(get("/api/books/search"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(10));
    }

    @Test
    @DisplayName("GET /api/books/{id} 200 with full body")
    void getById() throws Exception {
        given(bookService.getBookById(10L)).willReturn(sampleResponse());
        mockMvc.perform(get("/api/books/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categories[0].name").value("Software"));
    }

    @Test
    @DisplayName("GET /api/books/{id} 404 when missing")
    void getByIdMissing() throws Exception {
        given(bookService.getBookById(9999L)).willThrow(new ResourceNotFoundException("Book", 9999L));
        mockMvc.perform(get("/api/books/9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource not found"));
    }

    @Test
    @DisplayName("GET /api/books/abc 400 (non-numeric id)")
    void getByIdNonNumeric() throws Exception {
        mockMvc.perform(get("/api/books/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid parameter"));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("DELETE 204")
    void deleteOk() throws Exception {
        mockMvc.perform(delete("/api/books/10"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("DELETE 404 when missing")
    void deleteMissing() throws Exception {
        doThrow(new ResourceNotFoundException("Book", 9999L)).when(bookService).deleteBook(9999L);
        mockMvc.perform(delete("/api/books/9999"))
                .andExpect(status().isNotFound());
    }
}
