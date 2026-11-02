package com.example._8_method_security.controller;

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

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.example._8_method_security.dto.CommentRequestDto;
import com.example._8_method_security.dto.CommentResponseDto;
import com.example._8_method_security.dto.CommentUpdateRequestDto;
import com.example._8_method_security.dto.TaskSummaryDto;
import com.example._8_method_security.dto.UserSummaryDto;
import com.example._8_method_security.exception.ResourceNotFoundException;
import com.example._8_method_security.security.SecurityConfig;
import com.example._8_method_security.service.CommentService;
import tools.jackson.databind.ObjectMapper;

/**
 * Lab 8 (TaskFlow) — the {@code CommentController} web contract.
 *
 * <p>What this slice covers: status codes, the {@code Location} header, validation, and that a
 * denial from the service layer is rendered as a <b>403 ProblemDetail</b> rather than leaking out as
 * a 500. That last test is the one that matters most here — {@code AuthorizationDeniedException} is a
 * {@code RuntimeException}, so without the dedicated handler in {@code GlobalExceptionHandler} the
 * catch-all {@code Exception} handler would turn every legitimate denial into a 500.
 *
 * <p>What it deliberately does <b>not</b> cover: the {@code @PreAuthorize} rules themselves. This
 * slice replaces {@code CommentService} with a mock of the interface, so the annotations on the
 * implementation never execute. They are proven in {@code MethodSecurityTest}, and simulated here by
 * making the mock throw.
 */
@WebMvcTest(CommentController.class)
@Import(SecurityConfig.class)
@DisplayName("CommentController")
class CommentControllerTest {

    @Autowired private WebApplicationContext context;
    @Autowired private ObjectMapper objectMapper;
    @MockitoBean private CommentService commentService;
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

    private CommentResponseDto sample() {
        return new CommentResponseDto(502L, "On it", Instant.parse("2026-08-01T10:00:00Z"),
                new TaskSummaryDto(50L, "Design the lander"), new UserSummaryDto(2L, "bob"));
    }

    private CommentRequestDto validRequest() {
        return CommentRequestDto.builder().taskId(50L).text("On it").build();
    }

    @Test
    @DisplayName("POST 401 when anonymous")
    void createAnonymousUnauthorized() throws Exception {
        mockMvc.perform(post("/api/comments").contentType(MediaType.APPLICATION_JSON)
                        .content(json(validRequest())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "bob", roles = "USER")
    @DisplayName("POST 201 + Location; the author comes from the token, not the body")
    void create() throws Exception {
        given(commentService.createComment(any(CommentRequestDto.class), eq("bob"))).willReturn(sample());

        mockMvc.perform(post("/api/comments").contentType(MediaType.APPLICATION_JSON)
                        .content(json(validRequest())))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/comments/502")))
                .andExpect(jsonPath("$.id").value(502))
                .andExpect(jsonPath("$.text").value("On it"))
                .andExpect(jsonPath("$.author.username").value("bob"))
                .andExpect(jsonPath("$.task.id").value(50));
    }

    @Test
    @WithMockUser(username = "bob", roles = "USER")
    @DisplayName("POST 400 when text is blank")
    void createBlankText() throws Exception {
        mockMvc.perform(post("/api/comments").contentType(MediaType.APPLICATION_JSON)
                        .content(json(CommentRequestDto.builder().taskId(50L).text("  ").build())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.errors.text").value("Text is required"));
    }

    @Test
    @WithMockUser(username = "bob", roles = "USER")
    @DisplayName("POST 400 when taskId is missing")
    void createMissingTaskId() throws Exception {
        mockMvc.perform(post("/api/comments").contentType(MediaType.APPLICATION_JSON)
                        .content(json(CommentRequestDto.builder().text("On it").build())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.taskId").value("Task id is required"));
    }

    @Test
    @WithMockUser(username = "bob", roles = "USER")
    @DisplayName("GET /api/comments/{id} 200")
    void getById() throws Exception {
        given(commentService.getCommentById(502L)).willReturn(sample());

        mockMvc.perform(get("/api/comments/502"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(502))
                .andExpect(jsonPath("$.createdAt").value("2026-08-01T10:00:00Z"));
    }

    @Test
    @WithMockUser(username = "bob", roles = "USER")
    @DisplayName("GET /api/comments/{id} 404 for an unknown id")
    void getByIdUnknown() throws Exception {
        given(commentService.getCommentById(999L)).willThrow(new ResourceNotFoundException("Comment", 999L));

        mockMvc.perform(get("/api/comments/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource not found"));
    }

    @Test
    @WithMockUser(username = "bob", roles = "USER")
    @DisplayName("PUT /api/comments/{id} 200")
    void update() throws Exception {
        given(commentService.updateComment(eq(502L), any(CommentUpdateRequestDto.class)))
                .willReturn(sample());

        mockMvc.perform(put("/api/comments/502").contentType(MediaType.APPLICATION_JSON)
                        .content(json(new CommentUpdateRequestDto("Edited"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(502));
    }

    @Test
    @WithMockUser(username = "bob", roles = "USER")
    @DisplayName("DELETE /api/comments/{id} 204")
    void deleteOk() throws Exception {
        mockMvc.perform(delete("/api/comments/502"))
                .andExpect(status().isNoContent());
        verify(commentService).deleteComment(502L);
    }

    // --- The 403 wiring: a method-security denial must not surface as a 500 ---------------------

    @Test
    @WithMockUser(username = "carol", roles = "USER")
    @DisplayName("PUT 403 ProblemDetail when the service denies the write (not 500)")
    void updateDeniedIsForbidden() throws Exception {
        willThrow(new AuthorizationDeniedException("Access Denied"))
                .given(commentService).updateComment(eq(502L), any(CommentUpdateRequestDto.class));

        mockMvc.perform(put("/api/comments/502").contentType(MediaType.APPLICATION_JSON)
                        .content(json(new CommentUpdateRequestDto("Hijacked"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.title").value("Access denied"))
                // The detail is generic on purpose: it must not reveal whether the row exists.
                .andExpect(jsonPath("$.detail").value("You do not have permission to perform this action."));
    }

    @Test
    @WithMockUser(username = "carol", roles = "USER")
    @DisplayName("DELETE 403 ProblemDetail when the service denies the delete")
    void deleteDeniedIsForbidden() throws Exception {
        willThrow(new AuthorizationDeniedException("Access Denied"))
                .given(commentService).deleteComment(502L);

        mockMvc.perform(delete("/api/comments/502"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Access denied"));
    }
}
