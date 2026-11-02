package com.example._7_jwt.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example._7_jwt.config.JwtProperties;

@DisplayName("JwtService")
class JwtServiceTest {

    private JwtProperties props(Duration accessTtl) {
        JwtProperties p = new JwtProperties();
        // The default secret is a valid Base64 HS256 key (>= 256 bits); only vary the TTL.
        p.setAccessTokenTtl(accessTtl);
        return p;
    }

    private JwtService service() {
        return new JwtService(props(Duration.ofMinutes(15)));
    }

    /**
     * Corrupt a token's signature so verification must fail.
     *
     * <p>Note the deliberate choice of WHICH character to change. An HS256 signature is 32 bytes,
     * which base64url-encodes to 43 unpadded characters — so the LAST character carries only 4
     * significant bits and its low 2 bits are ignored padding. Flipping the last character between
     * 'A' (000000) and 'B' (000001) therefore decodes to the very same 32 bytes and the token stays
     * VALID. We change the FIRST character of the signature segment instead, where all 6 bits are
     * significant, which always alters the decoded signature.
     */
    private String tamper(String token) {
        int lastDot = token.lastIndexOf('.');
        char first = token.charAt(lastDot + 1);
        char replacement = (first == 'a') ? 'b' : 'a';
        return token.substring(0, lastDot + 1) + replacement + token.substring(lastDot + 2);
    }

    @Test
    @DisplayName("issue then parse round-trips the subject and the roles claim, and is valid")
    void roundTrip() {
        JwtService service = service();

        String token = service.generateAccessToken("alice", List.of("USER", "ADMIN"));

        assertThat(service.isValid(token)).isTrue();
        assertThat(service.extractUsername(token)).isEqualTo("alice");
        assertThat(service.extractRoles(token)).containsExactly("USER", "ADMIN");
        assertThat(service.extractExpiration(token)).isAfter(java.time.Instant.now());
    }

    @Test
    @DisplayName("exposes the access-token TTL in seconds")
    void ttlSeconds() {
        assertThat(service().getAccessTokenTtlSeconds()).isEqualTo(900L);
    }

    @Test
    @DisplayName("rejects an expired token (negative TTL makes it expire immediately)")
    void rejectsExpired() {
        JwtService expiring = new JwtService(props(Duration.ofSeconds(-60)));

        String token = expiring.generateAccessToken("alice", List.of("USER"));

        assertThat(expiring.isValid(token)).isFalse();
    }

    @Test
    @DisplayName("rejects a token whose signature was tampered with")
    void rejectsTampered() {
        JwtService service = service();
        String token = service.generateAccessToken("alice", List.of("USER"));

        assertThat(service.isValid(tamper(token))).isFalse();
    }

    @Test
    @DisplayName("rejects a token signed with a different secret (wrong key)")
    void rejectsForeignKey() {
        String foreignToken = service().generateAccessToken("mallory", List.of("ADMIN"));

        JwtProperties other = new JwtProperties();
        other.setSecret("Zm9yZWlnbi1zZWNyZXQtdGhhdC1pcy1sb25nLWVub3VnaC1mb3ItSFMyNTYtMzg0Yml0cw==");
        JwtService differentKey = new JwtService(other);

        assertThat(differentKey.isValid(foreignToken)).isFalse();
    }
}
