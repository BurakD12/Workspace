package com.example._6_security.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // DELIBERATE DECISION: CSRF protection is disabled.
                // CSRF defends cookie/session-authenticated *browser* form posts, where the
                // browser attaches the session cookie automatically and an attacker's page can
                // forge a request. This API is exercised by non-browser clients authenticating
                // with HTTP Basic (an explicit Authorization header that a cross-site page cannot
                // silently attach), so CSRF tokens add friction without adding protection here.
                // A full CSRF treatment (and the stateless-token rationale) is deferred to Lab 7.
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // --- Public: self-service registration + all read traffic ---
                        .requestMatchers(HttpMethod.POST, "/api/auth/register").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/**").permitAll()

                        // --- Catalog administration: only ADMIN may mutate categories/authors/publishers ---
                        .requestMatchers(HttpMethod.POST, "/api/categories/**", "/api/authors/**", "/api/publishers/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/categories/**", "/api/authors/**", "/api/publishers/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/categories/**", "/api/authors/**", "/api/publishers/**").hasRole("ADMIN")

                        // --- Book writes: any authenticated user (USER or ADMIN) ---
                        .requestMatchers(HttpMethod.POST, "/api/books/**").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/books/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/books/**").authenticated()

                        // --- Everything else must be authenticated ---
                        .anyRequest().authenticated())
                // Two login mechanisms: HTTP Basic (the primary path for API/non-browser clients,
                // returning 401 on anonymous access) and a default form login for browsers.
                .httpBasic(Customizer.withDefaults())
                .formLogin(Customizer.withDefaults());
        return http.build();
    }
}
