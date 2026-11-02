package com.example._9_docker.security;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.crypto.SecretKey;

import com.example._9_docker.config.JwtProperties;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

public class JwtService {

    private static final String ROLES_CLAIM = "roles";

    private final SecretKey key;
    private final Duration accessTokenTtl;

    public JwtService(JwtProperties properties) {
        // Keys.hmacShaKeyFor validates the key is long enough for HMAC-SHA (>= 256 bits
        // for HS256).
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(properties.getSecret()));
        this.accessTokenTtl = properties.getAccessTokenTtl();
    }

    /**
     * Issue a signed access token: subject = username, a roles claim, plus iat and
     * exp.
     */
    public String generateAccessToken(String username, Collection<String> roles) {
        Instant now = Instant.now();
        Instant expiry = now.plus(accessTokenTtl);
        return Jwts.builder()
                .subject(username)
                .claim(ROLES_CLAIM, List.copyOf(roles))
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Parse and cryptographically verify a token. Throws (subclasses of
     * {@link JwtException}) on a
     * tampered signature, an expired token, or malformed input — the caller decides
     * how to react.
     */
    private Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * True only if the token verifies against our key and has not expired. Never
     * throws.
     */
    public boolean isValid(String token) {
        try {
            parse(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    public String extractUsername(String token) {
        return parse(token).getSubject();
    }

    public List<String> extractRoles(String token) {
        Object roles = parse(token).get(ROLES_CLAIM);
        if (roles instanceof Collection<?> collection) {
            return collection.stream().map(String::valueOf).toList();
        }
        return List.of();
    }

    public Instant extractExpiration(String token) {
        return parse(token).getExpiration().toInstant();
    }

    public long getAccessTokenTtlSeconds() {
        return accessTokenTtl.toSeconds();
    }
}
