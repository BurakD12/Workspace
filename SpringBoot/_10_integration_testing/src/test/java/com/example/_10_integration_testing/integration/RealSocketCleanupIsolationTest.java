package com.example._10_integration_testing.integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Transactional;

import com.example._10_integration_testing.integration.support.RequestMother;
import com.example._10_integration_testing.repository.CategoryRepository;

/**
 * Lab 10 — isolation, part 2: on a <b>real socket</b> a {@code @Transactional} test does <em>not</em>
 * undo what the API wrote, so real-socket tests must clean up explicitly.
 *
 * <p>The request leaves the test thread, crosses a TCP connection and is handled by a Tomcat worker
 * thread. Spring transactions are bound to a thread: the service opens and <b>commits</b> its own
 * transaction on the worker, and the test-managed transaction on the test thread — which holds no
 * work at all — is rolled back to no effect. The only thing that removes the row is a script we run
 * ourselves: {@code @Sql(executionPhase = AFTER_TEST_METHOD)}.
 *
 * <p>Ordered on purpose, like {@code TransactionalRollbackIsolationTest}: the dependency is the lesson.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RealSocketCleanupIsolationTest extends AbstractRestIntegrationTest {

    private static final String PROBE_NAME = "Socket Probe";

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    @Order(1)
    @Transactional
    @DisplayName("1. inside a @Transactional test, the server has already committed the row")
    void serverCommitsOnItsOwnThread() {
        assertThat(TestTransaction.isActive()).isTrue();

        client.post().uri("/api/categories")
                .header(HttpHeaders.AUTHORIZATION, adminBearer())
                .contentType(MediaType.APPLICATION_JSON)
                .body(RequestMother.category().name(PROBE_NAME).build())
                .exchange()
                .expectStatus().isCreated();

        // the test transaction is still open and will be rolled back — but the row is already
        // committed by the worker thread's transaction, so it is visible from here too
        assertThat(categoryRepository.existsByNameIgnoreCase(PROBE_NAME)).isTrue();
    }

    @Test
    @Order(2)
    @Sql(scripts = "/sql/cleanup-catalogue.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    @DisplayName("2. the row survived test 1's rollback, so an explicit cleanup script is required")
    void rowSurvivedTheRollback() {
        assertThat(categoryRepository.existsByNameIgnoreCase(PROBE_NAME))
                .as("rollback on the test thread cannot reach the server-side transaction")
                .isTrue();
    }

    @Test
    @Order(3)
    @DisplayName("3. after the AFTER_TEST_METHOD script the table is empty again")
    void cleanupScriptEmptiedTheTable() {
        assertThat(categoryRepository.existsByNameIgnoreCase(PROBE_NAME)).isFalse();
        assertThat(categoryRepository.count()).isZero();
    }
}
