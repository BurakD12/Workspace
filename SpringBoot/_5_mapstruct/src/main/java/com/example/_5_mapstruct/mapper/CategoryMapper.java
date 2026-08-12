package com.example._5_mapstruct.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.example._5_mapstruct.dto.CategoryRequestDto;
import com.example._5_mapstruct.dto.CategoryResponseDto;
import com.example._5_mapstruct.dto.CategorySummaryDto;
import com.example._5_mapstruct.model.Category;

@Mapper(componentModel = "spring")
public interface CategoryMapper {
    CategoryResponseDto toResponse(Category category);

    CategorySummaryDto toSummary(Category category);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "books", ignore = true)
    Category toEntity(CategoryRequestDto dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "books", ignore = true)
    void updateEntity(CategoryRequestDto dto, @MappingTarget Category category);
}
