package com.example._8_method_security.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.example._8_method_security.dto.PagedResponse;
import com.example._8_method_security.dto.TaskRequestDto;
import com.example._8_method_security.dto.TaskResponseDto;
import com.example._8_method_security.exception.ResourceNotFoundException;
import com.example._8_method_security.model.Priority;
import com.example._8_method_security.model.Project;
import com.example._8_method_security.model.Role;
import com.example._8_method_security.model.Status;
import com.example._8_method_security.model.Task;
import com.example._8_method_security.model.User;
import com.example._8_method_security.repository.ProjectRepository;
import com.example._8_method_security.repository.TaskRepository;
import com.example._8_method_security.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("TaskServiceImpl")
class TaskServiceImplTest {

    @Mock private TaskRepository taskRepository;
    @Mock private ProjectRepository projectRepository;
    @Mock private UserRepository userRepository;
    @InjectMocks private TaskServiceImpl service;

    private Project project() {
        return Project.builder().id(1L).name("Apollo")
                .owner(User.builder().id(7L).username("alice").email("a@x").password("h").build())
                .build();
    }

    private User assignee() {
        return User.builder().id(2L).username("bob").email("b@x").password("h")
                .roles(Set.of(Role.USER)).build();
    }

    private TaskRequestDto request() {
        return TaskRequestDto.builder()
                .title("Design").description("d").status(Status.IN_PROGRESS).priority(Priority.HIGH)
                .projectId(1L).assigneeId(2L).build();
    }

    @Test
    @DisplayName("createTask resolves project + assignee, saves, and returns a fully nested response")
    void createHappyPath() {
        given(projectRepository.findById(1L)).willReturn(Optional.of(project()));
        given(userRepository.findById(2L)).willReturn(Optional.of(assignee()));
        given(taskRepository.save(any(Task.class))).willAnswer(inv -> {
            Task t = inv.getArgument(0);
            t.setId(50L);
            return t;
        });

        TaskResponseDto res = service.createTask(request());

        assertThat(res.getId()).isEqualTo(50L);
        assertThat(res.getTitle()).isEqualTo("Design");
        assertThat(res.getStatus()).isEqualTo(Status.IN_PROGRESS);
        assertThat(res.getPriority()).isEqualTo(Priority.HIGH);
        assertThat(res.getProject().getName()).isEqualTo("Apollo");
        assertThat(res.getAssignee().getUsername()).isEqualTo("bob");
    }

    @Test
    @DisplayName("createTask defaults status/priority to TODO/MEDIUM and allows a null assignee")
    void createAppliesDefaults() {
        given(projectRepository.findById(1L)).willReturn(Optional.of(project()));
        given(taskRepository.save(any(Task.class))).willAnswer(inv -> inv.getArgument(0));

        TaskRequestDto req = TaskRequestDto.builder().title("Unspecified").projectId(1L).build();
        TaskResponseDto res = service.createTask(req);

        assertThat(res.getStatus()).isEqualTo(Status.TODO);
        assertThat(res.getPriority()).isEqualTo(Priority.MEDIUM);
        assertThat(res.getAssignee()).isNull();
        verify(userRepository, never()).findById(any());
    }

    @Test
    @DisplayName("createTask throws ResourceNotFoundException when the project id is unknown")
    void createUnknownProject() {
        given(projectRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.createTask(request()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Project");
        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    @DisplayName("createTask throws ResourceNotFoundException when a non-null assignee id is unknown")
    void createUnknownAssignee() {
        given(projectRepository.findById(1L)).willReturn(Optional.of(project()));
        given(userRepository.findById(2L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.createTask(request()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User");
        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    @DisplayName("getTaskById throws ResourceNotFoundException when missing")
    void getByIdMissing() {
        given(taskRepository.findById(9999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.getTaskById(9999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Task");
    }

    @Test
    @DisplayName("getTasks filters by status when a status is supplied")
    void getTasksByStatus() {
        Task t = Task.builder().id(1L).title("T").status(Status.DONE).priority(Priority.LOW)
                .project(project()).build();
        given(taskRepository.findByStatus(Status.DONE, PageRequest.of(0, 20)))
                .willReturn(new PageImpl<>(List.of(t), PageRequest.of(0, 20), 1));

        PagedResponse<TaskResponseDto> res = service.getTasks(Status.DONE, null, PageRequest.of(0, 20));

        assertThat(res.getContent()).hasSize(1);
        assertThat(res.getContent().get(0).getStatus()).isEqualTo(Status.DONE);
        verify(taskRepository, never()).findAll(any(PageRequest.class));
    }

    @Test
    @DisplayName("getTasks filters by assignee when no status but an assigneeId is supplied")
    void getTasksByAssignee() {
        Task t = Task.builder().id(1L).title("T").status(Status.TODO).priority(Priority.LOW)
                .project(project()).assignee(assignee()).build();
        given(taskRepository.findByAssigneeId(2L, PageRequest.of(0, 20)))
                .willReturn(new PageImpl<>(List.of(t), PageRequest.of(0, 20), 1));

        PagedResponse<TaskResponseDto> res = service.getTasks(null, 2L, PageRequest.of(0, 20));

        assertThat(res.getContent().get(0).getAssignee().getUsername()).isEqualTo("bob");
    }

    @Test
    @DisplayName("getTasks with no filters delegates to findAll")
    void getTasksNoFilter() {
        given(taskRepository.findAll(PageRequest.of(0, 20)))
                .willReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        PagedResponse<TaskResponseDto> res = service.getTasks(null, null, PageRequest.of(0, 20));

        assertThat(res.getContent()).isEmpty();
        verify(taskRepository, never()).findByStatus(any(), any());
        verify(taskRepository, never()).findByAssigneeId(any(), any());
    }

    @Test
    @DisplayName("getTasksByProject throws ResourceNotFoundException when the project does not exist")
    void getTasksByUnknownProject() {
        given(projectRepository.existsById(9999L)).willReturn(false);

        assertThatThrownBy(() -> service.getTasksByProject(9999L, PageRequest.of(0, 20)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Project");
    }

    @Test
    @DisplayName("updateTask updates fields and re-resolves the relationships")
    void updateTask() {
        Task existing = Task.builder().id(5L).title("Old").status(Status.TODO).priority(Priority.LOW)
                .project(project()).build();
        given(taskRepository.findById(5L)).willReturn(Optional.of(existing));
        given(projectRepository.findById(1L)).willReturn(Optional.of(project()));
        given(userRepository.findById(2L)).willReturn(Optional.of(assignee()));
        given(taskRepository.save(any(Task.class))).willAnswer(inv -> inv.getArgument(0));

        TaskResponseDto res = service.updateTask(5L, request());

        assertThat(res.getTitle()).isEqualTo("Design");
        assertThat(res.getStatus()).isEqualTo(Status.IN_PROGRESS);
        assertThat(res.getAssignee().getUsername()).isEqualTo("bob");
    }
}
