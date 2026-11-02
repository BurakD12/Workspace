package com.example._10_integration_testing.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.example._10_integration_testing.config.JwtProperties;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    JwtService jwtService(JwtProperties jwtProperties) {
        return new JwtService(jwtProperties);
    }

    @Bean
    JwtAuthenticationFilter jwtAuthenticationFilter(JwtService jwtService) {
        return new JwtAuthenticationFilter(jwtService);
    }

    @Bean
    JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint() {
        return new JwtAuthenticationEntryPoint();
    }

    @Bean
    RestAccessDeniedHandler restAccessDeniedHandler() {
        return new RestAccessDeniedHandler();
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
            RestAccessDeniedHandler restAccessDeniedHandler) throws Exception {
        http
                // A Bearer-token API is immune to CSRF (no ambient cookie the browser
                // attaches),
                // so CSRF protection is safely and deliberately disabled.
                .csrf(csrf -> csrf.disable())
                // No HttpSession: the SecurityContext is rebuilt per-request from the JWT.
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // --- Public: register + login + refresh (a client has no token yet) ---
                        .requestMatchers("/api/auth/**").permitAll()

                        // --- Public: the liveness question a container platform asks (Lab 9) ---
                        .requestMatchers("/actuator/health").permitAll()

                        // --- Admin-only area (user administration) ----------------------------
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // --- A clerk records stock. ORDER MATTERS: this must sit ABOVE the ADMIN
                        // rule for POST /api/products/** below, or that broader pattern wins
                        // and every clerk gets 403. First matching rule decides. -------------
                        .requestMatchers(HttpMethod.POST, "/api/products/*/movements").authenticated()

                        // --- Catalogue writes are ADMIN --------------------------------------
                        .requestMatchers(HttpMethod.POST,
                                "/api/categories/**", "/api/suppliers/**", "/api/products/**")
                        .hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT,
                                "/api/categories/**", "/api/suppliers/**", "/api/products/**")
                        .hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE,
                                "/api/categories/**", "/api/suppliers/**", "/api/products/**")
                        .hasRole("ADMIN")

                        // --- Everything else (all reads, GET /api/users/**) needs a token ------
                        .anyRequest().authenticated())
                // With no form/basic mechanism, restore 401 (not the default 403) on anonymous
                // or
                // invalid-token access; and render URL-rule 403s as ProblemDetail so they match
                // the
                // method-security 403s from GlobalExceptionHandler.
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        .accessDeniedHandler(restAccessDeniedHandler))
                // Validate the Bearer token before the username/password filter slot.
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
