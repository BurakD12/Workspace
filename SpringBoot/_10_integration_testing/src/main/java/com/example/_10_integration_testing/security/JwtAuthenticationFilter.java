package com.example._10_integration_testing.security;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * Lab 9 (StockFlow) — the once-per-request Bearer-token authenticator. It reads
 * {@code Authorization: Bearer <jwt>}, and if the token verifies, builds a
 * {@link UsernamePasswordAuthenticationToken} (principal = username,
 * authorities = the roles claim
 * mapped to {@code ROLE_<name>}) and places it in the
 * {@link SecurityContextHolder}. Registered
 * with {@code addFilterBefore(..., UsernamePasswordAuthenticationFilter.class)}
 * in
 * {@code SecurityConfig}, so it slots into Lab 6's filter chain ahead of
 * form-login processing.
 *
 * <p>
 * Deliberately permissive on failure: a missing header, a non-Bearer header, or
 * an
 * invalid/expired/tampered token simply leaves the context unauthenticated and
 * lets the chain
 * continue — the authorization rules then produce a 401 via the entry point.
 * The filter never
 * throws and never writes a response itself. It also yields to an
 * already-populated context (e.g.
 * a test's {@code @WithMockUser}), so it is authentication-mechanism-agnostic.
 */

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String header = request.getHeader(HEADER);
        if (header != null && header.startsWith(BEARER_PREFIX)
                && SecurityContextHolder.getContext().getAuthentication() == null) {

            String token = header.substring(BEARER_PREFIX.length());
            if (jwtService.isValid(token)) {
                String username = jwtService.extractUsername(token);
                List<SimpleGrantedAuthority> authorities = jwtService.extractRoles(token).stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                        .toList();

                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(username, null,
                        authorities);
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }
        chain.doFilter(request, response);
    }
}
