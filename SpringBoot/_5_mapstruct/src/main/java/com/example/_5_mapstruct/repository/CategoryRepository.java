package com.example._5_mapstruct.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example._5_mapstruct.model.Category;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    boolean existsByNameIgnoreCase(String name);
    Optional<Category> findByNameIgnoreCase(String name);
}
