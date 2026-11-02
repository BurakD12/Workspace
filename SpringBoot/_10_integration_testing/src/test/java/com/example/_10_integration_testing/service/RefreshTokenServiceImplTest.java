package com.example._10_integration_testing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example._10_integration_testing.config.JwtProperties;
import com.example._10_integration_testing.exception.InvalidTokenException;
import com.example._10_integration_testing.model.RefreshToken;
import com.example._10_integration_testing.model.Role;
import com.example._10_integration_testing.model.User;
import com.example._10_integration_testing.repository.RefreshTokenRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("RefreshTokenServiceImpl")
class RefreshTokenServiceImplTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    private final JwtProperties jwtProperties = new JwtProperties(); // refresh TTL default = 7 days
    private RefreshTokenServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new RefreshTokenServiceImpl(refreshTokenRepository, jwtProperties);
    }

    private User alice() {
        return User.builder()
                .id(1L).username("alice").email("alice@stockflow.io").password("bcrypt-hash")
                .roles(Set.of(Role.CLERK))
                .build();
    }

    private RefreshToken row(String token, Instant expiresAt, boolean revoked) {
        return RefreshToken.builder()
                .id(1L).token(token).user(alice()).expiresAt(expiresAt).revoked(revoked)
                .build();
    }

    @Test
    @DisplayName("create persists a non-revoked token with a UUID value and a future expiry")
    void createPersistsToken() {
        given(refreshTokenRepository.save(any(RefreshToken.class))).willAnswer(inv -> inv.getArgument(0));

        RefreshToken created = service.create(alice());

        assertThat(created.getToken()).isNotBlank();
        assertThat(created.isRevoked()).isFalse();
        assertThat(created.getUser().getUsername()).isEqualTo("alice");
        // ~7 days out (allow slack): comfortably in the future.
        assertThat(created.getExpiresAt()).isAfter(Instant.now().plusSeconds(6 * 24 * 3600L));
    }

    @Test
    @DisplayName("verify returns the row when it is present, unrevoked, and unexpired")
    void verifyValid() {
        RefreshToken valid = row("good", Instant.now().plusSeconds(3600), false);
        given(refreshTokenRepository.findByToken("good")).willReturn(Optional.of(valid));

        assertThat(service.verify("good")).isSameAs(valid);
    }

    @Test
    @DisplayName("verify throws when the token is unknown")
    void verifyUnknown() {
        given(refreshTokenRepository.findByToken("ghost")).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.verify("ghost"))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("not recognized");
    }

    @Test
    @DisplayName("verify throws when the token is revoked")
    void verifyRevoked() {
        given(refreshTokenRepository.findByToken("revoked"))
                .willReturn(Optional.of(row("revoked", Instant.now().plusSeconds(3600), true)));

        assertThatThrownBy(() -> service.verify("revoked"))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("revoked");
    }

    @Test
    @DisplayName("verify throws when the token is expired")
    void verifyExpired() {
        given(refreshTokenRepository.findByToken("stale"))
                .willReturn(Optional.of(row("stale", Instant.now().minusSeconds(60), false)));

        assertThatThrownBy(() -> service.verify("stale"))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("expired");
    }

    @Test
    @DisplayName("rotate revokes the presented token and returns a brand-new token for the same user")
    void rotateRevokesOldAndIssuesNew() {
        RefreshToken old = row("old-token", Instant.now().plusSeconds(3600), false);
        given(refreshTokenRepository.findByToken("old-token")).willReturn(Optional.of(old));
        given(refreshTokenRepository.save(any(RefreshToken.class))).willAnswer(inv -> inv.getArgument(0));

        RefreshToken rotated = service.rotate("old-token");

        // The presented token was revoked...
        assertThat(old.isRevoked()).isTrue();
        // ...and a genuinely different token was issued for the same user.
        assertThat(rotated.getToken()).isNotEqualTo("old-token");
        assertThat(rotated.isRevoked()).isFalse();
        assertThat(rotated.getUser().getUsername()).isEqualTo("alice");

        // save() called twice: once to persist the revocation, once to persist the new
        // token.
        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        assertThat(captor.getAllValues()).anyMatch(RefreshToken::isRevoked);
    }

    @Test
    @DisplayName("rotate throws (and issues nothing) when the presented token is invalid")
    void rotateRejectsInvalid() {
        given(refreshTokenRepository.findByToken("bad")).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.rotate("bad"))
                .isInstanceOf(InvalidTokenException.class);

        verify(refreshTokenRepository, org.mockito.Mockito.never()).save(any(RefreshToken.class));
    }
}
