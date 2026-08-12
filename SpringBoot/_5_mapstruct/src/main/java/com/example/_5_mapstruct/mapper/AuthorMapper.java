package com.example._5_mapstruct.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.example._5_mapstruct.dto.AuthorRequestDto;
import com.example._5_mapstruct.dto.AuthorResponseDto;
import com.example._5_mapstruct.dto.AuthorSummaryDto;
import com.example._5_mapstruct.model.Author;

@Mapper(componentModel = "spring")
public interface AuthorMapper {
    AuthorResponseDto toResponse(Author author);
    
    AuthorSummaryDto toSummary(Author author);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "books", ignore = true)
    Author toEntity(AuthorRequestDto dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "books", ignore = true)
    void updateEntity(AuthorRequestDto dto, @MappingTarget Author author);
}
