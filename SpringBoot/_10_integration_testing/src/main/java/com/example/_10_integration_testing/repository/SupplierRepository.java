package com.example._10_integration_testing.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example._10_integration_testing.model.Supplier;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {
    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
