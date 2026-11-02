package com.example._10_integration_testing.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example._10_integration_testing.model.StockMovement;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {
    @EntityGraph(attributePaths = { "product", "recordedBy" })
    Page<StockMovement> findByProductId(Long productId, Pageable pageable);

    boolean existsByProductId(Long productId);
}
