package com.example._8_method_security.service;

import org.springframework.data.domain.Pageable;

import com.example._8_method_security.dto.PagedResponse;
import com.example._8_method_security.dto.ProjectRequestDto;
import com.example._8_method_security.dto.ProjectResponseDto;

public interface ProjectService {
    ProjectResponseDto createProject(ProjectRequestDto request, String ownerUsername);

    PagedResponse<ProjectResponseDto> getAllProjects(Pageable pageable);

    ProjectResponseDto getProjectById(Long id);

    ProjectResponseDto updateProject(Long id, ProjectRequestDto request);

    void deleteProject(Long id);
}
