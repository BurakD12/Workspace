package com.example._9_docker.service;

import org.springframework.data.domain.Pageable;

import com.example._9_docker.dto.CategoryRequestDto;
import com.example._9_docker.dto.CategoryResponseDto;
import com.example._9_docker.dto.PagedResponse;

public interface CategoryService {
    CategoryResponseDto create(CategoryRequestDto request);
    PagedResponse<CategoryResponseDto> getAll(Pageable pageable);
    CategoryResponseDto getById(Long id);
    CategoryResponseDto update(Long id, CategoryRequestDto request);
    void delete(Long id);
}
