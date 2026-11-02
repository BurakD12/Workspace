package com.example._9_docker.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example._9_docker.model.Supplier;

public interface SupplierRepository extends JpaRepository<Supplier, Long>{
    boolean existsByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
