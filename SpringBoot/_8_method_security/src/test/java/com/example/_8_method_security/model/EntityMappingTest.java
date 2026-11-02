package com.example._8_method_security.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

@DataJpaTest
@DisplayName("Entity relationship mapping")
class EntityMappingTest {

    @Autowired
    private TestEntityManager em;

    @Test
    @DisplayName("persists a Task linked to a Project (with owner) and an assignee, and reloads every relationship")
    void persistsAndReloadsRelationships() {
        User owner = User.builder()
                .username("alice").email("alice@taskflow.io").password("bcrypt-hash")
                .roles(Set.of(Role.USER))
                .build();
        em.persist(owner);

        Project project = Project.builder().name("Apollo").description("Moon landing").owner(owner).build();
        em.persist(project);

        Task task = Task.builder()
                .title("Design the lander")
                .description("Sketch the descent stage")
                .status(Status.IN_PROGRESS)
                .priority(Priority.HIGH)
                .dueDate(LocalDate.of(2026, 1, 15))
                .project(project)
                .assignee(owner)
                .build();
        Task saved = em.persistFlushFind(task);
        em.clear(); // detach everything so the reload actually hits the database

        Task reloaded = em.find(Task.class, saved.getId());
        assertThat(reloaded.getProject().getName()).isEqualTo("Apollo");
        assertThat(reloaded.getProject().getOwner().getUsername()).isEqualTo("alice");
        assertThat(reloaded.getAssignee().getUsername()).isEqualTo("alice");
        assertThat(reloaded.getStatus()).isEqualTo(Status.IN_PROGRESS);
        assertThat(reloaded.getPriority()).isEqualTo(Priority.HIGH);
        assertThat(reloaded.getDueDate()).isEqualTo(LocalDate.of(2026, 1, 15));
    }

    @Test
    @DisplayName("a Task built without status/priority/assignee defaults to TODO/MEDIUM and a null assignee")
    void appliesEntityDefaults() {
        User owner = User.builder()
                .username("bob").email("bob@taskflow.io").password("bcrypt-hash")
                .roles(Set.of(Role.USER))
                .build();
        em.persist(owner);
        Project project = Project.builder().name("Gemini").owner(owner).build();
        em.persist(project);

        Task task = Task.builder().title("Unspecified").project(project).build();
        Task saved = em.persistFlushFind(task);
        em.clear();

        Task reloaded = em.find(Task.class, saved.getId());
        assertThat(reloaded.getStatus()).isEqualTo(Status.TODO);
        assertThat(reloaded.getPriority()).isEqualTo(Priority.MEDIUM);
        assertThat(reloaded.getAssignee()).isNull();
    }
}
