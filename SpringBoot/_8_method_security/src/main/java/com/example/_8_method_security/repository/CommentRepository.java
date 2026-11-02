package com.example._8_method_security.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example._8_method_security.model.Comment;

public interface CommentRepository extends JpaRepository<Comment, Long>{
    @Override
    @EntityGraph(attributePaths = {"task", "author"})
    Optional<Comment> findById(Long id);

    @EntityGraph(attributePaths = {"task", "author"})
    List<Comment> findByTaskIdOrderByCreatedAtAsc(Long taskId);

    boolean existsByIdAndAuthorUsername(Long id, String username);
}
