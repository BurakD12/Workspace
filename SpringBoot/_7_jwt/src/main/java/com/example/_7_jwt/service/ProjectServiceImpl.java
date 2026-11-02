package com.example._7_jwt.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example._7_jwt.dto.PagedResponse;
import com.example._7_jwt.dto.ProjectRequestDto;
import com.example._7_jwt.dto.ProjectResponseDto;
import com.example._7_jwt.dto.UserSummaryDto;
import com.example._7_jwt.exception.ResourceNotFoundException;
import com.example._7_jwt.model.Project;
import com.example._7_jwt.model.User;
import com.example._7_jwt.repository.ProjectRepository;
import com.example._7_jwt.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ProjectServiceImpl implements ProjectService {
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    @Override
    public ProjectResponseDto createProject(ProjectRequestDto request, String ownerUsername) {
        User user = userRepository.findByUsername(ownerUsername)
                .orElseThrow(() -> new ResourceNotFoundException("User", ownerUsername));
        Project project = Project.builder()
                .name(request.getName())
                .description(request.getDescription())
                .owner(user)
                .build();
        Project savedProject = projectRepository.save(project);
        return toResponse(savedProject);
    }

    @Override
    public PagedResponse<ProjectResponseDto> getAllProjects(Pageable pageable) {
        Page<Project> page = projectRepository.findAll(pageable);
        return PagedResponse.from(page, page.getContent().stream().map(this::toResponse).toList());
    }

    @Override
    public ProjectResponseDto getProjectById(Long id) {
        return toResponse(getEntity(id));
    }

    @Override
    public ProjectResponseDto updateProject(Long id, ProjectRequestDto request) {
        Project project = projectRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Project", id));
        project.setName(request.getName());
        project.setDescription(request.getDescription());
        Project savedProject = projectRepository.save(project);
        return toResponse(savedProject);
    }

    @Override
    public void deleteProject(Long id) {
        if (!projectRepository.existsById(id)) {
            throw new ResourceNotFoundException("Project", id);
        } 
        projectRepository.deleteById(id);
    }

    private Project getEntity(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project", id));
    }
    
    private ProjectResponseDto toResponse(Project project) {
        User owner = project.getOwner();
        UserSummaryDto ownerDto = owner == null ? null : new UserSummaryDto(owner.getId(), owner.getUsername());

        return ProjectResponseDto.builder()
            .id(project.getId())
            .name(project.getName())
            .description(project.getDescription())
            .owner(ownerDto)
            .build();
    }
}
