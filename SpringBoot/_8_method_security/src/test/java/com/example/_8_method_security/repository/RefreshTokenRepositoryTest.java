package com.example._8_method_security.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import com.example._8_method_security.model.RefreshToken;
import com.example._8_method_security.model.Role;
import com.example._8_method_security.model.User;

@DataJpaTest
@DisplayName("RefreshTokenRepository")
class RefreshTokenRepositoryTest {

    @Autowired private RefreshTokenRepository refreshTokenRepository;
    @Autowired private TestEntityManager em;

    private User alice;

    @BeforeEach
    void seed() {
        alice = em.persist(User.builder()
                .username("alice").email("alice@taskflow.io").password("bcrypt-hash")
                .roles(Set.of(Role.USER))
                .build());
        em.persist(RefreshToken.builder()
                .token("token-abc").user(alice)
                .expiresAt(Instant.now().plusSeconds(3600)).revoked(false)
                .build());
        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("findByToken returns the row (with its owning user) when present")
    void findByTokenPresent() {
        Optional<RefreshToken> found = refreshTokenRepository.findByToken("token-abc");

        assertThat(found).isPresent();
        assertThat(found.get().isRevoked()).isFalse();
        assertThat(found.get().getUser().getUsername()).isEqualTo("alice");
        assertThat(found.get().getExpiresAt()).isAfter(Instant.now());
    }

    @Test
    @DisplayName("findByToken returns empty when the token is unknown")
    void findByTokenAbsent() {
        assertThat(refreshTokenRepository.findByToken("nope")).isEmpty();
    }

    @Test
    @DisplayName("a persisted token can be marked revoked and read back")
    void revokeRoundTrip() {
        RefreshToken token = refreshTokenRepository.findByToken("token-abc").orElseThrow();
        token.setRevoked(true);
        refreshTokenRepository.saveAndFlush(token);
        em.clear();

        assertThat(refreshTokenRepository.findByToken("token-abc").orElseThrow().isRevoked()).isTrue();
    }
}
