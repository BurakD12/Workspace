package com.example._6_security.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.example._6_security.dto.CategoryRequestDto;
import com.example._6_security.dto.CategoryResponseDto;
import com.example._6_security.dto.CategorySummaryDto;
import com.example._6_security.model.Category;

/**
 * MapStruct mapper for Category. Request -> entity ignores id (DB-generated)
 * and the books back-reference (owned by Book's join table).
 */
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
