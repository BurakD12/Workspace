package com.example._7_jwt.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
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
import com.example._7_jwt.dto.ProjectRequestDto;
import com.example._7_jwt.dto.ProjectResponseDto;
import com.example._7_jwt.dto.TaskResponseDto;
import com.example._7_jwt.dto.UserSummaryDto;
import com.example._7_jwt.exception.ResourceNotFoundException;
import com.example._7_jwt.security.SecurityConfig;
import com.example._7_jwt.service.ProjectService;
import com.example._7_jwt.service.TaskService;
import tools.jackson.databind.ObjectMapper;

// All project endpoints require authentication (anonymous → 401). SecurityConfig is imported so
// the slice enforces the real rules; UserDetailsService is mocked for the AuthenticationManager.
@WebMvcTest(ProjectController.class)
@Import(SecurityConfig.class)
@DisplayName("ProjectController")
class ProjectControllerTest {

    @Autowired private WebApplicationContext context;
    @Autowired private ObjectMapper objectMapper;
    @MockitoBean private ProjectService projectService;
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

    private ProjectResponseDto sample() {
        return new ProjectResponseDto(7L, "Apollo", "Moon landing", new UserSummaryDto(1L, "alice"));
    }

    @Test
    @DisplayName("POST 401 when anonymous")
    void createAnonymousUnauthorized() throws Exception {
        mockMvc.perform(post("/api/projects").contentType(MediaType.APPLICATION_JSON)
                        .content(json(new ProjectRequestDto("Apollo", "Moon landing"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "alice", roles = "USER")
    @DisplayName("POST 201 + Location, owner set to the authenticated principal")
    void create() throws Exception {
        given(projectService.createProject(any(ProjectRequestDto.class), eq("alice"))).willReturn(sample());
        mockMvc.perform(post("/api/projects").contentType(MediaType.APPLICATION_JSON)
                        .content(json(new ProjectRequestDto("Apollo", "Moon landing"))))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/projects/7")))
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.name").value("Apollo"))
                .andExpect(jsonPath("$.owner.username").value("alice"));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("POST 400 when the name is blank")
    void createInvalid() throws Exception {
        mockMvc.perform(post("/api/projects").contentType(MediaType.APPLICATION_JSON)
                        .content(json(new ProjectRequestDto("", "desc"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").value("Name is required"));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("GET /api/projects 200 paged")
    void getAll() throws Exception {
        given(projectService.getAllProjects(any(Pageable.class)))
                .willReturn(new PagedResponse<>(List.of(sample()), 0, 20, 1, 1, true));
        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(7))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.last").value(true));
    }

    @Test
    @DisplayName("GET /api/projects 401 when anonymous")
    void getAllAnonymousUnauthorized() throws Exception {
        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("GET /api/projects/{id} 200")
    void getById() throws Exception {
        given(projectService.getProjectById(7L)).willReturn(sample());
        mockMvc.perform(get("/api/projects/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.owner.username").value("alice"));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("GET /api/projects/{id} 404 when missing")
    void getByIdMissing() throws Exception {
        given(projectService.getProjectById(9999L)).willThrow(new ResourceNotFoundException("Project", 9999L));
        mockMvc.perform(get("/api/projects/9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource not found"));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("PUT /api/projects/{id} 200")
    void update() throws Exception {
        given(projectService.updateProject(eq(7L), any(ProjectRequestDto.class))).willReturn(sample());
        mockMvc.perform(put("/api/projects/7").contentType(MediaType.APPLICATION_JSON)
                        .content(json(new ProjectRequestDto("Apollo", "updated"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("DELETE /api/projects/{id} 204")
    void deleteOk() throws Exception {
        mockMvc.perform(delete("/api/projects/7"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /api/projects/{id} 401 when anonymous")
    void deleteAnonymousUnauthorized() throws Exception {
        mockMvc.perform(delete("/api/projects/7"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("GET /api/projects/{id}/tasks 200 paged (nested task listing)")
    void getProjectTasks() throws Exception {
        TaskResponseDto task = new TaskResponseDto(
                50L, "Design", "d", com.example._7_jwt.model.Status.TODO,
                com.example._7_jwt.model.Priority.HIGH, null,
                new com.example._7_jwt.dto.ProjectSummaryDto(7L, "Apollo"), null);
        given(taskService.getTasksByProject(eq(7L), any(Pageable.class)))
                .willReturn(new PagedResponse<>(List.of(task), 0, 20, 1, 1, true));
        mockMvc.perform(get("/api/projects/7/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(50))
                .andExpect(jsonPath("$.content[0].project.name").value("Apollo"));
    }
}
