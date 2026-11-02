package com.example._9_docker.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example._9_docker.model.Product;

public interface ProductRepository extends JpaRepository<Product, Long> {

    // Every finder that feeds DTO mapping pulls the two LAZY @ManyToOne sides with an entity graph,
    // so mapping a page of products never fires an N+1 (Lab 3 discipline). supplier may be null.
    @Override
    @EntityGraph(attributePaths = {"category", "supplier"})
    Page<Product> findAll(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"category", "supplier"})
    Optional<Product> findById(Long id);

    @EntityGraph(attributePaths = {"category", "supplier"})
    Page<Product> findByCategoryId(Long categoryId, Pageable pageable);

    /**
     * The low-stock rule, in JPQL. It mirrors {@code Product.isLowStock()} exactly — a query cannot
     * call a Java method, so the comparison lives in both places and they must be kept in step.
     */
    @EntityGraph(attributePaths = {"category", "supplier"})
    @Query("select p from Product p where p.quantityOnHand <= p.reorderLevel")
    Page<Product> findLowStock(Pageable pageable);

    @EntityGraph(attributePaths = {"category", "supplier"})
    @Query("select p from Product p where p.category.id = :categoryId and p.quantityOnHand <= p.reorderLevel")
    Page<Product> findLowStockByCategoryId(@Param("categoryId") Long categoryId, Pageable pageable);

    // --- uniqueness probes ----------------------------------------------------------------------

    boolean existsBySkuIgnoreCase(String sku);

    /** Update-time duplicate check that excludes the row being updated. */
    boolean existsBySkuIgnoreCaseAndIdNot(String sku, Long id);

    // --- "is it still referenced?" probes, asked BEFORE a delete so the answer is a 409, not a 500 --

    boolean existsByCategoryId(Long categoryId);

    boolean existsBySupplierId(Long supplierId);
    
}
