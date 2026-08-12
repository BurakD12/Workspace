package com.example._6_security.service;

import java.util.List;

import com.example._6_security.dto.CategoryRequestDto;
import com.example._6_security.dto.CategoryResponseDto;

public interface CategoryService {
    CategoryResponseDto createCategory(CategoryRequestDto requestDto);

    List<CategoryResponseDto> getAllCategories();

    CategoryResponseDto getCategoryById(Long id);

    CategoryResponseDto updateCategory(Long id, CategoryRequestDto requestDto);

    void deleteCategory(Long id);
}
