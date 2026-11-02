package com.example._8_method_security.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import com.example._8_method_security.dto.PagedResponse;
import com.example._8_method_security.dto.ProjectSummaryDto;
import com.example._8_method_security.dto.TaskRequestDto;
import com.example._8_method_security.dto.TaskResponseDto;
import com.example._8_method_security.dto.UserSummaryDto;
import com.example._8_method_security.exception.ResourceNotFoundException;
import com.example._8_method_security.model.Priority;
import com.example._8_method_security.model.Project;
import com.example._8_method_security.model.Status;
import com.example._8_method_security.model.Task;
import com.example._8_method_security.model.User;
import com.example._8_method_security.repository.ProjectRepository;
import com.example._8_method_security.repository.TaskRepository;
import com.example._8_method_security.repository.UserRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class TaskServiceImpl implements TaskService {
    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    @Override
    @PreAuthorize("hasRole('ADMIN') or @taskFlowPermissions.isProjectOwner(#request.projectId, authentication)")
    public TaskResponseDto createTask(TaskRequestDto request) {
        Project project = resolveProject(request.getProjectId());
        User assignee = resolveAssignee(request.getAssigneeId());
        Task task = Task.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .status(request.getStatus() != null ? request.getStatus() : Status.TODO)
                .priority(request.getPriority() != null ? request.getPriority() : Priority.MEDIUM)
                .dueDate(request.getDueDate())
                .project(project)
                .assignee(assignee)
                .build();

        Task savedTask = taskRepository.save(task);
        return toResponse(savedTask);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public PagedResponse<TaskResponseDto> getTasks(Status status, Long assigneeId, Pageable pageable) {
        Page<Task> page;
        if (status != null) {
            page = taskRepository.findByStatus(status, pageable);
        } else if (assigneeId != null) {
            page = taskRepository.findByAssigneeId(assigneeId, pageable);
        } else {
            page = taskRepository.findAll(pageable);
        }
        return toPaged(page);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN') or @taskFlowPermissions.isProjectOwner(#projectId, authentication)")
    public PagedResponse<TaskResponseDto> getTasksByProject(Long projectId, Pageable pageable) {
        if (!projectRepository.existsById(projectId)) {
            throw new ResourceNotFoundException("Project", projectId);
        }

        return toPaged(taskRepository.findByProjectId(projectId, pageable));
    }

    @Override
    @PreAuthorize("hasRole('ADMIN') or @taskFlowPermissions.canWorkOnTask(#id, authentication)")
    public TaskResponseDto getTaskById(Long id) {
        return toResponse(getTask(id));
    }

    @Override
    @PreAuthorize("hasRole('ADMIN') or @taskFlowPermissions.canWorkOnTask(#id, authentication)" + 
    " and @taskFlowPermissions.canPlaceTaskInProject(#id, #request.projectId, authentication)")
    public TaskResponseDto updateTask(Long id, TaskRequestDto request) {
        Task task = getTask(id);
        Project project = resolveProject(request.getProjectId());
        User assignee = resolveAssignee(request.getAssigneeId());
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setStatus(request.getStatus() != null ? request.getStatus() : Status.TODO);
        task.setPriority(request.getPriority());
        task.setDueDate(request.getDueDate());
        task.setAssignee(assignee);
        task.setProject(project);
        taskRepository.save(task);
        return toResponse(task);
    }

    private Project resolveProject(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", projectId));
    }

    private User resolveAssignee(Long assigneeId) {
        if (assigneeId == null) {
            return null;
        }
        return userRepository.findById(assigneeId)
                .orElseThrow(() -> new ResourceNotFoundException("User", assigneeId));
    }

    private TaskResponseDto toResponse(Task task) {
        Project project = task.getProject();
        ProjectSummaryDto projectSummary = new ProjectSummaryDto(project.getId(), project.getName());

        User assignee = task.getAssignee();
        UserSummaryDto assigneeSummary = assignee == null ? null
                : new UserSummaryDto(assignee.getId(), assignee.getUsername());

        return new TaskResponseDto(task.getId(), task.getTitle(), task.getDescription(), task.getStatus(),
                task.getPriority(), task.getDueDate(), projectSummary, assigneeSummary);

    }

    private PagedResponse<TaskResponseDto> toPaged(Page<Task> page) {
        return PagedResponse.from(page, page.getContent().stream().map(this::toResponse).toList());
    }

    @Override
    @PreAuthorize("hasRole('ADMIN') or @taskFlowPermissions.isProjectOwnerOfTask(#id, authentication)")
    public void deleteTask(Long id) {
        if (!taskRepository.existsById(id)) {
            throw new ResourceNotFoundException("Task", id);
        }
        taskRepository.deleteById(id);
    }

    private Task getTask(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task", id));
    }
}
