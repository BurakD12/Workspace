package com.example._5_mapstruct.service;

import java.util.HashSet;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example._5_mapstruct.dto.CategoryRequestDto;
import com.example._5_mapstruct.dto.CategoryResponseDto;
import com.example._5_mapstruct.exception.DuplicateResourceException;
import com.example._5_mapstruct.exception.ResourceNotFoundException;
import com.example._5_mapstruct.mapper.CategoryMapper;
import com.example._5_mapstruct.model.Book;
import com.example._5_mapstruct.model.Category;
import com.example._5_mapstruct.repository.CategoryRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    @Override
    @Transactional
    public CategoryResponseDto createCategory(CategoryRequestDto requestDto) {
        if (categoryRepository.existsByNameIgnoreCase(requestDto.getName())) {
            throw new DuplicateResourceException("name", requestDto.getName());
        }
        Category category = Category.builder().name(requestDto.getName()).build();
        return categoryMapper.toResponse(categoryRepository.save(category));
    }

    @Override
    public List<CategoryResponseDto> getAllCategories() {
        return categoryRepository.findAll().stream().map(categoryMapper::toResponse).toList();
    }

    @Override
    public CategoryResponseDto getCategoryById(Long id) {
        return categoryMapper.toResponse(getCategoryEntity(id));
    }

    @Override
    @Transactional
    public CategoryResponseDto updateCategory(Long id, CategoryRequestDto requestDto) {
        Category category = getCategoryEntity(id);
        if (!category.getName().equalsIgnoreCase(requestDto.getName())
                && categoryRepository.existsByNameIgnoreCase(requestDto.getName())) {
            throw new DuplicateResourceException("name", requestDto.getName());
        }
        category.setName(requestDto.getName());
        return categoryMapper.toResponse(categoryRepository.save(category));
    }

    @Override
    @Transactional
    public void deleteCategory(Long id) {
        Category category = getCategoryEntity(id);
        // Book is the owning side of the join table, so detach from each book first.
        for (Book book : new HashSet<>(category.getBooks())) {
            book.getCategories().remove(category);
        }
        category.getBooks().clear();
        categoryRepository.delete(category);
    }

    private Category getCategoryEntity(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));
    }
}
