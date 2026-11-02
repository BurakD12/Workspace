package com.example._10_integration_testing.security;

import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.example._10_integration_testing.model.Role;
import com.example._10_integration_testing.model.User;
import com.example._10_integration_testing.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Lab 9 (StockFlow) — seeds a single ADMIN account at startup so the admin area
 * is usable out of
 * the box. Self-registration only ever yields {@code Role.CLERK}, so an ADMIN
 * must be bootstrapped.
 *
 * <p>
 * Deliberately a plain {@code @Component} implementing
 * {@link ApplicationRunner} rather than a
 * {@code @Bean} inside a {@code @Configuration}: the web/JPA test slices
 * ({@code @WebMvcTest},
 * {@code @DataJpaTest}) do not scan arbitrary {@code @Component}s, so this
 * seeder never runs
 * during those tests and cannot pollute their contexts.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AdminUserSeeder implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // Documented default (seeded credentials: admin / admin12345); override in
    // production via
    // app.security.admin-password.
    @Value("${app.security.admin-password:admin12345}")
    private String adminPassword;

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.existsByUsername("admin")) {
            return; // idempotent: never seed twice
        }
        User admin = User.builder()
                .username("admin")
                .email("admin@stockflow.local")
                .password(passwordEncoder.encode(adminPassword))
                .roles(new HashSet<>(Set.of(Role.ADMIN)))
                .build();
        userRepository.save(admin);
        log.info("Seeded default ADMIN user 'admin'");
    }
}
