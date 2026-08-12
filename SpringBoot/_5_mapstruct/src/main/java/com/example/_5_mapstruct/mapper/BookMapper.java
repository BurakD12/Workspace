package com.example._5_mapstruct.mapper;

import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.example._5_mapstruct.dto.BookRequestDto;
import com.example._5_mapstruct.dto.BookResponseDto;
import com.example._5_mapstruct.model.Book;

@Mapper(componentModel = "spring", 
        uses = {AuthorMapper.class, PublisherMapper.class, CategoryMapper.class},
        injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface BookMapper {
    BookResponseDto toDetailResponse(Book book);

    @Mapping(target = "categories", expression = "java(java.util.List.of())")
    BookResponseDto toListResponse(Book book);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "author", ignore = true)
    @Mapping(target = "publisher", ignore = true)
    @Mapping(target = "categories", ignore = true)
    Book toEntity(BookRequestDto dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "author", ignore = true)
    @Mapping(target = "publisher", ignore = true)
    @Mapping(target = "categories", ignore = true)
    void updateEntity(BookRequestDto dto, @MappingTarget Book book);
}