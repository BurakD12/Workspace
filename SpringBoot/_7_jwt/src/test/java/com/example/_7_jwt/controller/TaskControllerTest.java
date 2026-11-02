package com.example._7_jwt.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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

import com.example._7_jwt.dto.PagedResponse;
import com.example._7_jwt.dto.ProjectSummaryDto;
import com.example._7_jwt.dto.TaskRequestDto;
import com.example._7_jwt.dto.TaskResponseDto;
import com.example._7_jwt.dto.UserSummaryDto;
import com.example._7_jwt.exception.ResourceNotFoundException;
import com.example._7_jwt.model.Priority;
import com.example._7_jwt.model.Status;
import com.example._7_jwt.security.SecurityConfig;
import com.example._7_jwt.service.TaskService;
import tools.jackson.databind.ObjectMapper;

// All task endpoints require authentication (anonymous → 401).
@WebMvcTest(TaskController.class)
@Import(SecurityConfig.class)
@DisplayName("TaskController")
class TaskControllerTest {

    @Autowired private WebApplicationContext context;
    @Autowired private ObjectMapper objectMapper;
    @MockitoBean private TaskService taskService;
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

    private TaskRequestDto validRequest() {
        return TaskRequestDto.builder()
                .title("Design").description("d").status(Status.TODO).priority(Priority.HIGH)
                .dueDate(LocalDate.of(2026, 3, 1)).projectId(1L).assigneeId(2L).build();
    }

    private TaskResponseDto sample() {
        return new TaskResponseDto(
                50L, "Design", "d", Status.TODO, Priority.HIGH, LocalDate.of(2026, 3, 1),
                new ProjectSummaryDto(1L, "Apollo"), new UserSummaryDto(2L, "bob"));
    }

    @Test
    @DisplayName("POST 401 when anonymous")
    void createAnonymousUnauthorized() throws Exception {
        mockMvc.perform(post("/api/tasks").contentType(MediaType.APPLICATION_JSON).content(json(validRequest())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("POST 201 + Location + nested body")
    void create() throws Exception {
        given(taskService.createTask(any(TaskRequestDto.class))).willReturn(sample());
        mockMvc.perform(post("/api/tasks").contentType(MediaType.APPLICATION_JSON).content(json(validRequest())))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/tasks/50")))
                .andExpect(jsonPath("$.id").value(50))
                .andExpect(jsonPath("$.status").value("TODO"))
                .andExpect(jsonPath("$.dueDate").value("2026-03-01"))
                .andExpect(jsonPath("$.project.name").value("Apollo"))
                .andExpect(jsonPath("$.assignee.username").value("bob"));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("POST 400 when projectId is missing")
    void createMissingProjectId() throws Exception {
        TaskRequestDto bad = TaskRequestDto.builder().title("Design").build();
        mockMvc.perform(post("/api/tasks").contentType(MediaType.APPLICATION_JSON).content(json(bad)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.projectId").value("Project id is required"));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("GET /api/tasks binds optional ?status and ?assigneeId filters and returns a paged response")
    void getAllWithFilters() throws Exception {
        given(taskService.getTasks(any(), any(), any(Pageable.class)))
                .willReturn(new PagedResponse<>(List.of(sample()), 0, 20, 1, 1, true));

        mockMvc.perform(get("/api/tasks").param("status", "DONE").param("assigneeId", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(50))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(taskService).getTasks(eq(Status.DONE), eq(5L), any(Pageable.class));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("GET /api/tasks?status=BOGUS 400 (invalid enum value)")
    void getAllInvalidStatus() throws Exception {
        mockMvc.perform(get("/api/tasks").param("status", "BOGUS"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid parameter"));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("GET /api/tasks/{id} 200")
    void getById() throws Exception {
        given(taskService.getTaskById(50L)).willReturn(sample());
        mockMvc.perform(get("/api/tasks/50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Design"));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("GET /api/tasks/{id} 404 when missing")
    void getByIdMissing() throws Exception {
        given(taskService.getTaskById(9999L)).willThrow(new ResourceNotFoundException("Task", 9999L));
        mockMvc.perform(get("/api/tasks/9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource not found"));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("GET /api/tasks/abc 400 (non-numeric id)")
    void getByIdNonNumeric() throws Exception {
        mockMvc.perform(get("/api/tasks/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid parameter"));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("PUT /api/tasks/{id} 200")
    void update() throws Exception {
        given(taskService.updateTask(eq(50L), any(TaskRequestDto.class))).willReturn(sample());
        mockMvc.perform(put("/api/tasks/50").contentType(MediaType.APPLICATION_JSON).content(json(validRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(50));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("DELETE /api/tasks/{id} 204")
    void deleteOk() throws Exception {
        mockMvc.perform(delete("/api/tasks/50"))
                .andExpect(status().isNoContent());
    }
}
