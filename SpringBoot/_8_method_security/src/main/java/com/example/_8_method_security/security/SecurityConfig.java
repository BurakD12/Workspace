package com.example._8_method_security.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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

import com.example._8_method_security.config.JwtProperties;

/**
 * Lab 7 (TaskFlow) — the stateless, token-based SecurityFilterChain. This is
 * the Lab 6 foundation
 * (DB-backed {@code UserDetailsService}, BCrypt, URL role rules, exposed
 * {@code AuthenticationManager})
 * reconfigured for JWT: no server-side session, no login form, authentication
 * carried entirely by
 * the {@code Authorization: Bearer} header and validated by
 * {@link JwtAuthenticationFilter}.
 *
 * <p>
 * Everything uses the Spring Security 7 lambda DSL. The 5/6 APIs
 * {@code authorizeRequests},
 * {@code antMatchers}, and {@code WebSecurityConfigurerAdapter} are removed in
 * 7 and not used.
 *
 * <p>
 * <b>What changed from the Lab 6 base (the Lab 7 delta):</b>
 * <ul>
 * <li><b>STATELESS</b> — {@code SessionCreationPolicy.STATELESS}: no
 * {@code HttpSession}, no
 * {@code JSESSIONID}; every request re-authenticates from its Bearer
 * token.</li>
 * <li><b>CSRF stays disabled</b> — now unambiguously correct: a Bearer-token
 * API cannot be driven
 * by a browser's ambient cookies, which is exactly the threat CSRF
 * defends.</li>
 * <li><b>formLogin and httpBasic removed</b> — this is a pure token API with no
 * interactive login
 * page and no Basic challenge. Removing them also removes their
 * {@code AuthenticationEntryPoint},
 * so a 401 entry point is wired explicitly (Spring's fallback would be a
 * 403).</li>
 * <li><b>JWT filter</b> — {@link JwtAuthenticationFilter} added with
 * {@code addFilterBefore(..., UsernamePasswordAuthenticationFilter.class)}.</li>
 * <li><b>/api/auth/** is permitAll</b> — register + login + refresh must be
 * reachable without a token.</li>
 * </ul>
 * The domain authorization matrix is otherwise unchanged, and the
 * {@code AuthenticationManager}
 * bean (used by the /login endpoint) is preserved.
 *
 * <p>
 * {@code JwtService}, the filter, and the entry point are declared as
 * {@code @Bean}s here (not
 * component-scanned) so importing this one config into a {@code @WebMvcTest}
 * slice fully wires the
 * chain — the mechanism-agnostic {@code @WithMockUser} tests keep working
 * untouched.
 */
@Configuration
@EnableWebSecurity
// --- Lab 8
// -----------------------------------------------------------------------------------
// Turns on annotation-driven method authorization: @PreAuthorize /
// @PostAuthorize / @PreFilter /
// @PostFilter become active on Spring beans. Mechanically it registers AOP
// advisors, so the rules
// are enforced by a PROXY around each annotated bean — which is why an
// annotation only fires when
// the method is called from OUTSIDE its own bean (see the self-invocation note
// in UserServiceImpl).
//
// This is a SECOND, independent layer, not a replacement for the URL rules
// below. The filter chain
// still decides "may this request reach a controller at all"; method security
// decides "may THIS
// principal run THIS method on THIS row". Nothing here is enabled by default:
// without this
// annotation the @PreAuthorize annotations in the service package are inert
// comments.
@EnableMethodSecurity
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    /**
     * BCrypt: adaptive, salted password hashing. Used to encode on register and
     * match on login.
     */
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

    /**
     * Lab 8: RFC 7807 body for 403s raised by the URL rules (see the class
     * javadoc).
     */
    @Bean
    RestAccessDeniedHandler restAccessDeniedHandler() {
        return new RestAccessDeniedHandler();
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

                        // --- Admin-only area (user administration) exercises role-based authz ---
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // --- Everything else needs a token -------------------------------------
                        // Lab 8 note: the URL rules stay exactly this coarse ON PURPOSE. Every
                        // per-row rule (own this project, wrote this comment) now lives on the
                        // service methods, because a matcher can only see the METHOD and the PATH
                        // — never the request body, never the row being touched, never who owns it.
                        // /api/comments/** needs no new entry here: it is covered by anyRequest().
                        .anyRequest().authenticated())
                // With no form/basic mechanism, restore 401 (not the default 403) on anonymous
                // or
                // invalid-token access to a protected endpoint; and render URL-rule 403s as
                // ProblemDetail so they match the method-security 403s from
                // GlobalExceptionHandler.
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        .accessDeniedHandler(restAccessDeniedHandler))
                // Validate the Bearer token before the username/password filter slot.
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /**
     * Expose the AuthenticationManager built from the AuthenticationConfiguration.
     * The JWT /login
     * endpoint authenticates credentials through it, reusing the Lab 6
     * UserDetailsService + BCrypt
     * path unchanged.
     */
    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }
}
