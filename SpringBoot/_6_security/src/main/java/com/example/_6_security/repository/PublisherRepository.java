package com.example._6_security.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example._6_security.model.Publisher;

public interface PublisherRepository extends JpaRepository<Publisher, Long> {
    boolean existsByNameIgnoreCase(String name);
}
