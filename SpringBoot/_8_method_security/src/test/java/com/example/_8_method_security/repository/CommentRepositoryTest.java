package com.example._8_method_security.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import com.example._8_method_security.model.Comment;
import com.example._8_method_security.model.Project;
import com.example._8_method_security.model.Role;
import com.example._8_method_security.model.Task;
import com.example._8_method_security.model.User;

/**
 * Lab 8 (TaskFlow) — {@code CommentRepository} against a real (H2) database.
 *
 * <p>This slice earns its keep twice over. It verifies the thread ordering, and — more importantly —
 * it is the only layer that proves the derived query name {@code existsByIdAndAuthorUsername}
 * actually <b>resolves</b>. Spring Data parses method names into property paths when the repository
 * is created, so a typo or an unwalkable association fails at <em>context startup</em>, not at the
 * call site. In {@code MethodSecurityTest} the repository is a mock and a misspelt finder would
 * "pass" happily; here it cannot.
 */
@DataJpaTest
@DisplayName("CommentRepository (thread + ownership probes)")
class CommentRepositoryTest {

    @Autowired private CommentRepository commentRepository;
    @Autowired private TestEntityManager em;

    private Long taskId;
    private Long aliceCommentId;
    private Long bobCommentId;

    @BeforeEach
    void seed() {
        // alice owns Apollo; the task inside it is assigned to bob; both have commented.
        User alice = User.builder().username("alice").email("a@x.io").password("h")
                .roles(Set.of(Role.USER)).build();
        User bob = User.builder().username("bob").email("b@x.io").password("h")
                .roles(Set.of(Role.USER)).build();
        em.persist(alice);
        em.persist(bob);

        Project apollo = Project.builder().name("Apollo").owner(alice).build();
        em.persist(apollo);

        Task task = Task.builder().title("Design the lander").project(apollo).assignee(bob).build();
        em.persist(task);
        taskId = task.getId();

        // Persisted out of chronological order on purpose, so the ORDER BY is really tested.
        Comment bobs = Comment.builder().text("On it").task(task).author(bob)
                .createdAt(Instant.parse("2026-08-01T12:00:00Z")).build();
        Comment alices = Comment.builder().text("Owner note").task(task).author(alice)
                .createdAt(Instant.parse("2026-08-01T09:00:00Z")).build();
        em.persist(bobs);
        em.persist(alices);
        bobCommentId = bobs.getId();
        aliceCommentId = alices.getId();

        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("findByTaskIdOrderByCreatedAtAsc returns the whole thread oldest-first")
    void threadIsOrderedOldestFirst() {
        List<Comment> thread = commentRepository.findByTaskIdOrderByCreatedAtAsc(taskId);

        assertThat(thread).extracting(Comment::getText).containsExactly("Owner note", "On it");
    }

    @Test
    @DisplayName("findByTaskIdOrderByCreatedAtAsc returns empty for a task with no comments")
    void threadEmptyForUncommentedTask() {
        assertThat(commentRepository.findByTaskIdOrderByCreatedAtAsc(9999L)).isEmpty();
    }

    @Test
    @DisplayName("existsByIdAndAuthorUsername is true only for the real author")
    void authorProbe() {
        assertThat(commentRepository.existsByIdAndAuthorUsername(bobCommentId, "bob")).isTrue();
        assertThat(commentRepository.existsByIdAndAuthorUsername(bobCommentId, "alice")).isFalse();
        assertThat(commentRepository.existsByIdAndAuthorUsername(aliceCommentId, "alice")).isTrue();
    }

    @Test
    @DisplayName("existsByIdAndAuthorUsername is false for an unknown id (fail closed, no exception)")
    void authorProbeUnknownId() {
        assertThat(commentRepository.existsByIdAndAuthorUsername(9999L, "bob")).isFalse();
    }

    @Test
    @DisplayName("deleting a task cascades to its comments instead of hitting the task_id FK")
    void deletingTaskRemovesItsComments() {
        // Without the inverse @OneToMany(cascade = ALL, orphanRemoval = true) on Task, this delete
        // violates comments.task_id and surfaces to the client as a 500 rather than a 204.
        Task task = em.find(Task.class, taskId);
        assertThat(commentRepository.findByTaskIdOrderByCreatedAtAsc(taskId)).hasSize(2);

        em.getEntityManager().remove(task);
        em.flush();
        em.clear();

        assertThat(commentRepository.findByTaskIdOrderByCreatedAtAsc(taskId)).isEmpty();
        assertThat(commentRepository.existsById(bobCommentId)).isFalse();
        assertThat(commentRepository.existsById(aliceCommentId)).isFalse();
    }

    @Test
    @DisplayName("@PrePersist stamps createdAt when the caller did not set it")
    void createdAtIsStamped() {
        User carol = User.builder().username("carol").email("c@x.io").password("h")
                .roles(Set.of(Role.USER)).build();
        em.persist(carol);
        Task task = em.find(Task.class, taskId);

        Comment saved = commentRepository.save(
                Comment.builder().text("No timestamp given").task(task).author(carol).build());
        em.flush();

        assertThat(saved.getCreatedAt()).isNotNull();
    }
}
