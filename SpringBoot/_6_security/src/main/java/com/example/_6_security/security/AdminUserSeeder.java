package com.example._6_security.security;

import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.example._6_security.model.Role;
import com.example._6_security.model.User;
import com.example._6_security.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminUserSeeder implements ApplicationRunner {
    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;

    @Value("${app.security.admin-password:admin12345}")
    private String adminPassword;
    
    @Override
    public void run(ApplicationArguments args) throws Exception {        
        if (userRepository.existsByUsername("admin")) {
            return;
        }
        User admin = User.builder()
                .username("admin")
                .password(passwordEncoder.encode(adminPassword))
                .roles(new HashSet<>(Set.of(Role.ADMIN))).build();
        
        userRepository.save(admin);

        log.info("Seeded default ADMIN user 'admin'");
    }

   
}
