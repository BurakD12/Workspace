package com.example._10_integration_testing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.example._10_integration_testing.dto.UserResponse;
import com.example._10_integration_testing.exception.ResourceNotFoundException;
import com.example._10_integration_testing.model.Role;
import com.example._10_integration_testing.model.User;
import com.example._10_integration_testing.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserServiceImpl")
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @InjectMocks
    private UserServiceImpl service;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private User alice() {
        return User.builder()
                .id(1L).username("alice").email("alice@stockflow.io").password("hash")
                .roles(Set.of(Role.CLERK))
                .build();
    }

    @Test
    @DisplayName("getByUsername maps the user to a UserResponse")
    void getByUsernamePresent() {
        given(userRepository.findByUsername("alice")).willReturn(Optional.of(alice()));

        UserResponse res = service.getByUsername("alice");

        assertThat(res.getId()).isEqualTo(1L);
        assertThat(res.getUsername()).isEqualTo("alice");
        assertThat(res.getEmail()).isEqualTo("alice@stockflow.io");
        assertThat(res.getRoles()).containsExactly(Role.CLERK);
    }

    @Test
    @DisplayName("getByUsername throws ResourceNotFoundException when absent")
    void getByUsernameAbsent() {
        given(userRepository.findByUsername("ghost")).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.getByUsername("ghost"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("ghost");
    }

    @Test
    @DisplayName("getCurrentUser resolves the principal from the SecurityContext")
    void getCurrentUser() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("alice", "n/a", List.of()));
        given(userRepository.findByUsername("alice")).willReturn(Optional.of(alice()));

        UserResponse res = service.getCurrentUser();

        assertThat(res.getUsername()).isEqualTo("alice");
    }

    @Test
    @DisplayName("getAllUsers maps every user")
    void getAllUsers() {
        User bob = User.builder().id(2L).username("bob").email("bob@x.io").password("h")
                .roles(Set.of(Role.ADMIN)).build();
        given(userRepository.findAll()).willReturn(List.of(alice(), bob));

        List<UserResponse> all = service.getAllUsers();

        assertThat(all).extracting(UserResponse::getUsername).containsExactly("alice", "bob");
    }
}
