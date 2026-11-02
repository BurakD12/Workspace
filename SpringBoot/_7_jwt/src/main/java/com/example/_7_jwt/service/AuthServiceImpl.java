package com.example._7_jwt.service;

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

import com.example._7_jwt.dto.LoginRequest;
import com.example._7_jwt.dto.RefreshRequest;
import com.example._7_jwt.dto.RegisterRequest;
import com.example._7_jwt.dto.TokenResponse;
import com.example._7_jwt.dto.UserResponse;
import com.example._7_jwt.exception.DuplicateResourceException;
import com.example._7_jwt.exception.ResourceNotFoundException;
import com.example._7_jwt.model.RefreshToken;
import com.example._7_jwt.model.Role;
import com.example._7_jwt.model.User;
import com.example._7_jwt.repository.UserRepository;
import com.example._7_jwt.security.JwtService;

import lombok.RequiredArgsConstructor;

/**
 * Lab 7 (TaskFlow) — registration plus the stateless JWT auth flow.
 *
 * <p>
 * <b>register</b> is unchanged from the Lab 6 base (duplicate checks, BCrypt
 * hash, USER role).
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
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    private static final String BEARER_TYPE = "Bearer";
    private static final String ROLE_PREFIX = "ROLE_";

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
                // Self-registered users always start with the plain USER role.
                .roles(new HashSet<>(Set.of(Role.USER)))
                .build();

        User saved = userRepository.save(user);
        return new UserResponse(saved.getId(), saved.getUsername(), saved.getEmail(), saved.getRoles());
    }

    @Override
    @Transactional
    public TokenResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager
                .authenticate(new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
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
    @Transactional
    public TokenResponse refresh(RefreshRequest request) {
        RefreshToken rotated = refreshTokenService.rotate(request.getRefreshToken());
        User user = rotated.getUser();

        List<String> roles = user.getRoles().stream().map(Role::name).toList();
        String accessToken = jwtService.generateAccessToken(user.getUsername(), roles);

        return new TokenResponse(accessToken, rotated.getToken(), BEARER_TYPE,
                jwtService.getAccessTokenTtlSeconds());
    }

    private static String stripRolePrefix(String authority) {
        return authority.startsWith(ROLE_PREFIX) ? authority.substring(ROLE_PREFIX.length()) : authority;
    }
}
