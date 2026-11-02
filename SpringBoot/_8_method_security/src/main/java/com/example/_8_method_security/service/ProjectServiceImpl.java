package com.example._8_method_security.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example._8_method_security.dto.PagedResponse;
import com.example._8_method_security.dto.ProjectRequestDto;
import com.example._8_method_security.dto.ProjectResponseDto;
import com.example._8_method_security.dto.UserSummaryDto;
import com.example._8_method_security.exception.ResourceNotFoundException;
import com.example._8_method_security.model.Project;
import com.example._8_method_security.model.User;
import com.example._8_method_security.repository.ProjectRepository;
import com.example._8_method_security.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ProjectServiceImpl implements ProjectService {
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private static final String ROLE_ADMIN = "ROLE_ADMIN";

    private static boolean isAdmin(Authentication authentication) {
        return authentication != null && 
        authentication.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .allMatch(ROLE_ADMIN::equals);
    }

    private static String usernameOf(Authentication authentication) {
        return authentication == null ? "" : authentication.getName();
    }

    @Override
    @PreAuthorize("isAuthenticated() and #ownerUsername == authentication.name")
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
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Page<Project> page = isAdmin(authentication) 
                    ? projectRepository.findAll(pageable)
                    : projectRepository.findByOwnerUsername(usernameOf(authentication), pageable);
        return PagedResponse.from(page, page.getContent().stream().map(this::toResponse).toList());
    }

    @Override
    @PostAuthorize("hasRole('ADMIN') or returnObject.owner.username == authentication.name")
    public ProjectResponseDto getProjectById(Long id) {
        return toResponse(getEntity(id));
    }

    @Override
    @PreAuthorize("hasRole('ADMIN') or @taskFlowPermissions.isProjectOwner(#id, authentication)")
    public ProjectResponseDto updateProject(Long id, ProjectRequestDto request) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project", id));
        project.setName(request.getName());
        project.setDescription(request.getDescription());
        Project savedProject = projectRepository.save(project);
        return toResponse(savedProject);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN') or @taskFlowPermissions.isProjectOwner(#id, authentication)")
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
