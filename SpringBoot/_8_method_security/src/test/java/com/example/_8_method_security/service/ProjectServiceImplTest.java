package com.example._8_method_security.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import com.example._8_method_security.dto.PagedResponse;
import com.example._8_method_security.dto.ProjectRequestDto;
import com.example._8_method_security.dto.ProjectResponseDto;
import com.example._8_method_security.exception.ResourceNotFoundException;
import com.example._8_method_security.model.Project;
import com.example._8_method_security.model.Role;
import com.example._8_method_security.model.User;
import com.example._8_method_security.repository.ProjectRepository;
import com.example._8_method_security.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProjectServiceImpl")
class ProjectServiceImplTest {

    @Mock private ProjectRepository projectRepository;
    @Mock private UserRepository userRepository;
    @InjectMocks private ProjectServiceImpl service;

    private User owner() {
        return User.builder().id(7L).username("alice").email("alice@x.io").password("h")
                .roles(Set.of(Role.USER)).build();
    }

    @Test
    @DisplayName("createProject sets the owner to the given principal and returns a nested response")
    void createSetsOwner() {
        given(userRepository.findByUsername("alice")).willReturn(Optional.of(owner()));
        given(projectRepository.save(any(Project.class))).willAnswer(inv -> {
            Project p = inv.getArgument(0);
            p.setId(100L);
            return p;
        });

        ProjectResponseDto res = service.createProject(new ProjectRequestDto("Apollo", "Moon"), "alice");

        assertThat(res.getId()).isEqualTo(100L);
        assertThat(res.getName()).isEqualTo("Apollo");
        assertThat(res.getOwner().getId()).isEqualTo(7L);
        assertThat(res.getOwner().getUsername()).isEqualTo("alice");
    }

    @Test
    @DisplayName("createProject throws ResourceNotFoundException when the principal has no User row")
    void createUnknownOwner() {
        given(userRepository.findByUsername("ghost")).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.createProject(new ProjectRequestDto("Apollo", null), "ghost"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("ghost");
        verify(projectRepository, never()).save(any(Project.class));
    }

    @Test
    @DisplayName("getProjectById throws ResourceNotFoundException when missing")
    void getByIdMissing() {
        given(projectRepository.findById(9999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.getProjectById(9999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Project");
    }

    @Test
    @DisplayName("updateProject changes name/description but leaves the original owner untouched")
    void updateKeepsOwner() {
        Project existing = Project.builder().id(5L).name("Old").description("old").owner(owner()).build();
        given(projectRepository.findById(5L)).willReturn(Optional.of(existing));
        given(projectRepository.save(any(Project.class))).willAnswer(inv -> inv.getArgument(0));

        ProjectResponseDto res = service.updateProject(5L, new ProjectRequestDto("New name", "new desc"));

        assertThat(res.getName()).isEqualTo("New name");
        assertThat(res.getDescription()).isEqualTo("new desc");
        assertThat(res.getOwner().getUsername()).isEqualTo("alice");
        verify(userRepository, never()).findByUsername(any());
    }

    @Test
    @DisplayName("deleteProject throws ResourceNotFoundException when the id does not exist")
    void deleteMissing() {
        given(projectRepository.existsById(9999L)).willReturn(false);

        assertThatThrownBy(() -> service.deleteProject(9999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Project");
        verify(projectRepository, never()).deleteById(any());
    }

    // --- Lab 8 -------------------------------------------------------------------------------------
    // getAllProjects now picks its query from the principal, so these tests must place an
    // Authentication in the SecurityContextHolder themselves. @WithMockUser cannot help here: this is
    // a plain Mockito test with no Spring TestContext to run the annotation's listener.
    //
    // Note also what this class canNOT verify: the @PreAuthorize/@PostAuthorize rules on the service.
    // `service` is constructed with `new` by @InjectMocks, so there is no AOP proxy and the
    // annotations never execute — see MethodSecurityTest for the tests that actually enforce them.

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(String username, String... authorities) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(username, "n/a",
                        AuthorityUtils.createAuthorityList(authorities)));
    }

    @Test
    @DisplayName("getAllProjects scopes a USER's page to their own projects (query-level filtering)")
    void getAllPagedScopedForUser() {
        authenticateAs("alice", "ROLE_USER");
        Project p = Project.builder().id(1L).name("Apollo").owner(owner()).build();
        given(projectRepository.findByOwnerUsername(eq("alice"), any(PageRequest.class)))
                .willReturn(new PageImpl<>(List.of(p), PageRequest.of(0, 20), 1));

        PagedResponse<ProjectResponseDto> res = service.getAllProjects(PageRequest.of(0, 20));

        assertThat(res.getTotalElements()).isEqualTo(1);
        assertThat(res.isLast()).isTrue();
        assertThat(res.getContent()).hasSize(1);
        assertThat(res.getContent().get(0).getOwner().getUsername()).isEqualTo("alice");
        verify(projectRepository, never()).findAll(any(PageRequest.class));
    }

    @Test
    @DisplayName("getAllProjects returns the unscoped page for an ADMIN")
    void getAllPagedUnscopedForAdmin() {
        authenticateAs("admin", "ROLE_ADMIN");
        Project p = Project.builder().id(1L).name("Apollo").owner(owner()).build();
        given(projectRepository.findAll(any(PageRequest.class)))
                .willReturn(new PageImpl<>(List.of(p), PageRequest.of(0, 20), 1));

        PagedResponse<ProjectResponseDto> res = service.getAllProjects(PageRequest.of(0, 20));

        assertThat(res.getContent()).hasSize(1);
        verify(projectRepository, never()).findByOwnerUsername(any(), any());
    }
}
