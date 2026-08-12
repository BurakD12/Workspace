package com.example._6_security.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example._6_security.model.Author;

public interface AuthorRepository extends JpaRepository<Author, Long> {
}
