package com.example._9_docker.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.example._9_docker.model.Role;
import com.example._9_docker.model.User;
import com.example._9_docker.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserDetailsServiceImpl")
class UserDetailsServiceImplTest {

    @Mock private UserRepository userRepository;
    @InjectMocks private UserDetailsServiceImpl userDetailsService;

    @Test
    @DisplayName("loads a user and exposes each role as a ROLE_ authority, carrying the stored hash")
    void loadsUserWithRoleAuthorities() {
        User user = User.builder()
                .id(1L).username("bob").email("bob@x.io").password("bcrypt-hash")
                .roles(Set.of(Role.CLERK, Role.ADMIN))
                .build();
        given(userRepository.findByUsername("bob")).willReturn(Optional.of(user));

        UserDetails details = userDetailsService.loadUserByUsername("bob");

        assertThat(details.getUsername()).isEqualTo("bob");
        assertThat(details.getPassword()).isEqualTo("bcrypt-hash");
        assertThat(details.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_CLERK", "ROLE_ADMIN");
    }

    @Test
    @DisplayName("throws UsernameNotFoundException when the user is absent")
    void throwsWhenMissing() {
        given(userRepository.findByUsername("ghost")).willReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("ghost"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("ghost");
    }
}
