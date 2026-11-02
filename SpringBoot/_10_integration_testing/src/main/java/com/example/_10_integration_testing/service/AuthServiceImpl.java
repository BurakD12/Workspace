package com.example._10_integration_testing.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example._10_integration_testing.dto.LoginRequest;
import com.example._10_integration_testing.dto.RefreshRequest;
import com.example._10_integration_testing.dto.RegisterRequest;
import com.example._10_integration_testing.dto.TokenResponse;
import com.example._10_integration_testing.dto.UserResponse;
import com.example._10_integration_testing.exception.DuplicateResourceException;
import com.example._10_integration_testing.exception.ResourceNotFoundException;
import com.example._10_integration_testing.model.RefreshToken;
import com.example._10_integration_testing.model.Role;
import com.example._10_integration_testing.model.User;
import com.example._10_integration_testing.repository.UserRepository;
import com.example._10_integration_testing.security.JwtService;

import lombok.RequiredArgsConstructor;

/**
 * Lab 9 (StockFlow) — registration plus the stateless JWT auth flow.
 *
 * <p>
 * <b>register</b> is unchanged from the Lab 6 base (duplicate checks, BCrypt
 * hash, CLERK role).
 *
 * <p>
 * <b>login</b> authenticates the credentials through the Lab 6
 * {@link AuthenticationManager}
 * (so the exact same {@code UserDetailsService} + BCrypt path is reused), then
 * issues a signed
 * access token carrying the user's bare role names plus a persisted, rotating
 * refresh token.
 *
 * <p>
 * <b>refresh</b> rotates the presented refresh token and mints a new access
 * token — no
 * credentials required, and the old refresh token is single-use.
 *
 * <p>
 * The constructor is a superset of the Lab 6 one (the two new collaborators
 * come last), so the
 * base unit test still constructs it positionally.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {

    private static final String BEARER_TYPE = "Bearer";
    private static final String ROLE_PREFIX = "ROLE_";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    @Override
    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("username", request.getUsername());
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("email", request.getEmail());
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                // Never store the raw password: BCrypt-hash it on the way in.
                .password(passwordEncoder.encode(request.getPassword()))
                // Self-registered users always start with the plain CLERK role.
                .roles(new HashSet<>(Set.of(Role.CLERK)))
                .build();

        User saved = userRepository.save(user);
        return new UserResponse(saved.getId(), saved.getUsername(), saved.getEmail(), saved.getRoles());
    }

    @Override
    @Transactional
    public TokenResponse login(LoginRequest request) {
        // Delegates to the DaoAuthenticationProvider: loads the user, matches the
        // BCrypt hash, and
        // throws BadCredentialsException (an AuthenticationException) on any failure.
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

        String username = authentication.getName();
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(AuthServiceImpl::stripRolePrefix)
                .toList();

        String accessToken = jwtService.generateAccessToken(username, roles);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", username));
        RefreshToken refreshToken = refreshTokenService.create(user);

        return new TokenResponse(accessToken, refreshToken.getToken(), BEARER_TYPE,
                jwtService.getAccessTokenTtlSeconds());
    }

    @Override
    public TokenResponse refresh(RefreshRequest refreshRequest) {
        RefreshToken rotated = refreshTokenService.rotate(refreshRequest.getRefreshToken());
        User user = rotated.getUser();
        List<String> roles = user.getRoles().stream().map(Role::name).toList();
        String accessToken = jwtService.generateAccessToken(user.getUsername(), roles);
        return new TokenResponse(accessToken, rotated.getToken(), BEARER_TYPE, jwtService.getAccessTokenTtlSeconds());
    }

    // Authorities carry the "ROLE_" prefix; the JWT roles claim stores bare names
    // (the filter
    // re-adds the prefix on the way back in).
    private static String stripRolePrefix(String authority) {
        return authority.startsWith(ROLE_PREFIX) ? authority.substring(ROLE_PREFIX.length()) : authority;
    }
}
