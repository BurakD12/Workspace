package com.example._10_integration_testing.security;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Lab 9 (StockFlow) — the 401 gateway for anonymous or invalid-token access to
 * a protected
 * endpoint. Because this is now a pure Bearer-token API (form login and HTTP
 * Basic are removed),
 * Spring Security would otherwise fall back to its default 403 entry point;
 * this restores the
 * correct 401 Unauthorized semantics and renders a small RFC 7807 ProblemDetail
 * body, matching
 * the house error style. Written by hand (no ObjectMapper dependency) so it is
 * robust in any slice.
 */
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.getWriter().write("{\"type\":\"about:blank\",\"title\":\"Unauthorized\",\"status\":401,"
                + "\"detail\":\"Authentication required: a valid Bearer access token is missing or invalid.\"}");
    }
}
