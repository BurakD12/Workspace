package com.example._10_integration_testing.service;

import org.springframework.data.domain.Pageable;

import com.example._10_integration_testing.dto.PagedResponse;
import com.example._10_integration_testing.dto.ProductRequestDto;
import com.example._10_integration_testing.dto.ProductResponseDto;

public interface ProductService {
    ProductResponseDto create(ProductRequestDto request);

    PagedResponse<ProductResponseDto> getAll(Long categoryId, boolean lowStock, Pageable pageable);

    ProductResponseDto getById(Long id);

    ProductResponseDto update(Long id, ProductRequestDto request);

    void delete(Long id);
}
