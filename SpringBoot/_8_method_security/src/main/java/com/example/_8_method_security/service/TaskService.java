package com.example._8_method_security.service;

import org.springframework.data.domain.Pageable;

import com.example._8_method_security.dto.PagedResponse;
import com.example._8_method_security.dto.TaskRequestDto;
import com.example._8_method_security.dto.TaskResponseDto;
import com.example._8_method_security.model.Status;

public interface TaskService {
    TaskResponseDto createTask(TaskRequestDto request);

    PagedResponse<TaskResponseDto> getTasks(Status status, Long assigneeId, Pageable pageable);

    PagedResponse<TaskResponseDto> getTasksByProject(Long projectId, Pageable pageable);

    TaskResponseDto getTaskById(Long id);

    TaskResponseDto updateTask(Long id, TaskRequestDto request);

    void deleteTask(Long id);
}
