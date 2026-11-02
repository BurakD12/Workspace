package com.example._8_method_security.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.stereotype.Component;

import com.example._8_method_security.repository.CommentRepository;
import com.example._8_method_security.repository.ProjectRepository;
import com.example._8_method_security.repository.TaskRepository;
import com.example._8_method_security.service.CommentServiceImpl;
import com.example._8_method_security.service.ProjectServiceImpl;
import com.example._8_method_security.service.TaskServiceImpl;
import com.example._8_method_security.service.UserServiceImpl;

/**
 * Lab 8 (TaskFlow) — {@code TaskFlowPermissions} in isolation.
 *
 * <p>This class is a plain unit test on purpose: the permission bean carries no annotations of its
 * own, so there is nothing here that needs a proxy. What it must prove is the property every
 * authorization primitive lives or dies by — that it <b>fails closed</b>. A probe that returns
 * {@code true} when it cannot tell, or that throws on a null argument and turns a 403 into a 500, is
 * a bug with security consequences.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TaskFlowPermissions")
class TaskFlowPermissionsTest {

    @Mock private ProjectRepository projectRepository;
    @Mock private TaskRepository taskRepository;
    @Mock private CommentRepository commentRepository;
    @InjectMocks private TaskFlowPermissions permissions;

    private static final long PROJECT_ID = 7L;
    private static final long TASK_ID = 50L;
    private static final long COMMENT_ID = 502L;

    private Authentication alice() {
        return new UsernamePasswordAuthenticationToken("alice", "n/a",
                AuthorityUtils.createAuthorityList("ROLE_USER"));
    }

    private Authentication anonymous() {
        return new AnonymousAuthenticationToken("key", "anonymousUser",
                AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));
    }

    @Nested
    @DisplayName("delegates the right question to the right query")
    class HappyPath {

        @Test
        @DisplayName("isProjectOwner asks the project table")
        void isProjectOwner() {
            given(projectRepository.existsByIdAndOwnerUsername(PROJECT_ID, "alice")).willReturn(true);

            assertThat(permissions.isProjectOwner(PROJECT_ID, alice())).isTrue();
        }

        @Test
        @DisplayName("isProjectOwnerOfTask walks task → project → owner")
        void isProjectOwnerOfTask() {
            given(taskRepository.existsByIdAndProjectOwnerUsername(TASK_ID, "alice")).willReturn(true);

            assertThat(permissions.isProjectOwnerOfTask(TASK_ID, alice())).isTrue();
        }

        @Test
        @DisplayName("isTaskAssignee asks the assignee side")
        void isTaskAssignee() {
            given(taskRepository.existsByIdAndAssigneeUsername(TASK_ID, "alice")).willReturn(true);

            assertThat(permissions.isTaskAssignee(TASK_ID, alice())).isTrue();
        }

        @Test
        @DisplayName("isCommentAuthor asks the comment table")
        void isCommentAuthor() {
            given(commentRepository.existsByIdAndAuthorUsername(COMMENT_ID, "alice")).willReturn(true);

            assertThat(permissions.isCommentAuthor(COMMENT_ID, alice())).isTrue();
        }
    }

    @Nested
    @DisplayName("canWorkOnTask = project owner OR assignee")
    class CanWorkOnTask {

        @Test
        @DisplayName("true for the project owner, and it short-circuits before the assignee query")
        void trueForOwner() {
            given(taskRepository.existsByIdAndProjectOwnerUsername(TASK_ID, "alice")).willReturn(true);

            assertThat(permissions.canWorkOnTask(TASK_ID, alice())).isTrue();
            verify(taskRepository, never()).existsByIdAndAssigneeUsername(TASK_ID, "alice");
        }

        @Test
        @DisplayName("true for the assignee when they do not own the project")
        void trueForAssignee() {
            given(taskRepository.existsByIdAndProjectOwnerUsername(TASK_ID, "alice")).willReturn(false);
            given(taskRepository.existsByIdAndAssigneeUsername(TASK_ID, "alice")).willReturn(true);

            assertThat(permissions.canWorkOnTask(TASK_ID, alice())).isTrue();
        }

        @Test
        @DisplayName("false for someone with no stake in the task")
        void falseForStranger() {
            given(taskRepository.existsByIdAndProjectOwnerUsername(TASK_ID, "alice")).willReturn(false);
            given(taskRepository.existsByIdAndAssigneeUsername(TASK_ID, "alice")).willReturn(false);

            assertThat(permissions.canWorkOnTask(TASK_ID, alice())).isFalse();
        }
    }

    @Nested
    @DisplayName("canPlaceTaskInProject: staying put is free, moving needs both ends")
    class CanPlaceTaskInProject {

        @Test
        @DisplayName("true when the task is already in that project (nothing is being moved)")
        void trueWhenUnchanged() {
            given(taskRepository.existsByIdAndProjectId(TASK_ID, PROJECT_ID)).willReturn(true);

            assertThat(permissions.canPlaceTaskInProject(TASK_ID, PROJECT_ID, alice())).isTrue();
            // No ownership question needs asking at all.
            verifyNoInteractions(projectRepository);
        }

        @Test
        @DisplayName("true for a real move when the caller owns the source AND the destination")
        void trueWhenOwningBothEnds() {
            long destination = 8L;
            given(taskRepository.existsByIdAndProjectId(TASK_ID, destination)).willReturn(false);
            given(taskRepository.existsByIdAndProjectOwnerUsername(TASK_ID, "alice")).willReturn(true);
            given(projectRepository.existsByIdAndOwnerUsername(destination, "alice")).willReturn(true);

            assertThat(permissions.canPlaceTaskInProject(TASK_ID, destination, alice())).isTrue();
        }

        @Test
        @DisplayName("false when the caller owns only the destination — this is the theft it prevents")
        void falseWhenOwningOnlyDestination() {
            long destination = 8L;
            given(taskRepository.existsByIdAndProjectId(TASK_ID, destination)).willReturn(false);
            given(taskRepository.existsByIdAndProjectOwnerUsername(TASK_ID, "alice")).willReturn(false);

            assertThat(permissions.canPlaceTaskInProject(TASK_ID, destination, alice())).isFalse();
            // Short-circuits: owning the destination is never even asked about.
            verify(projectRepository, never()).existsByIdAndOwnerUsername(destination, "alice");
        }

        @Test
        @DisplayName("false when the caller owns only the source")
        void falseWhenOwningOnlySource() {
            long destination = 8L;
            given(taskRepository.existsByIdAndProjectId(TASK_ID, destination)).willReturn(false);
            given(taskRepository.existsByIdAndProjectOwnerUsername(TASK_ID, "alice")).willReturn(true);
            given(projectRepository.existsByIdAndOwnerUsername(destination, "alice")).willReturn(false);

            assertThat(permissions.canPlaceTaskInProject(TASK_ID, destination, alice())).isFalse();
        }
    }

    @Nested
    @DisplayName("fails closed without ever throwing or querying")
    class FailsClosed {

        @Test
        @DisplayName("a null Authentication is 'no'")
        void nullAuthentication() {
            assertThat(permissions.isProjectOwner(PROJECT_ID, null)).isFalse();
            assertThat(permissions.isProjectOwnerOfTask(TASK_ID, null)).isFalse();
            assertThat(permissions.isTaskAssignee(TASK_ID, null)).isFalse();
            assertThat(permissions.isCommentAuthor(COMMENT_ID, null)).isFalse();
            verifyNoInteractions(projectRepository, taskRepository, commentRepository);
        }

        @Test
        @DisplayName("an anonymous token is 'no' — it is NOT a principal named \"anonymousUser\"")
        void anonymousToken() {
            assertThat(permissions.isProjectOwner(PROJECT_ID, anonymous())).isFalse();
            assertThat(permissions.isCommentAuthor(COMMENT_ID, anonymous())).isFalse();
            verifyNoInteractions(projectRepository, commentRepository);
        }

        @Test
        @DisplayName("a null id is 'no' (no NullPointerException turning a 403 into a 500)")
        void nullId() {
            assertThat(permissions.isProjectOwner(null, alice())).isFalse();
            assertThat(permissions.isProjectOwnerOfTask(null, alice())).isFalse();
            assertThat(permissions.isTaskAssignee(null, alice())).isFalse();
            assertThat(permissions.isCommentAuthor(null, alice())).isFalse();
            assertThat(permissions.canPlaceTaskInProject(null, PROJECT_ID, alice())).isFalse();
            assertThat(permissions.canPlaceTaskInProject(TASK_ID, null, alice())).isFalse();
            verifyNoInteractions(projectRepository, taskRepository, commentRepository);
        }

        @Test
        @DisplayName("canWorkOnTask is 'no' for a null and for an anonymous principal")
        void canWorkOnTaskWithoutPrincipal() {
            assertThat(permissions.canWorkOnTask(TASK_ID, null)).isFalse();
            assertThat(permissions.canWorkOnTask(TASK_ID, anonymous())).isFalse();
            verifyNoInteractions(taskRepository);
        }

        @Test
        @DisplayName("canPlaceTaskInProject is 'no' without a principal — even when nothing is moving")
        void canPlaceTaskInProjectWithoutPrincipal() {
            // The regression this pins: the "unchanged project" short-circuit sits before the two
            // ownership probes, so if the principal is only checked inside those probes, an anonymous
            // caller whose task already lives in the named project gets `true` — and issues a query
            // to find that out. Fail-closed means answering "no" before doing any work.
            assertThat(permissions.canPlaceTaskInProject(TASK_ID, PROJECT_ID, null)).isFalse();
            assertThat(permissions.canPlaceTaskInProject(TASK_ID, PROJECT_ID, anonymous())).isFalse();
            verifyNoInteractions(projectRepository, taskRepository, commentRepository);
        }
    }

    @Nested
    @DisplayName("the SpEL bean-name contract")
    class BeanName {

        /**
         * SpEL resolves {@code @taskFlowPermissions} from a <b>string</b> at request time, so a rename
         * of this class (or dropping the explicit name from {@code @Component}) breaks every expression
         * — and breaks it when a user hits an endpoint, not when you compile.
         *
         * <p>Nothing else in the suite can catch that: {@code MethodSecurityTest} declares its own
         * {@code taskFlowPermissions} {@code @Bean}, and the {@code @WebMvcTest} slices mock the service
         * interfaces. This test is the guard.
         */
        @Test
        @DisplayName("the @Component name is exactly the one every SpEL expression references")
        void componentNameIsPinned() {
            Component component = TaskFlowPermissions.class.getAnnotation(Component.class);

            assertThat(component).as("TaskFlowPermissions must be a @Component").isNotNull();
            assertThat(component.value())
                    .as("SpEL references @taskFlowPermissions by name; do not rely on the default")
                    .isEqualTo("taskFlowPermissions");
        }

        /** Every bean reference in every authorization annotation must match that name. */
        @Test
        @DisplayName("no authorization annotation references a bean name that does not exist")
        void everySpelBeanReferenceResolves() {
            String expected = "@" + TaskFlowPermissions.class.getAnnotation(Component.class).value();
            List<Class<?>> services = List.of(ProjectServiceImpl.class, TaskServiceImpl.class,
                    CommentServiceImpl.class, UserServiceImpl.class);

            List<String> referenced = new ArrayList<>();
            for (Class<?> service : services) {
                for (Method m : service.getDeclaredMethods()) {
                    Stream.of(
                                    m.getAnnotation(PreAuthorize.class) == null ? null
                                            : m.getAnnotation(PreAuthorize.class).value(),
                                    m.getAnnotation(PostAuthorize.class) == null ? null
                                            : m.getAnnotation(PostAuthorize.class).value(),
                                    m.getAnnotation(PostFilter.class) == null ? null
                                            : m.getAnnotation(PostFilter.class).value())
                            .filter(Objects::nonNull)
                            .forEach(expr -> {
                                Matcher matcher = Pattern.compile("@[A-Za-z_$][\\w$]*").matcher(expr);
                                while (matcher.find()) {
                                    referenced.add(matcher.group());
                                }
                            });
                }
            }

            assertThat(referenced)
                    .as("the annotations must actually reference a bean (sanity check on this test)")
                    .isNotEmpty();
            assertThat(referenced).allSatisfy(ref -> assertThat(ref).isEqualTo(expected));
        }
    }
}
