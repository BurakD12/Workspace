package com.example._9_docker.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example._9_docker.dto.CategoryRequestDto;
import com.example._9_docker.dto.CategoryResponseDto;
import com.example._9_docker.dto.PagedResponse;
import com.example._9_docker.exception.DuplicateResourceException;
import com.example._9_docker.exception.ResourceInUseException;
import com.example._9_docker.exception.ResourceNotFoundException;
import com.example._9_docker.model.Category;
import com.example._9_docker.repository.CategoryRepository;
import com.example._9_docker.repository.ProductRepository;

import lombok.RequiredArgsConstructor;

/**
 * Lab 9 (StockFlow) — category CRUD with manual entity↔DTO mapping.
 *
 * <p><b>Authorization.</b> Reads are open to any authenticated user; every write is ADMIN-only. The
 * URL rules in {@code SecurityConfig} already enforce that for the HTTP paths, and the
 * {@code @PreAuthorize} annotations below enforce it again on the method — the defense-in-depth
 * pattern from Lab 8: the URL rule protects one path, the annotation protects the operation for any
 * future caller.
 *
 * <p><b>The delete rule.</b> A category that still has products cannot be deleted. The service asks
 * the question ({@code productRepository.existsByCategoryId}) and answers 409 itself. It never lets
 * the {@code products.category_id} foreign key be the thing that refuses, because a constraint
 * violation reaches the client as a 500.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public CategoryResponseDto create(CategoryRequestDto request) {
        if (categoryRepository.existsByNameIgnoreCase(request.getName())) {
            throw new DuplicateResourceException("category name", request.getName());
        }
        Category category = Category.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();
        return toResponse(categoryRepository.save(category));
    }

    @Override
    public PagedResponse<CategoryResponseDto> getAll(Pageable pageable) {
        Page<Category> page = categoryRepository.findAll(pageable);
        return PagedResponse.from(page, page.getContent().stream().map(this::toResponse).toList());
    }

    @Override
    public CategoryResponseDto getById(Long id) {
        return toResponse(categoryRepository.findById(id).orElseThrow(
                    () -> new ResourceNotFoundException("Category", id)));
    }

    @Override
    @Transactional 
    @PreAuthorize("hasRole('ADMIN')")
    public CategoryResponseDto update(Long id, CategoryRequestDto request) {
        Category category = categoryRepository.findById(id).orElseThrow(
                    () -> new ResourceNotFoundException("Category", id));
        if (categoryRepository.existsByNameIgnoreCaseAndIdNot(request.getName(), id)) {
            throw new DuplicateResourceException("category name", request.getName());
        }

        category.setName(request.getName());
        category.setDescription(request.getDescription());
        return toResponse(categoryRepository.save(category));
    }

    @Override
    @Transactional 
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new ResourceNotFoundException("Category", id);
        }
        if (productRepository.existsByCategoryId(id)) {
            throw new ResourceInUseException("Category", id, "products");
        }

        categoryRepository.deleteById(id);
    }

    private CategoryResponseDto toResponse(Category category) {
        return new CategoryResponseDto(category.getId(), category.getName(), category.getDescription());
    }

    private Category toEntity(CategoryRequestDto request) {
        return Category.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();
    }
    
}
