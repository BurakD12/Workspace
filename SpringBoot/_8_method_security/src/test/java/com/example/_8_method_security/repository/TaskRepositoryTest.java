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

import com.example._8_method_security.model.Priority;
import com.example._8_method_security.model.Project;
import com.example._8_method_security.model.Role;
import com.example._8_method_security.model.Status;
import com.example._8_method_security.model.Task;
import com.example._8_method_security.model.User;

@DataJpaTest
@DisplayName("TaskRepository (finders + pagination)")
class TaskRepositoryTest {

    @Autowired private TaskRepository taskRepository;
    @Autowired private TestEntityManager em;

    private Long projectAId;
    private Long projectBId;
    private Long aliceId;
    // Lab 8: T2 lives in Apollo (owned by alice) and is assigned to bob; T4 is unassigned.
    private Long taskT2Id;
    private Long taskT4Id;

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
        Task t2 = Task.builder().title("T2").status(Status.IN_PROGRESS).priority(Priority.MEDIUM)
                .project(apollo).assignee(bob).build();
        em.persist(t2);
        em.persist(Task.builder().title("T3").status(Status.DONE).priority(Priority.HIGH)
                .project(gemini).assignee(alice).build());
        Task t4 = Task.builder().title("T4").status(Status.TODO).priority(Priority.MEDIUM)
                .project(gemini).build(); // unassigned
        em.persist(t4);

        em.flush();
        em.clear();

        projectAId = apollo.getId();
        projectBId = gemini.getId();
        aliceId = alice.getId();
        taskT2Id = t2.getId();
        taskT4Id = t4.getId();
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

    // --- Lab 8: the ownership probes behind TaskFlowPermissions ---------------------------------
    // These tests exist to prove the derived query NAMES resolve against a real database. Spring
    // Data turns "ByIdAndProjectOwnerUsername" into the property path id + project.owner.username at
    // repository-creation time, so a wrong name breaks context startup. A mocked repository (as used
    // in MethodSecurityTest) would never catch that.

    @Test
    @DisplayName("existsByIdAndProjectOwnerUsername walks task → project → owner → username")
    void projectOwnerProbe() {
        // T2 sits in Apollo, which alice owns — bob is only its assignee.
        assertThat(taskRepository.existsByIdAndProjectOwnerUsername(taskT2Id, "alice")).isTrue();
        assertThat(taskRepository.existsByIdAndProjectOwnerUsername(taskT2Id, "bob")).isFalse();
    }

    @Test
    @DisplayName("existsByIdAndAssigneeUsername is true only for the assignee")
    void assigneeProbe() {
        assertThat(taskRepository.existsByIdAndAssigneeUsername(taskT2Id, "bob")).isTrue();
        assertThat(taskRepository.existsByIdAndAssigneeUsername(taskT2Id, "alice")).isFalse();
    }

    @Test
    @DisplayName("existsByIdAndAssigneeUsername is false for an unassigned task (no NPE on a null side)")
    void assigneeProbeUnassignedTask() {
        assertThat(taskRepository.existsByIdAndAssigneeUsername(taskT4Id, "bob")).isFalse();
    }

    @Test
    @DisplayName("existsByIdAndProjectId detects whether an update is actually moving the task")
    void projectMembershipProbe() {
        assertThat(taskRepository.existsByIdAndProjectId(taskT2Id, projectAId)).isTrue();
        assertThat(taskRepository.existsByIdAndProjectId(taskT2Id, projectBId)).isFalse();
    }

    @Test
    @DisplayName("every probe returns false for an unknown task id (fail closed, never throws)")
    void probesFailClosedOnUnknownId() {
        assertThat(taskRepository.existsByIdAndProjectOwnerUsername(9999L, "alice")).isFalse();
        assertThat(taskRepository.existsByIdAndAssigneeUsername(9999L, "alice")).isFalse();
        assertThat(taskRepository.existsByIdAndProjectId(9999L, projectAId)).isFalse();
    }
}
