package com.example._10_integration_testing.service;

import org.springframework.data.domain.Pageable;

import com.example._10_integration_testing.dto.CategoryRequestDto;
import com.example._10_integration_testing.dto.CategoryResponseDto;
import com.example._10_integration_testing.dto.PagedResponse;

public interface CategoryService {
    CategoryResponseDto create(CategoryRequestDto request);

    PagedResponse<CategoryResponseDto> getAll(Pageable pageable);

    CategoryResponseDto getById(Long id);

    CategoryResponseDto update(Long id, CategoryRequestDto request);

    void delete(Long id);
}
