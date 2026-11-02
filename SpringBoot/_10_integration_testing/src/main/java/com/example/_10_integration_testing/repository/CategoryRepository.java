package com.example._10_integration_testing.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example._10_integration_testing.model.Category;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    /**
     * Duplicate check on create. Case-insensitive: "fasteners" and "Fasteners" are
     * the same category.
     */
    boolean existsByNameIgnoreCase(String name);

    /**
     * Duplicate check on update: "does ANOTHER category already have this name?"
     * Without the
     * {@code AndIdNot} clause, renaming a category to its own current name would be
     * refused as a
     * duplicate of itself.
     */
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
