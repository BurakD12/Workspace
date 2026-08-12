package com.example._5_mapstruct.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example._5_mapstruct.model.Publisher;

public interface PublisherRepository extends JpaRepository<Publisher, Long> {
    boolean existsByNameIgnoreCase(String name);
}
