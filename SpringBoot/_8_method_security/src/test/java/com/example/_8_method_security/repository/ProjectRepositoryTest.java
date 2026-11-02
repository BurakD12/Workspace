package com.example._8_method_security.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import com.example._8_method_security.model.Project;
import com.example._8_method_security.model.Role;
import com.example._8_method_security.model.User;

/**
 * Lab 8 (TaskFlow) — {@code ProjectRepository}'s new visibility scoping and ownership probe.
 *
 * <p>The scoping test is the one to read carefully: it asserts that {@code totalElements} reflects
 * <b>only</b> the caller's own projects. That is the whole argument for filtering in the WHERE clause
 * instead of with {@code @PostFilter} — a post-filtered page would still report the unfiltered
 * total, telling the client there are more results than it is allowed to see.
 */
@DataJpaTest
@DisplayName("ProjectRepository (owner scoping + ownership probe)")
class ProjectRepositoryTest {

    @Autowired private ProjectRepository projectRepository;
    @Autowired private TestEntityManager em;

    private Long apolloId;

    @BeforeEach
    void seed() {
        User alice = User.builder().username("alice").email("a@x.io").password("h")
                .roles(Set.of(Role.USER)).build();
        User bob = User.builder().username("bob").email("b@x.io").password("h")
                .roles(Set.of(Role.USER)).build();
        em.persist(alice);
        em.persist(bob);

        Project apollo = Project.builder().name("Apollo").owner(alice).build();
        em.persist(apollo);
        em.persist(Project.builder().name("Artemis").owner(alice).build());
        em.persist(Project.builder().name("Gemini").owner(bob).build());
        apolloId = apollo.getId();

        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("findByOwnerUsername pages only the caller's own projects, with a truthful total")
    void ownerScopedPage() {
        Page<Project> page = projectRepository.findByOwnerUsername(
                "alice", PageRequest.of(0, 20, Sort.by("name")));

        assertThat(page.getContent()).extracting(Project::getName).containsExactly("Apollo", "Artemis");
        // 2, not 3: the third project belongs to bob and was never fetched or counted.
        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getTotalPages()).isEqualTo(1);
    }

    @Test
    @DisplayName("findByOwnerUsername returns an empty page for a user who owns nothing")
    void ownerScopedPageEmpty() {
        Page<Project> page = projectRepository.findByOwnerUsername("carol", PageRequest.of(0, 20));

        assertThat(page.getContent()).isEmpty();
        assertThat(page.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("existsByIdAndOwnerUsername is true only for the real owner")
    void ownerProbe() {
        assertThat(projectRepository.existsByIdAndOwnerUsername(apolloId, "alice")).isTrue();
        assertThat(projectRepository.existsByIdAndOwnerUsername(apolloId, "bob")).isFalse();
    }

    @Test
    @DisplayName("existsByIdAndOwnerUsername is false for an unknown id (fail closed)")
    void ownerProbeUnknownId() {
        assertThat(projectRepository.existsByIdAndOwnerUsername(9999L, "alice")).isFalse();
    }
}
