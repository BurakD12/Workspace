package com.example._10_integration_testing.integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Transactional;

import com.example._10_integration_testing.integration.support.RequestMother;
import com.example._10_integration_testing.repository.CategoryRepository;
import com.example._10_integration_testing.repository.UserRepository;

/**
 * Lab 10 — isolation, part 1: with {@code webEnvironment = MOCK} a {@code @Transactional} test
 * <em>does</em> undo what the API wrote.
 *
 * <p>The mock servlet dispatches the request on the <b>test thread</b>. The service's own
 * {@code @Transactional} (propagation REQUIRED) finds the test-managed transaction already open on
 * that thread and joins it. When the test method ends, {@code TransactionalTestExecutionListener} rolls
 * that one transaction back — controller write included — and no cleanup script is needed.
 *
 * <p>The methods are ordered on purpose. Ordinary tests must never depend on each other; here the
 * dependency <em>is</em> the demonstration: test 2 can only pass if test 1's row vanished.
 */
@Transactional
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TransactionalRollbackIsolationTest extends AbstractMockMvcIntegrationTest {

    private static final String PROBE_NAME = "Rollback Probe";

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    @Order(1)
    @DisplayName("1. a category created through the API is visible inside the test transaction")
    void createdRowIsVisibleInsideTheTestTransaction() {
        assertThat(TestTransaction.isActive()).isTrue();
        assertThat(TestTransaction.isFlaggedForRollback()).isTrue();

        assertThat(mvc.post().uri("/api/categories")
                .header(HttpHeaders.AUTHORIZATION, adminBearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(RequestMother.category().name(PROBE_NAME).build()))
                .exchange())
                .hasStatus(HttpStatus.CREATED);

        // same thread, same transaction: the repository sees the uncommitted row
        assertThat(categoryRepository.existsByNameIgnoreCase(PROBE_NAME)).isTrue();
    }

    @Test
    @Order(2)
    @DisplayName("2. the row is gone: test 1's transaction was rolled back, controller write included")
    void rowWasRolledBack() {
        assertThat(categoryRepository.existsByNameIgnoreCase(PROBE_NAME)).isFalse();
        assertThat(categoryRepository.count()).isZero();
    }

    @Test
    @Order(3)
    @DisplayName("3. the seeded admin survives: it was committed at startup, outside any test transaction")
    void startupSeedIsNotAffected() {
        assertThat(userRepository.existsByUsername(ADMIN_USERNAME)).isTrue();
    }
}
