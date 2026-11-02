package com.example._7_jwt.repository;

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

import com.example._7_jwt.model.Priority;
import com.example._7_jwt.model.Project;
import com.example._7_jwt.model.Role;
import com.example._7_jwt.model.Status;
import com.example._7_jwt.model.Task;
import com.example._7_jwt.model.User;

@DataJpaTest
@DisplayName("TaskRepository (finders + pagination)")
class TaskRepositoryTest {

    @Autowired private TaskRepository taskRepository;
    @Autowired private TestEntityManager em;

    private Long projectAId;
    private Long aliceId;

    @BeforeEach
    void seed() {
        User alice = User.builder().username("alice").email("a@x.io").password("h").roles(Set.of(Role.USER)).build();
        User bob = User.builder().username("bob").email("b@x.io").password("h").roles(Set.of(Role.USER)).build();
        em.persist(alice);
        em.persist(bob);

        Project apollo = Project.builder().name("Apollo").owner(alice).build();
        Project gemini = Project.builder().name("Gemini").owner(bob).build();
        em.persist(apollo);
        em.persist(gemini);

        em.persist(Task.builder().title("T1").status(Status.TODO).priority(Priority.LOW)
                .project(apollo).assignee(alice).build());
        em.persist(Task.builder().title("T2").status(Status.IN_PROGRESS).priority(Priority.MEDIUM)
                .project(apollo).assignee(bob).build());
        em.persist(Task.builder().title("T3").status(Status.DONE).priority(Priority.HIGH)
                .project(gemini).assignee(alice).build());
        em.persist(Task.builder().title("T4").status(Status.TODO).priority(Priority.MEDIUM)
                .project(gemini).build()); // unassigned

        em.flush();
        em.clear();

        projectAId = apollo.getId();
        aliceId = alice.getId();
    }

    @Test
    @DisplayName("findByProjectId returns only that project's tasks")
    void findByProjectId() {
        Page<Task> page = taskRepository.findByProjectId(projectAId, PageRequest.of(0, 10, Sort.by("title")));
        assertThat(page.getContent()).extracting(Task::getTitle).containsExactly("T1", "T2");
    }

    @Test
    @DisplayName("findByAssigneeId returns only that user's tasks (unassigned tasks excluded)")
    void findByAssigneeId() {
        Page<Task> page = taskRepository.findByAssigneeId(aliceId, PageRequest.of(0, 10, Sort.by("title")));
        assertThat(page.getContent()).extracting(Task::getTitle).containsExactly("T1", "T3");
    }

    @Test
    @DisplayName("findByStatus filters by lifecycle state")
    void findByStatus() {
        Page<Task> page = taskRepository.findByStatus(Status.TODO, PageRequest.of(0, 10, Sort.by("title")));
        assertThat(page.getContent()).extracting(Task::getTitle).containsExactly("T1", "T4");
    }

    @Test
    @DisplayName("findAll paginates and sorts")
    void findAllPaginated() {
        Page<Task> page = taskRepository.findAll(PageRequest.of(0, 2, Sort.by("title")));
        assertThat(page.getTotalElements()).isEqualTo(4);
        assertThat(page.getTotalPages()).isEqualTo(2);
        assertThat(page.isLast()).isFalse();
        assertThat(page.getContent()).extracting(Task::getTitle).containsExactly("T1", "T2");
    }
}
