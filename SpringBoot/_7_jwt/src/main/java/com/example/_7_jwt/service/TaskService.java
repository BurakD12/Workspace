package com.example._7_jwt.service;

import org.springframework.data.domain.Pageable;

import com.example._7_jwt.dto.PagedResponse;
import com.example._7_jwt.dto.TaskRequestDto;
import com.example._7_jwt.dto.TaskResponseDto;
import com.example._7_jwt.model.Status;

public interface TaskService {
    TaskResponseDto createTask(TaskRequestDto request);
    PagedResponse<TaskResponseDto> getTasks(Status status, Long assigneeId, Pageable pageable);
    PagedResponse<TaskResponseDto> getTasksByProject(Long projectId, Pageable pageable);
    TaskResponseDto getTaskById(Long id);
    TaskResponseDto updateTask(Long id, TaskRequestDto request);
    void deleteTask(Long id);
}
