package com.example._8_method_security.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example._8_method_security.model.Project;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    @Override
    @EntityGraph(attributePaths = "owner")
    Page<Project> findAll(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = "owner")
    Optional<Project> findById(Long id);

    @EntityGraph(attributePaths = "owner")
    Page<Project> findByOwnerUsername(String username, Pageable pageable);

    boolean existsByIdAndOwnerUsername(Long id, String username);
}
