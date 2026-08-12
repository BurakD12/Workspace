package com.example._5_mapstruct.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example._5_mapstruct.model.Author;

public interface AuthorRepository extends JpaRepository<Author, Long> {
}
