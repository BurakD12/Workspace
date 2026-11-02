package com.example._8_method_security.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example._8_method_security.dto.LoginRequest;
import com.example._8_method_security.dto.RefreshRequest;
import com.example._8_method_security.dto.RegisterRequest;
import com.example._8_method_security.dto.TokenResponse;
import com.example._8_method_security.dto.UserResponse;
import com.example._8_method_security.exception.DuplicateResourceException;
import com.example._8_method_security.exception.InvalidTokenException;
import com.example._8_method_security.model.RefreshToken;
import com.example._8_method_security.model.Role;
import com.example._8_method_security.model.User;
import com.example._8_method_security.repository.UserRepository;
import com.example._8_method_security.security.JwtService;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthServiceImpl")
class AuthServiceImplTest {

    @Mock private UserRepository userRepository;
    // Real encoder so matches(...) proves a genuine BCrypt hash was stored.
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtService jwtService;
    @Mock private RefreshTokenService refreshTokenService;
    private AuthServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AuthServiceImpl(userRepository, passwordEncoder,
                authenticationManager, jwtService, refreshTokenService);
    }

    private RegisterRequest request() {
        return new RegisterRequest("alice", "alice@taskflow.io", "password123");
    }

    // ---------- register (Lab 6 base behaviour, unchanged) ----------

    @Test
    @DisplayName("register assigns default role USER and returns a UserResponse with id/username/email")
    void registerSuccess() {
        given(userRepository.existsByUsername("alice")).willReturn(false);
        given(userRepository.existsByEmail("alice@taskflow.io")).willReturn(false);
        given(userRepository.save(any(User.class))).willAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });

        UserResponse res = service.register(request());

        assertThat(res.getId()).isEqualTo(1L);
        assertThat(res.getUsername()).isEqualTo("alice");
        assertThat(res.getEmail()).isEqualTo("alice@taskflow.io");
        assertThat(res.getRoles()).containsExactly(Role.USER);
    }

    @Test
    @DisplayName("register stores a BCrypt hash, never the raw password")
    void registerHashesPassword() {
        given(userRepository.existsByUsername("alice")).willReturn(false);
        given(userRepository.existsByEmail("alice@taskflow.io")).willReturn(false);
        given(userRepository.save(any(User.class))).willAnswer(inv -> inv.getArgument(0));

        service.register(request());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getPassword()).isNotEqualTo("password123");
        assertThat(passwordEncoder.matches("password123", saved.getPassword())).isTrue();
    }

    @Test
    @DisplayName("register throws DuplicateResourceException (username) and does not save when the username is taken")
    void registerDuplicateUsername() {
        given(userRepository.existsByUsername("alice")).willReturn(true);

        assertThatThrownBy(() -> service.register(request()))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("username");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("register throws DuplicateResourceException (email) and does not save when the email is taken")
    void registerDuplicateEmail() {
        given(userRepository.existsByUsername("alice")).willReturn(false);
        given(userRepository.existsByEmail("alice@taskflow.io")).willReturn(true);

        assertThatThrownBy(() -> service.register(request()))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("email");

        verify(userRepository, never()).save(any(User.class));
    }

    // ---------- login (Lab 7) ----------

    private User aliceEntity() {
        return User.builder()
                .id(1L).username("alice").email("alice@taskflow.io").password("bcrypt-hash")
                .roles(Set.of(Role.USER))
                .build();
    }

    @Test
    @DisplayName("login authenticates via the AuthenticationManager, then issues an access + refresh token")
    void loginSuccess() {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                "alice", "password123", List.of(new SimpleGrantedAuthority("ROLE_USER")));
        given(authenticationManager.authenticate(any())).willReturn(authentication);
        given(jwtService.generateAccessToken(eq("alice"), any())).willReturn("access.jwt.token");
        given(userRepository.findByUsername("alice")).willReturn(Optional.of(aliceEntity()));
        given(refreshTokenService.create(any(User.class))).willReturn(refreshTokenRow("refresh-uuid-1"));
        given(jwtService.getAccessTokenTtlSeconds()).willReturn(900L);

        TokenResponse res = service.login(new LoginRequest("alice", "password123"));

        assertThat(res.getAccessToken()).isEqualTo("access.jwt.token");
        assertThat(res.getRefreshToken()).isEqualTo("refresh-uuid-1");
        assertThat(res.getTokenType()).isEqualTo("Bearer");
        assertThat(res.getExpiresIn()).isEqualTo(900L);
    }

    @Test
    @DisplayName("login puts BARE role names (no ROLE_ prefix) into the token's roles claim")
    void loginStripsRolePrefixForClaim() {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                "alice", "password123", List.of(new SimpleGrantedAuthority("ROLE_USER")));
        given(authenticationManager.authenticate(any())).willReturn(authentication);
        given(jwtService.generateAccessToken(eq("alice"), any())).willReturn("access.jwt.token");
        given(userRepository.findByUsername("alice")).willReturn(Optional.of(aliceEntity()));
        given(refreshTokenService.create(any(User.class))).willReturn(refreshTokenRow("refresh-uuid-1"));

        service.login(new LoginRequest("alice", "password123"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<String>> rolesCaptor = ArgumentCaptor.forClass(List.class);
        verify(jwtService).generateAccessToken(eq("alice"), rolesCaptor.capture());
        assertThat(rolesCaptor.getValue()).containsExactly("USER");
    }

    @Test
    @DisplayName("login propagates BadCredentialsException and issues no tokens on bad credentials")
    void loginBadCredentials() {
        given(authenticationManager.authenticate(any()))
                .willThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> service.login(new LoginRequest("alice", "wrong")))
                .isInstanceOf(BadCredentialsException.class);

        verifyNoInteractions(jwtService, refreshTokenService);
    }

    // ---------- refresh (Lab 7) ----------

    @Test
    @DisplayName("refresh rotates the stored token and mints a NEW access token for the same user")
    void refreshRotates() {
        RefreshToken rotated = refreshTokenRow("refresh-uuid-2");
        given(refreshTokenService.rotate("refresh-uuid-1")).willReturn(rotated);
        given(jwtService.generateAccessToken(eq("alice"), any())).willReturn("new.access.token");
        given(jwtService.getAccessTokenTtlSeconds()).willReturn(900L);

        TokenResponse res = service.refresh(new RefreshRequest("refresh-uuid-1"));

        assertThat(res.getAccessToken()).isEqualTo("new.access.token");
        // The response carries the ROTATED (new) refresh token, not the presented one.
        assertThat(res.getRefreshToken()).isEqualTo("refresh-uuid-2");
        assertThat(res.getTokenType()).isEqualTo("Bearer");
        assertThat(res.getExpiresIn()).isEqualTo(900L);
    }

    @Test
    @DisplayName("refresh propagates InvalidTokenException when the presented token cannot be rotated")
    void refreshInvalidToken() {
        given(refreshTokenService.rotate("bad-token"))
                .willThrow(new InvalidTokenException("Refresh token has been revoked"));

        assertThatThrownBy(() -> service.refresh(new RefreshRequest("bad-token")))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("revoked");

        verifyNoInteractions(jwtService);
    }

    private RefreshToken refreshTokenRow(String token) {
        return RefreshToken.builder()
                .id(1L).token(token).user(aliceEntity())
                .expiresAt(Instant.now().plusSeconds(3600)).revoked(false)
                .build();
    }
}
