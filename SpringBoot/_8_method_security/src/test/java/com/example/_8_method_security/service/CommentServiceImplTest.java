package com.example._8_method_security.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example._8_method_security.dto.CommentRequestDto;
import com.example._8_method_security.dto.CommentResponseDto;
import com.example._8_method_security.dto.CommentUpdateRequestDto;
import com.example._8_method_security.exception.ResourceNotFoundException;
import com.example._8_method_security.model.Comment;
import com.example._8_method_security.model.Project;
import com.example._8_method_security.model.Role;
import com.example._8_method_security.model.Task;
import com.example._8_method_security.model.User;
import com.example._8_method_security.repository.CommentRepository;
import com.example._8_method_security.repository.TaskRepository;
import com.example._8_method_security.repository.UserRepository;

/**
 * Lab 8 (TaskFlow) — {@code CommentServiceImpl}'s business logic, in isolation.
 *
 * <p><b>Read this carefully, because it is a trap worth understanding.</b> {@code @InjectMocks}
 * constructs the service with {@code new} — no Spring, therefore no AOP proxy, therefore
 * <b>none of the {@code @PreAuthorize} / {@code @PostAuthorize} / {@code @PostFilter} annotations
 * run in this class.</b> Every method here executes unguarded.
 *
 * <p>That is not a flaw, it is the division of labour: this class tests mapping, id-resolution, 404s
 * and the fields a write does or does not change. {@code MethodSecurityTest} tests the rules. What
 * you must never do is write a "carol cannot delete bob's comment" test <em>here</em> and believe it —
 * it would pass whether the annotation existed or not, because nothing is checking.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CommentServiceImpl (business logic; authorization annotations are INERT here)")
class CommentServiceImplTest {

    @Mock private CommentRepository commentRepository;
    @Mock private TaskRepository taskRepository;
    @Mock private UserRepository userRepository;
    @InjectMocks private CommentServiceImpl service;

    private static final long TASK_ID = 50L;

    private User bob() {
        return User.builder().id(2L).username("bob").email("bob@taskflow.io").password("hash")
                .roles(Set.of(Role.USER)).build();
    }

    private Task task() {
        Project apollo = Project.builder().id(7L).name("Apollo")
                .owner(User.builder().id(1L).username("alice").build()).build();
        return Task.builder().id(TASK_ID).title("Design the lander").project(apollo).assignee(bob()).build();
    }

    private Comment existing() {
        return Comment.builder().id(502L).text("On it").createdAt(Instant.parse("2026-08-01T10:00:00Z"))
                .task(task()).author(bob()).build();
    }

    @Test
    @DisplayName("createComment sets the author from the principal argument and maps nested summaries")
    void createSetsAuthorAndMaps() {
        given(taskRepository.findById(TASK_ID)).willReturn(Optional.of(task()));
        given(userRepository.findByUsername("bob")).willReturn(Optional.of(bob()));
        given(commentRepository.save(any(Comment.class))).willAnswer(inv -> {
            Comment c = inv.getArgument(0);
            c.setId(502L);
            return c;
        });

        CommentResponseDto res = service.createComment(
                CommentRequestDto.builder().taskId(TASK_ID).text("On it").build(), "bob");

        assertThat(res.getId()).isEqualTo(502L);
        assertThat(res.getText()).isEqualTo("On it");
        assertThat(res.getAuthor().getUsername()).isEqualTo("bob");
        assertThat(res.getTask().getId()).isEqualTo(TASK_ID);
        assertThat(res.getTask().getTitle()).isEqualTo("Design the lander");
    }

    @Test
    @DisplayName("createComment 404s when the task does not exist")
    void createUnknownTask() {
        given(taskRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.createComment(
                CommentRequestDto.builder().taskId(999L).text("hi").build(), "bob"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Task");
        verify(commentRepository, never()).save(any(Comment.class));
    }

    @Test
    @DisplayName("getCommentsByTask 404s when the task does not exist")
    void threadUnknownTask() {
        given(taskRepository.existsById(999L)).willReturn(false);

        assertThatThrownBy(() -> service.getCommentsByTask(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Task");
    }

    @Test
    @DisplayName("getCommentsByTask returns a MUTABLE list, because @PostFilter rewrites it in place")
    void threadListIsMutable() {
        given(taskRepository.existsById(TASK_ID)).willReturn(true);
        given(commentRepository.findByTaskIdOrderByCreatedAtAsc(TASK_ID))
                .willReturn(List.of(existing()));

        List<CommentResponseDto> thread = service.getCommentsByTask(TASK_ID);

        // Spring Security filters by calling clear() + addAll() on the returned collection, so an
        // immutable list (List.of / stream().toList()) would fail at runtime with
        // UnsupportedOperationException. Proving mutability here documents that requirement.
        assertThat(thread).hasSize(1);
        thread.clear();
        assertThat(thread).isEmpty();
    }

    @Test
    @DisplayName("updateComment changes only the text; task, author and createdAt are untouched")
    void updateOnlyChangesText() {
        given(commentRepository.findById(502L)).willReturn(Optional.of(existing()));
        given(commentRepository.save(any(Comment.class))).willAnswer(inv -> inv.getArgument(0));

        CommentResponseDto res = service.updateComment(502L, new CommentUpdateRequestDto("Edited"));

        assertThat(res.getText()).isEqualTo("Edited");
        assertThat(res.getAuthor().getUsername()).isEqualTo("bob");
        assertThat(res.getTask().getId()).isEqualTo(TASK_ID);
        assertThat(res.getCreatedAt()).isEqualTo(Instant.parse("2026-08-01T10:00:00Z"));
    }

    @Test
    @DisplayName("updateComment 404s for an unknown id")
    void updateUnknown() {
        given(commentRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateComment(999L, new CommentUpdateRequestDto("x")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Comment");
    }

    @Test
    @DisplayName("deleteComment removes the row")
    void deleteOk() {
        given(commentRepository.existsById(502L)).willReturn(true);

        service.deleteComment(502L);

        verify(commentRepository).deleteById(502L);
    }

    @Test
    @DisplayName("deleteComment 404s for an unknown id")
    void deleteUnknown() {
        given(commentRepository.existsById(999L)).willReturn(false);

        assertThatThrownBy(() -> service.deleteComment(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Comment");
        verify(commentRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("getCommentById maps the row (the @PostAuthorize rule is not active in a unit test)")
    void getByIdMaps() {
        given(commentRepository.findById(502L)).willReturn(Optional.of(existing()));

        assertThat(service.getCommentById(502L).getAuthor().getUsername()).isEqualTo("bob");
    }
}
