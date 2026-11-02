package com.example._8_method_security.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import com.example._8_method_security.dto.CommentRequestDto;
import com.example._8_method_security.dto.CommentResponseDto;
import com.example._8_method_security.dto.CommentUpdateRequestDto;
import com.example._8_method_security.dto.ProjectRequestDto;
import com.example._8_method_security.dto.ProjectResponseDto;
import com.example._8_method_security.dto.TaskRequestDto;
import com.example._8_method_security.exception.ResourceNotFoundException;
import com.example._8_method_security.model.Comment;
import com.example._8_method_security.model.Project;
import com.example._8_method_security.model.Role;
import com.example._8_method_security.model.Task;
import com.example._8_method_security.model.User;
import com.example._8_method_security.repository.CommentRepository;
import com.example._8_method_security.repository.ProjectRepository;
import com.example._8_method_security.repository.TaskRepository;
import com.example._8_method_security.repository.UserRepository;
import com.example._8_method_security.service.CommentService;
import com.example._8_method_security.service.CommentServiceImpl;
import com.example._8_method_security.service.ProjectService;
import com.example._8_method_security.service.ProjectServiceImpl;
import com.example._8_method_security.service.TaskService;
import com.example._8_method_security.service.TaskServiceImpl;
import com.example._8_method_security.service.UserService;
import com.example._8_method_security.service.UserServiceImpl;

/**
 * Lab 8 (TaskFlow) — the test layer that actually proves method security works.
 *
 * <p><b>Why this is a Spring test and not a plain Mockito one.</b> {@code @PreAuthorize} is enforced
 * by an AOP <b>proxy</b> that Spring wraps around the bean. A Mockito unit test calls
 * {@code new ProjectServiceImpl(...)} directly — there is no proxy, so the annotations are inert and
 * every "denied" test would pass for the wrong reason: nothing was ever checked. A
 * {@code @WebMvcTest} slice is no better here, because it replaces the service with a mock of the
 * interface, and the rules live on the implementation.
 *
 * <p><b>Why it is not a {@code @SpringBootTest} either.</b> All that is required is a container that
 * creates proxies. So this test builds the smallest context that can: a {@code @Configuration} with
 * {@code @EnableMethodSecurity} and the real service implementations, over {@code @MockitoBean}
 * repositories. No web server, no database, no auto-configuration — milliseconds per test, and the
 * failure of any assertion points at an authorization rule rather than at infrastructure.
 * (Full-context integration testing arrives in Lab 10.)
 *
 * <p><b>Two details that make it work:</b>
 * <ul>
 *   <li>The services are exposed as their <b>interface</b> types, because a proxied bean with an
 *       interface is a JDK dynamic proxy — autowiring {@code ProjectServiceImpl} would fail.</li>
 *   <li>{@code taskFlowPermissions} is declared as an explicit {@code @Bean} <b>method</b>, because
 *       SpEL resolves {@code @taskFlowPermissions} by <b>bean name</b> and the method name supplies
 *       it. Registering the class another way (e.g. via {@code @Import}) names the bean after its
 *       fully-qualified class name and every expression referencing it fails at call time.</li>
 * </ul>
 *
 * <p>{@code @WithMockUser} populates the {@code SecurityContext} before each test, so the rules see a
 * principal without any login happening — the mechanism (Basic, JWT) is irrelevant to method
 * security, which only reads the {@code Authentication}.
 *
 * <p>A denial surfaces as {@link AuthorizationDeniedException} (a subclass of
 * {@link AccessDeniedException}), which {@code GlobalExceptionHandler} renders as 403.
 */
@SpringJUnitConfig(MethodSecurityTest.Config.class)
@DisplayName("Method security (@PreAuthorize / @PostAuthorize / @PostFilter)")
class MethodSecurityTest {

    /**
     * The minimal method-security container: real services + the permission bean, nothing else.
     * Repository dependencies are satisfied by the {@code @MockitoBean} fields below.
     */
    @Configuration
    @EnableMethodSecurity
    static class Config {

        @Bean
        ProjectService projectService(ProjectRepository projects, UserRepository users) {
            return new ProjectServiceImpl(projects, users);
        }

        @Bean
        TaskService taskService(TaskRepository tasks, ProjectRepository projects, UserRepository users) {
            return new TaskServiceImpl(tasks, projects, users);
        }

        @Bean
        CommentService commentService(CommentRepository comments, TaskRepository tasks, UserRepository users) {
            return new CommentServiceImpl(comments, tasks, users);
        }

        @Bean
        UserService userService(UserRepository users) {
            return new UserServiceImpl(users);
        }

        /** Bean NAME matters: SpEL looks up "@taskFlowPermissions" by name. */
        @Bean
        TaskFlowPermissions taskFlowPermissions(ProjectRepository projects, TaskRepository tasks,
                CommentRepository comments) {
            return new TaskFlowPermissions(projects, tasks, comments);
        }
    }

    @MockitoBean private ProjectRepository projectRepository;
    @MockitoBean private TaskRepository taskRepository;
    @MockitoBean private CommentRepository commentRepository;
    @MockitoBean private UserRepository userRepository;

    @Autowired private ProjectService projectService;
    @Autowired private TaskService taskService;
    @Autowired private CommentService commentService;
    @Autowired private UserService userService;

    // ---------------------------------------------------------------------------------------------
    // Fixtures. alice owns project 7; task 50 lives in it and is assigned to bob; carol is a stranger.
    // ---------------------------------------------------------------------------------------------

    private static final long PROJECT_ID = 7L;
    private static final long TASK_ID = 50L;

    private User user(long id, String username, Role role) {
        return User.builder().id(id).username(username).email(username + "@taskflow.io")
                .password("hash").roles(Set.of(role)).build();
    }

    private Project aliceProject() {
        return Project.builder().id(PROJECT_ID).name("Apollo").description("Moon landing")
                .owner(user(1L, "alice", Role.USER)).build();
    }

    private Task bobsTask() {
        return Task.builder().id(TASK_ID).title("Design the lander").project(aliceProject())
                .assignee(user(2L, "bob", Role.USER)).build();
    }

    private Comment comment(long id, String authorName, long authorId, String text) {
        return Comment.builder().id(id).text(text).createdAt(Instant.parse("2026-08-01T10:00:00Z"))
                .task(bobsTask()).author(user(authorId, authorName, Role.USER)).build();
    }

    private TaskRequestDto taskRequest(Long projectId) {
        return TaskRequestDto.builder().title("Design the lander").projectId(projectId).build();
    }

    // =============================================================================================
    // PROJECTS — @PreAuthorize on an argument, @PostAuthorize on the returned row, scoped queries
    // =============================================================================================

    @Test
    @WithMockUser(username = "alice", roles = "USER")
    @DisplayName("createProject: allowed when the owner argument is the principal")
    void createProjectAsSelf() {
        given(userRepository.findByUsername("alice")).willReturn(Optional.of(user(1L, "alice", Role.USER)));
        given(projectRepository.save(any(Project.class))).willAnswer(inv -> {
            Project p = inv.getArgument(0);
            p.setId(PROJECT_ID);
            return p;
        });

        ProjectResponseDto created =
                projectService.createProject(new ProjectRequestDto("Apollo", "Moon"), "alice");

        assertThat(created.getOwner().getUsername()).isEqualTo("alice");
    }

    @Test
    @WithMockUser(username = "alice", roles = "USER")
    @DisplayName("createProject: denied when trying to create a project owned by someone else")
    void createProjectAsSomeoneElseDenied() {
        assertThatThrownBy(() ->
                projectService.createProject(new ProjectRequestDto("Apollo", "Moon"), "bob"))
                .isInstanceOf(AuthorizationDeniedException.class);

        // The rule ran BEFORE the body: nothing was read and nothing was written.
        verify(projectRepository, never()).save(any(Project.class));
        verify(userRepository, never()).findByUsername(any());
    }

    @Test
    @WithAnonymousUser
    @DisplayName("createProject: anonymous is denied even if it names 'anonymousUser' as the owner")
    void createProjectAnonymousDenied() {
        assertThatThrownBy(() ->
                projectService.createProject(new ProjectRequestDto("Apollo", "Moon"), "anonymousUser"))
                .isInstanceOf(AuthorizationDeniedException.class);
    }

    @Test
    @WithMockUser(username = "alice", roles = "USER")
    @DisplayName("getProjectById: @PostAuthorize allows the owner")
    void getProjectByIdAsOwner() {
        given(projectRepository.findById(PROJECT_ID)).willReturn(Optional.of(aliceProject()));

        assertThat(projectService.getProjectById(PROJECT_ID).getName()).isEqualTo("Apollo");
    }

    @Test
    @WithMockUser(username = "carol", roles = "USER")
    @DisplayName("getProjectById: @PostAuthorize denies a non-owner AFTER the row was loaded")
    void getProjectByIdAsStrangerDenied() {
        given(projectRepository.findById(PROJECT_ID)).willReturn(Optional.of(aliceProject()));

        assertThatThrownBy(() -> projectService.getProjectById(PROJECT_ID))
                .isInstanceOf(AuthorizationDeniedException.class);

        // Post-invocation: the body DID run and the row WAS fetched — it just never reached the
        // caller. This is exactly why @PostAuthorize must not be used to guard a write.
        verify(projectRepository).findById(PROJECT_ID);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    @DisplayName("getProjectById: ADMIN may read any project")
    void getProjectByIdAsAdmin() {
        given(projectRepository.findById(PROJECT_ID)).willReturn(Optional.of(aliceProject()));

        assertThat(projectService.getProjectById(PROJECT_ID).getOwner().getUsername()).isEqualTo("alice");
    }

    @Test
    @WithMockUser(username = "alice", roles = "USER")
    @DisplayName("updateProject: allowed for the owner")
    void updateProjectAsOwner() {
        given(projectRepository.existsByIdAndOwnerUsername(PROJECT_ID, "alice")).willReturn(true);
        given(projectRepository.findById(PROJECT_ID)).willReturn(Optional.of(aliceProject()));
        given(projectRepository.save(any(Project.class))).willAnswer(inv -> inv.getArgument(0));

        assertThat(projectService.updateProject(PROJECT_ID, new ProjectRequestDto("Apollo 2", "Redo"))
                .getName()).isEqualTo("Apollo 2");
    }

    @Test
    @WithMockUser(username = "carol", roles = "USER")
    @DisplayName("updateProject: denied for a non-owner, and the row is never touched")
    void updateProjectAsStrangerDenied() {
        given(projectRepository.existsByIdAndOwnerUsername(PROJECT_ID, "carol")).willReturn(false);

        assertThatThrownBy(() ->
                projectService.updateProject(PROJECT_ID, new ProjectRequestDto("Hijacked", null)))
                .isInstanceOf(AuthorizationDeniedException.class);

        verify(projectRepository, never()).save(any(Project.class));
    }

    @Test
    @WithMockUser(username = "carol", roles = "USER")
    @DisplayName("deleteProject: a project id that does not exist answers 403, not 404")
    void deleteProjectUnknownIdIsForbidden() {
        given(projectRepository.existsByIdAndOwnerUsername(999L, "carol")).willReturn(false);

        assertThatThrownBy(() -> projectService.deleteProject(999L))
                .isInstanceOf(AuthorizationDeniedException.class);

        verify(projectRepository, never()).deleteById(any());
    }

    @Test
    @WithMockUser(username = "carol", roles = "USER")
    @DisplayName("getProjectById: the @PostAuthorize reads CANNOT hide existence — unknown id is 404")
    void postAuthorizeCannotHideExistence() {
        // Pinning a deliberate, documented limitation rather than pretending it away.
        //
        // A @PreAuthorize rule runs before the body, so it answers "not yours" (403) for an id that
        // does not exist. A @PostAuthorize rule cannot: the body has to run first, and the body
        // throws ResourceNotFoundException, so the rule is never evaluated.
        given(projectRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.getProjectById(999L))
                .isInstanceOf(ResourceNotFoundException.class);   // → 404, to EVERY caller

        // Whereas an existing project she does not own is a clean 403.
        given(projectRepository.findById(PROJECT_ID)).willReturn(Optional.of(aliceProject()));
        assertThatThrownBy(() -> projectService.getProjectById(PROJECT_ID))
                .isInstanceOf(AuthorizationDeniedException.class);

        // The two differ, so GET-by-id remains an existence oracle. That is the price of the
        // one-query @PostAuthorize optimisation; converting these reads to @PreAuthorize + a probe
        // would close it at the cost of an extra query per read.
    }

    @Test
    @WithMockUser(username = "alice", roles = "USER")
    @DisplayName("getAllProjects: a USER's page is scoped by the QUERY to projects they own")
    void getAllProjectsScopedForUser() {
        Pageable pageable = PageRequest.of(0, 20);
        given(projectRepository.findByOwnerUsername("alice", pageable))
                .willReturn(new PageImpl<>(List.of(aliceProject()), pageable, 1));

        assertThat(projectService.getAllProjects(pageable).getTotalElements()).isEqualTo(1);

        // The scoping happens in the WHERE clause, so totalElements is truthful. findAll is not used.
        verify(projectRepository).findByOwnerUsername("alice", pageable);
        verify(projectRepository, never()).findAll(pageable);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    @DisplayName("getAllProjects: ADMIN sees the unscoped page")
    void getAllProjectsUnscopedForAdmin() {
        Pageable pageable = PageRequest.of(0, 20);
        given(projectRepository.findAll(pageable))
                .willReturn(new PageImpl<>(List.of(aliceProject()), pageable, 1));

        assertThat(projectService.getAllProjects(pageable).getContent()).hasSize(1);

        verify(projectRepository).findAll(pageable);
        verify(projectRepository, never()).findByOwnerUsername(any(), any());
    }

    // =============================================================================================
    // TASKS — reading arguments out of the request body, and owner vs. assignee asymmetry
    // =============================================================================================

    @Test
    @WithMockUser(username = "alice", roles = "USER")
    @DisplayName("createTask: allowed when the caller owns the target project (read from the body)")
    void createTaskInOwnProject() {
        given(projectRepository.existsByIdAndOwnerUsername(PROJECT_ID, "alice")).willReturn(true);
        given(projectRepository.findById(PROJECT_ID)).willReturn(Optional.of(aliceProject()));
        given(taskRepository.save(any(Task.class))).willAnswer(inv -> {
            Task t = inv.getArgument(0);
            t.setId(TASK_ID);
            return t;
        });

        assertThat(taskService.createTask(taskRequest(PROJECT_ID)).getId()).isEqualTo(TASK_ID);
    }

    @Test
    @WithMockUser(username = "carol", roles = "USER")
    @DisplayName("createTask: denied when the body names a project the caller does not own")
    void createTaskInForeignProjectDenied() {
        given(projectRepository.existsByIdAndOwnerUsername(PROJECT_ID, "carol")).willReturn(false);

        assertThatThrownBy(() -> taskService.createTask(taskRequest(PROJECT_ID)))
                .isInstanceOf(AuthorizationDeniedException.class);

        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    @WithMockUser(username = "alice", roles = "USER")
    @DisplayName("getTasks: the cross-project feed is denied to a plain USER")
    void getTasksDeniedForUser() {
        assertThatThrownBy(() -> taskService.getTasks(null, null, PageRequest.of(0, 20)))
                .isInstanceOf(AuthorizationDeniedException.class);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    @DisplayName("getTasks: allowed for ADMIN")
    void getTasksAllowedForAdmin() {
        Pageable pageable = PageRequest.of(0, 20);
        given(taskRepository.findAll(pageable)).willReturn(new PageImpl<>(List.of(bobsTask()), pageable, 1));

        assertThat(taskService.getTasks(null, null, pageable).getContent()).hasSize(1);
    }

    @Test
    @WithMockUser(username = "bob", roles = "USER")
    @DisplayName("getTasksByProject: denied to the assignee — the project's list belongs to its owner")
    void getTasksByProjectDeniedForAssignee() {
        given(projectRepository.existsByIdAndOwnerUsername(PROJECT_ID, "bob")).willReturn(false);

        assertThatThrownBy(() -> taskService.getTasksByProject(PROJECT_ID, PageRequest.of(0, 20)))
                .isInstanceOf(AuthorizationDeniedException.class);
    }

    @Test
    @WithMockUser(username = "bob", roles = "USER")
    @DisplayName("getTaskById: the assignee may read their own task")
    void getTaskByIdAsAssignee() {
        given(taskRepository.existsByIdAndProjectOwnerUsername(TASK_ID, "bob")).willReturn(false);
        given(taskRepository.existsByIdAndAssigneeUsername(TASK_ID, "bob")).willReturn(true);
        given(taskRepository.findById(TASK_ID)).willReturn(Optional.of(bobsTask()));

        assertThat(taskService.getTaskById(TASK_ID).getTitle()).isEqualTo("Design the lander");
    }

    @Test
    @WithMockUser(username = "carol", roles = "USER")
    @DisplayName("getTaskById: denied to someone who is neither owner nor assignee")
    void getTaskByIdAsStrangerDenied() {
        given(taskRepository.existsByIdAndProjectOwnerUsername(TASK_ID, "carol")).willReturn(false);
        given(taskRepository.existsByIdAndAssigneeUsername(TASK_ID, "carol")).willReturn(false);

        assertThatThrownBy(() -> taskService.getTaskById(TASK_ID))
                .isInstanceOf(AuthorizationDeniedException.class);

        verify(taskRepository, never()).findById(TASK_ID);
    }

    @Test
    @WithMockUser(username = "bob", roles = "USER")
    @DisplayName("updateTask: the assignee may edit the task while it stays in the same project")
    void updateTaskAsAssigneeSameProject() {
        given(taskRepository.existsByIdAndAssigneeUsername(TASK_ID, "bob")).willReturn(true);
        given(taskRepository.existsByIdAndProjectId(TASK_ID, PROJECT_ID)).willReturn(true);
        given(taskRepository.findById(TASK_ID)).willReturn(Optional.of(bobsTask()));
        given(projectRepository.findById(PROJECT_ID)).willReturn(Optional.of(aliceProject()));
        given(taskRepository.save(any(Task.class))).willAnswer(inv -> inv.getArgument(0));

        assertThatCode(() -> taskService.updateTask(TASK_ID, taskRequest(PROJECT_ID)))
                .doesNotThrowAnyException();
    }

    @Test
    @WithMockUser(username = "bob", roles = "USER")
    @DisplayName("updateTask: the assignee may NOT move the task away — not even into a project they own")
    void updateTaskAsAssigneeMovingProjectDenied() {
        long bobsOwnProject = 99L;
        given(taskRepository.existsByIdAndAssigneeUsername(TASK_ID, "bob")).willReturn(true);
        // bob does not own project 7, the project the task is leaving...
        given(taskRepository.existsByIdAndProjectOwnerUsername(TASK_ID, "bob")).willReturn(false);
        // ...this really is a move (the task is not already in 99)...
        given(taskRepository.existsByIdAndProjectId(TASK_ID, bobsOwnProject)).willReturn(false);
        // ...and owning the DESTINATION is not enough: that is exactly the theft being prevented.
        given(projectRepository.existsByIdAndOwnerUsername(bobsOwnProject, "bob")).willReturn(true);

        assertThatThrownBy(() -> taskService.updateTask(TASK_ID, taskRequest(bobsOwnProject)))
                .isInstanceOf(AuthorizationDeniedException.class);

        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    @WithMockUser(username = "alice", roles = "USER")
    @DisplayName("updateTask: the project owner may move a task into another project they also own")
    void updateTaskMovingBetweenOwnProjects() {
        long alicesOtherProject = 8L;
        given(taskRepository.existsByIdAndProjectOwnerUsername(TASK_ID, "alice")).willReturn(true);
        given(taskRepository.existsByIdAndProjectId(TASK_ID, alicesOtherProject)).willReturn(false);
        given(projectRepository.existsByIdAndOwnerUsername(alicesOtherProject, "alice")).willReturn(true);
        given(taskRepository.findById(TASK_ID)).willReturn(Optional.of(bobsTask()));
        given(projectRepository.findById(alicesOtherProject))
                .willReturn(Optional.of(Project.builder().id(alicesOtherProject).name("Gemini")
                        .owner(user(1L, "alice", Role.USER)).build()));
        given(taskRepository.save(any(Task.class))).willAnswer(inv -> inv.getArgument(0));

        assertThat(taskService.updateTask(TASK_ID, taskRequest(alicesOtherProject))
                .getProject().getName()).isEqualTo("Gemini");
    }

    @Test
    @WithMockUser(username = "bob", roles = "USER")
    @DisplayName("deleteTask: denied to the assignee — deleting is an owner's call")
    void deleteTaskAsAssigneeDenied() {
        given(taskRepository.existsByIdAndProjectOwnerUsername(TASK_ID, "bob")).willReturn(false);

        assertThatThrownBy(() -> taskService.deleteTask(TASK_ID))
                .isInstanceOf(AuthorizationDeniedException.class);

        verify(taskRepository, never()).deleteById(any());
    }

    @Test
    @WithMockUser(username = "alice", roles = "USER")
    @DisplayName("deleteTask: allowed for the owner of the task's project")
    void deleteTaskAsProjectOwner() {
        given(taskRepository.existsByIdAndProjectOwnerUsername(TASK_ID, "alice")).willReturn(true);
        given(taskRepository.existsById(TASK_ID)).willReturn(true);

        taskService.deleteTask(TASK_ID);

        verify(taskRepository).deleteById(TASK_ID);
    }

    // =============================================================================================
    // COMMENTS — @PostFilter on a list, @PostAuthorize on one row, and the update/delete asymmetry
    // =============================================================================================

    @Test
    @WithMockUser(username = "bob", roles = "USER")
    @DisplayName("createComment: the assignee may comment on their task")
    void createCommentAsAssignee() {
        given(taskRepository.existsByIdAndProjectOwnerUsername(TASK_ID, "bob")).willReturn(false);
        given(taskRepository.existsByIdAndAssigneeUsername(TASK_ID, "bob")).willReturn(true);
        given(taskRepository.findById(TASK_ID)).willReturn(Optional.of(bobsTask()));
        given(userRepository.findByUsername("bob")).willReturn(Optional.of(user(2L, "bob", Role.USER)));
        given(commentRepository.save(any(Comment.class))).willAnswer(inv -> {
            Comment c = inv.getArgument(0);
            c.setId(500L);
            return c;
        });

        CommentResponseDto created = commentService.createComment(
                CommentRequestDto.builder().taskId(TASK_ID).text("On it").build(), "bob");

        assertThat(created.getAuthor().getUsername()).isEqualTo("bob");
        assertThat(created.getTask().getId()).isEqualTo(TASK_ID);
    }

    @Test
    @WithMockUser(username = "carol", roles = "USER")
    @DisplayName("createComment: denied on a task the caller has no stake in")
    void createCommentOnForeignTaskDenied() {
        given(taskRepository.existsByIdAndProjectOwnerUsername(TASK_ID, "carol")).willReturn(false);
        given(taskRepository.existsByIdAndAssigneeUsername(TASK_ID, "carol")).willReturn(false);

        assertThatThrownBy(() -> commentService.createComment(
                CommentRequestDto.builder().taskId(TASK_ID).text("hello").build(), "carol"))
                .isInstanceOf(AuthorizationDeniedException.class);

        verify(commentRepository, never()).save(any(Comment.class));
    }

    @Test
    @WithMockUser(username = "bob", roles = "USER")
    @DisplayName("createComment: denied when the author argument is not the principal")
    void createCommentAsSomeoneElseDenied() {
        assertThatThrownBy(() -> commentService.createComment(
                CommentRequestDto.builder().taskId(TASK_ID).text("forged").build(), "alice"))
                .isInstanceOf(AuthorizationDeniedException.class);
    }

    @Test
    @WithMockUser(username = "bob", roles = "USER")
    @DisplayName("@PostFilter: the assignee sees only their own comments in the thread")
    void postFilterTrimsThreadForAssignee() {
        given(taskRepository.existsByIdAndProjectOwnerUsername(TASK_ID, "bob")).willReturn(false);
        given(taskRepository.existsByIdAndAssigneeUsername(TASK_ID, "bob")).willReturn(true);
        given(taskRepository.existsById(TASK_ID)).willReturn(true);
        given(commentRepository.findByTaskIdOrderByCreatedAtAsc(TASK_ID)).willReturn(List.of(
                comment(501L, "alice", 1L, "Owner note"),
                comment(502L, "bob", 2L, "My note")));

        List<CommentResponseDto> visible = commentService.getCommentsByTask(TASK_ID);

        // @PreAuthorize let the call through; @PostFilter dropped the row bob may not see.
        // (This also proves the service returns a MUTABLE list: Spring Security filters by calling
        // clear() + addAll() on it, so an immutable List.of/toList() would blow up right here.)
        assertThat(visible).extracting(CommentResponseDto::getText).containsExactly("My note");
    }

    @Test
    @WithMockUser(username = "alice", roles = "USER")
    @DisplayName("@PostFilter: the project owner sees the whole thread")
    void postFilterKeepsThreadForProjectOwner() {
        given(taskRepository.existsByIdAndProjectOwnerUsername(TASK_ID, "alice")).willReturn(true);
        given(taskRepository.existsById(TASK_ID)).willReturn(true);
        given(commentRepository.findByTaskIdOrderByCreatedAtAsc(TASK_ID)).willReturn(List.of(
                comment(501L, "alice", 1L, "Owner note"),
                comment(502L, "bob", 2L, "My note")));

        assertThat(commentService.getCommentsByTask(TASK_ID))
                .extracting(CommentResponseDto::getText)
                .containsExactly("Owner note", "My note");
    }

    @Test
    @WithMockUser(username = "carol", roles = "USER")
    @DisplayName("getCommentsByTask: @PreAuthorize denies a stranger before any query runs")
    void getCommentsByTaskStrangerDenied() {
        given(taskRepository.existsByIdAndProjectOwnerUsername(TASK_ID, "carol")).willReturn(false);
        given(taskRepository.existsByIdAndAssigneeUsername(TASK_ID, "carol")).willReturn(false);

        assertThatThrownBy(() -> commentService.getCommentsByTask(TASK_ID))
                .isInstanceOf(AuthorizationDeniedException.class);

        verify(commentRepository, never()).findByTaskIdOrderByCreatedAtAsc(any());
    }

    @Test
    @WithMockUser(username = "bob", roles = "USER")
    @DisplayName("getCommentById: @PostAuthorize allows the author")
    void getCommentByIdAsAuthor() {
        given(commentRepository.findById(502L)).willReturn(Optional.of(comment(502L, "bob", 2L, "My note")));

        assertThat(commentService.getCommentById(502L).getText()).isEqualTo("My note");
    }

    @Test
    @WithMockUser(username = "carol", roles = "USER")
    @DisplayName("getCommentById: @PostAuthorize denies a stranger by inspecting returnObject")
    void getCommentByIdAsStrangerDenied() {
        given(commentRepository.findById(502L)).willReturn(Optional.of(comment(502L, "bob", 2L, "My note")));
        given(taskRepository.existsByIdAndProjectOwnerUsername(TASK_ID, "carol")).willReturn(false);

        assertThatThrownBy(() -> commentService.getCommentById(502L))
                .isInstanceOf(AuthorizationDeniedException.class);
    }

    @Test
    @WithMockUser(username = "bob", roles = "USER")
    @DisplayName("updateComment: the author may edit their own comment")
    void updateCommentAsAuthor() {
        given(commentRepository.existsByIdAndAuthorUsername(502L, "bob")).willReturn(true);
        given(commentRepository.findById(502L)).willReturn(Optional.of(comment(502L, "bob", 2L, "My note")));
        given(commentRepository.save(any(Comment.class))).willAnswer(inv -> inv.getArgument(0));

        assertThat(commentService.updateComment(502L, new CommentUpdateRequestDto("Edited")).getText())
                .isEqualTo("Edited");
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    @DisplayName("updateComment: even ADMIN cannot rewrite someone else's comment")
    void updateCommentAsAdminDenied() {
        given(commentRepository.existsByIdAndAuthorUsername(502L, "admin")).willReturn(false);

        assertThatThrownBy(() ->
                commentService.updateComment(502L, new CommentUpdateRequestDto("Rewritten by admin")))
                .isInstanceOf(AuthorizationDeniedException.class);

        verify(commentRepository, never()).save(any(Comment.class));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    @DisplayName("deleteComment: ADMIN may moderate any comment")
    void deleteCommentAsAdmin() {
        given(commentRepository.existsById(502L)).willReturn(true);

        commentService.deleteComment(502L);

        verify(commentRepository).deleteById(502L);
    }

    @Test
    @WithMockUser(username = "carol", roles = "USER")
    @DisplayName("deleteComment: denied to a non-author, non-admin")
    void deleteCommentAsStrangerDenied() {
        given(commentRepository.existsByIdAndAuthorUsername(502L, "carol")).willReturn(false);

        assertThatThrownBy(() -> commentService.deleteComment(502L))
                .isInstanceOf(AuthorizationDeniedException.class);

        verify(commentRepository, never()).deleteById(any());
    }

    // =============================================================================================
    // USERS — the simplest ownership rule: compare an argument to the principal
    // =============================================================================================

    @Test
    @WithMockUser(username = "alice", roles = "USER")
    @DisplayName("getByUsername: allowed for your own profile (#username == authentication.name)")
    void getOwnProfile() {
        given(userRepository.findByUsername("alice")).willReturn(Optional.of(user(1L, "alice", Role.USER)));

        assertThat(userService.getByUsername("alice").getUsername()).isEqualTo("alice");
    }

    @Test
    @WithMockUser(username = "alice", roles = "USER")
    @DisplayName("getByUsername: denied for someone else's profile")
    void getForeignProfileDenied() {
        assertThatThrownBy(() -> userService.getByUsername("bob"))
                .isInstanceOf(AuthorizationDeniedException.class);

        verify(userRepository, never()).findByUsername("bob");
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    @DisplayName("getByUsername: ADMIN may read anyone's profile")
    void getForeignProfileAsAdmin() {
        given(userRepository.findByUsername("bob")).willReturn(Optional.of(user(2L, "bob", Role.USER)));

        assertThat(userService.getByUsername("bob").getUsername()).isEqualTo("bob");
    }

    @Test
    @WithMockUser(username = "alice", roles = "USER")
    @DisplayName("getCurrentUser: needs no annotation — it can only ever return the caller's own row")
    void getCurrentUserIsAlwaysAllowed() {
        given(userRepository.findByUsername("alice")).willReturn(Optional.of(user(1L, "alice", Role.USER)));

        assertThat(userService.getCurrentUser().getUsername()).isEqualTo("alice");
    }

    @Test
    @WithMockUser(username = "alice", roles = "USER")
    @DisplayName("getAllUsers: denied to a plain USER (defense in depth behind the URL rule)")
    void getAllUsersDeniedForUser() {
        assertThatThrownBy(() -> userService.getAllUsers())
                .isInstanceOf(AuthorizationDeniedException.class);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    @DisplayName("getAllUsers: allowed for ADMIN")
    void getAllUsersAllowedForAdmin() {
        given(userRepository.findAll()).willReturn(List.of(user(1L, "alice", Role.USER)));

        assertThat(userService.getAllUsers()).hasSize(1);
    }
}
