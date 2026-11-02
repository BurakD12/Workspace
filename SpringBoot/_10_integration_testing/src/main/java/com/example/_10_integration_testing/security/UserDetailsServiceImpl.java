package com.example._10_integration_testing.security;

import java.util.List;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.example._10_integration_testing.model.User;
import com.example._10_integration_testing.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * Lab 9 (StockFlow) — bridges the app's own {@link User} table to Spring
 * Security's
 * authentication machinery. The DaoAuthenticationProvider calls
 * {@code loadUserByUsername}
 * during login; we load the user from the database and adapt it to a Spring
 * Security
 * {@link UserDetails}, mapping each
 * {@link com.example._10_integration_testing.model.Role} to a
 * {@code ROLE_<name>} authority so URL rules written with
 * {@code hasRole("ADMIN")} line up.
 *
 * <p>
 * Roles are mechanism-agnostic: this same UserDetailsService backs HTTP Basic
 * today and will
 * back the JWT AuthenticationManager login in Lab 7's stateless increment
 * unchanged.
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

        private final UserRepository userRepository;

        @Override
        public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
                User user = userRepository.findByUsername(username)
                                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

                List<SimpleGrantedAuthority> authorities = user.getRoles().stream()
                                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
                                .toList();

                // Use the framework's built-in UserDetails implementation as the adapter.
                return new org.springframework.security.core.userdetails.User(
                                user.getUsername(), user.getPassword(), authorities);
        }
}
