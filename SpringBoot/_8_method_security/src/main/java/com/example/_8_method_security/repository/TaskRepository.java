package com.example._8_method_security.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example._8_method_security.model.Status;
import com.example._8_method_security.model.Task;

public interface TaskRepository extends JpaRepository<Task, Long> {
    @Override
    @EntityGraph(attributePaths = { "project", "assignee" })
    Page<Task> findAll(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = { "project", "assignee" })
    Optional<Task> findById(Long id);

    @EntityGraph(attributePaths = { "project", "assignee" })
    Page<Task> findByProjectId(Long projectId, Pageable pageable);

    @EntityGraph(attributePaths = { "project", "assignee" })
    Page<Task> findByAssigneeId(Long assigneeId, Pageable pageable);

    @EntityGraph(attributePaths = { "project", "assignee" })
    Page<Task> findByStatus(Status status, Pageable pageable);

    boolean existsByIdAndProjectOwnerUsername(Long id, String username);

    boolean existsByIdAndAssigneeUsername(Long id, String username);

    boolean existsByIdAndProjectId(Long id, Long projectId);
}
