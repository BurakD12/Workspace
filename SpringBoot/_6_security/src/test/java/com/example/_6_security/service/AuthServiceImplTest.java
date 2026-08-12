package com.example._6_security.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example._6_security.dto.RegisterRequest;
import com.example._6_security.dto.UserResponse;
import com.example._6_security.exception.DuplicateResourceException;
import com.example._6_security.model.Role;
import com.example._6_security.model.User;
import com.example._6_security.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthServiceImpl")
class AuthServiceImplTest {

    @Mock private UserRepository userRepository;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private AuthServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AuthServiceImpl(userRepository, passwordEncoder);
    }

    @Test
    @DisplayName("register hashes the password, assigns default role USER, and returns a UserResponse")
    void registerSuccess() {
        given(userRepository.existsByUsername("alice")).willReturn(false);
        given(userRepository.save(any(User.class))).willAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });

        UserResponse res = service.register(new RegisterRequest("alice", "password123"));

        assertThat(res.getId()).isEqualTo(1L);
        assertThat(res.getUsername()).isEqualTo("alice");
        assertThat(res.getRoles()).containsExactly(Role.USER);
    }

    @Test
    @DisplayName("register stores a BCrypt hash, never the raw password")
    void registerHashesPassword() {
        given(userRepository.existsByUsername("alice")).willReturn(false);
        given(userRepository.save(any(User.class))).willAnswer(inv -> inv.getArgument(0));

        service.register(new RegisterRequest("alice", "password123"));

        org.mockito.ArgumentCaptor<User> captor = org.mockito.ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getPassword()).isNotEqualTo("password123");
        assertThat(passwordEncoder.matches("password123", saved.getPassword())).isTrue();
    }

    @Test
    @DisplayName("register throws DuplicateResourceException and does not save when username exists")
    void registerDuplicate() {
        given(userRepository.existsByUsername("alice")).willReturn(true);

        assertThatThrownBy(() -> service.register(new RegisterRequest("alice", "password123")))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("username");

        verify(userRepository, never()).save(any(User.class));
    }
}
