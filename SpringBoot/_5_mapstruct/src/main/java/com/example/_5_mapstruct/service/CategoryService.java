package com.example._5_mapstruct.service;

import java.util.List;

import com.example._5_mapstruct.dto.CategoryRequestDto;
import com.example._5_mapstruct.dto.CategoryResponseDto;





public interface CategoryService {
    CategoryResponseDto createCategory(CategoryRequestDto requestDto);
    List<CategoryResponseDto> getAllCategories();
    CategoryResponseDto getCategoryById(Long id);
    CategoryResponseDto updateCategory(Long id, CategoryRequestDto requestDto);
    void deleteCategory(Long id);
}
