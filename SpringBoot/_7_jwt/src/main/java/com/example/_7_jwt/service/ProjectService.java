package com.example._7_jwt.service;

import org.springframework.data.domain.Pageable;

import com.example._7_jwt.dto.PagedResponse;
import com.example._7_jwt.dto.ProjectRequestDto;
import com.example._7_jwt.dto.ProjectResponseDto;

public interface ProjectService {
    ProjectResponseDto createProject(ProjectRequestDto request, String ownerUsername);
    PagedResponse<ProjectResponseDto> getAllProjects(Pageable pageable);
    ProjectResponseDto getProjectById(Long id);
    ProjectResponseDto updateProject(Long id, ProjectRequestDto request);
    void deleteProject(Long id);
}
