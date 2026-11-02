package com.example._7_jwt.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import com.example._7_jwt.model.Role;
import com.example._7_jwt.model.User;

@DataJpaTest
@DisplayName("UserRepository")
class UserRepositoryTest {

    @Autowired private UserRepository userRepository;
    @Autowired private TestEntityManager em;

    @BeforeEach
    void seed() {
        em.persist(User.builder()
                .username("alice").email("alice@taskflow.io").password("bcrypt-hash")
                .roles(Set.of(Role.USER, Role.ADMIN))
                .build());
        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("findByUsername returns the user with eagerly-loaded roles when present")
    void findByUsernamePresent() {
        Optional<User> found = userRepository.findByUsername("alice");

        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("alice@taskflow.io");
        assertThat(found.get().getPassword()).isEqualTo("bcrypt-hash");
        assertThat(found.get().getRoles()).containsExactlyInAnyOrder(Role.USER, Role.ADMIN);
    }

    @Test
    @DisplayName("findByUsername returns empty when absent")
    void findByUsernameAbsent() {
        assertThat(userRepository.findByUsername("nobody")).isEmpty();
    }

    @Test
    @DisplayName("existsByUsername reflects presence")
    void existsByUsername() {
        assertThat(userRepository.existsByUsername("alice")).isTrue();
        assertThat(userRepository.existsByUsername("nobody")).isFalse();
    }

    @Test
    @DisplayName("existsByEmail reflects presence")
    void existsByEmail() {
        assertThat(userRepository.existsByEmail("alice@taskflow.io")).isTrue();
        assertThat(userRepository.existsByEmail("nobody@taskflow.io")).isFalse();
    }
}
