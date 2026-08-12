package com.example._7_jwt.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example._7_jwt.model.Status;
import com.example._7_jwt.model.Task;

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
}
