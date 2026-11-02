package com.example._9_docker.service;

import org.springframework.data.domain.Pageable;

import com.example._9_docker.dto.PagedResponse;
import com.example._9_docker.dto.ProductRequestDto;
import com.example._9_docker.dto.ProductResponseDto;

public interface ProductService {
    ProductResponseDto create(ProductRequestDto request);
    PagedResponse<ProductResponseDto> getAll(Long categoryId, boolean lowStock, Pageable pageable);
    ProductResponseDto getById(Long id);
    ProductResponseDto update(Long id, ProductRequestDto request);
    void delete(Long id);
}
