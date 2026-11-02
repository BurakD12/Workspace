package com.example._10_integration_testing.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example._10_integration_testing.dto.CategorySummaryDto;
import com.example._10_integration_testing.dto.PagedResponse;
import com.example._10_integration_testing.dto.ProductRequestDto;
import com.example._10_integration_testing.dto.ProductResponseDto;
import com.example._10_integration_testing.dto.SupplierSummaryDto;
import com.example._10_integration_testing.exception.DuplicateResourceException;
import com.example._10_integration_testing.exception.ResourceInUseException;
import com.example._10_integration_testing.exception.ResourceNotFoundException;
import com.example._10_integration_testing.model.Category;
import com.example._10_integration_testing.model.Product;
import com.example._10_integration_testing.model.Supplier;
import com.example._10_integration_testing.repository.CategoryRepository;
import com.example._10_integration_testing.repository.ProductRepository;
import com.example._10_integration_testing.repository.StockMovementRepository;
import com.example._10_integration_testing.repository.SupplierRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductServiceImpl implements ProductService {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final SupplierRepository supplierRepository;
    private final StockMovementRepository stockMovementRepository;

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public ProductResponseDto create(ProductRequestDto request) {
        if (productRepository.existsBySkuIgnoreCase(request.getSku())) {
            throw new DuplicateResourceException("sku", request.getSku());
        }
        Category category = resolveCategory(request.getCategoryId());
        Supplier supplier = null;
        if (request.getSupplierId() != null) {
            supplier = resolveSupplier(request.getSupplierId());
        }
        Product p = Product.builder()
                .name(request.getName())
                .category(category)
                .description(request.getDescription())
                .reorderLevel(request.getReorderLevel())
                .sku(request.getSku())
                .supplier(supplier)
                .unitPrice(request.getUnitPrice())
                .quantityOnHand(0)
                .build();

        return toResponse(productRepository.save(p));
    }

    @Override
    public PagedResponse<ProductResponseDto> getAll(Long categoryId, boolean lowStock, Pageable pageable) {
        Page<Product> page;

        if (categoryId != null && lowStock) {
            page = productRepository.findLowStockByCategoryId(categoryId, pageable);
        } else if (categoryId != null) {
            page = productRepository.findByCategoryId(categoryId, pageable);
        } else if (lowStock) {
            page = productRepository.findLowStock(pageable);
        } else {
            page = productRepository.findAll(pageable);
        }

        return PagedResponse.from(page, page.getContent().stream().map(this::toResponse).toList());
    }

    @Override
    public ProductResponseDto getById(Long id) {
        return toResponse(
                productRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Product", id)));
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    @Override
    public ProductResponseDto update(Long id, ProductRequestDto request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
        if (productRepository.existsBySkuIgnoreCaseAndIdNot(request.getSku(), id)) {
            throw new DuplicateResourceException("sku", request.getSku());
        }
        product.setSku(request.getSku());
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setUnitPrice(request.getUnitPrice());
        product.setReorderLevel(request.getReorderLevel());
        product.setCategory(resolveCategory(request.getCategoryId()));
        product.setSupplier(resolveSupplier(request.getSupplierId()));

        return toResponse(productRepository.save(product));
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(Long id) {
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("Product", id);
        }
        if (stockMovementRepository.existsByProductId(id)) {
            throw new ResourceInUseException("Product", id, "stock movements");
        }

        productRepository.deleteById(id);
    }

    private Category resolveCategory(Long id) {
        return categoryRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Category", id));
    }

    private Supplier resolveSupplier(Long id) {
        if (id == null) {
            return null;
        }
        return supplierRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Supplier", id));
    }

    private ProductResponseDto toResponse(Product p) {
        Category c = p.getCategory();
        CategorySummaryDto categoryDto = c == null ? null : new CategorySummaryDto(c.getId(), c.getName());
        Supplier s = p.getSupplier();
        SupplierSummaryDto supplierDto = s == null ? null : new SupplierSummaryDto(s.getId(), s.getName());
        return new ProductResponseDto(
                p.getId(), p.getSku(), p.getName(), p.getDescription(), p.getUnitPrice(),
                p.getQuantityOnHand(), p.getReorderLevel(), p.isLowStock(), categoryDto, supplierDto);
    }
}