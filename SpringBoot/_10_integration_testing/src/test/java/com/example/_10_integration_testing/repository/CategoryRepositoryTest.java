package com.example._10_integration_testing.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import com.example._10_integration_testing.model.Category;

@DataJpaTest
@DisplayName("CategoryRepository (uniqueness probes)")
class CategoryRepositoryTest {

    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private TestEntityManager em;

    private Long fastenersId;

    @BeforeEach
    void seed() {
        Category fasteners = Category.builder().name("Fasteners").build();
        em.persist(fasteners);
        em.persist(Category.builder().name("Power Tools").build());
        em.flush();
        em.clear();
        fastenersId = fasteners.getId();
    }

    @Test
    @DisplayName("existsByNameIgnoreCase matches regardless of case")
    void existsIgnoringCase() {
        assertThat(categoryRepository.existsByNameIgnoreCase("Fasteners")).isTrue();
        assertThat(categoryRepository.existsByNameIgnoreCase("FASTENERS")).isTrue();
        assertThat(categoryRepository.existsByNameIgnoreCase("fasteners")).isTrue();
        assertThat(categoryRepository.existsByNameIgnoreCase("Plumbing")).isFalse();
    }

    @Test
    @DisplayName("existsByNameIgnoreCaseAndIdNot excludes the row being updated (a no-op rename is not a duplicate)")
    void existsExcludingSelf() {
        // Renaming Fasteners to "fasteners" is fine: the only match IS this row.
        assertThat(categoryRepository.existsByNameIgnoreCaseAndIdNot("fasteners", fastenersId)).isFalse();
        // Renaming Fasteners to "Power Tools" collides with the other row.
        assertThat(categoryRepository.existsByNameIgnoreCaseAndIdNot("power tools", fastenersId)).isTrue();
    }
}
