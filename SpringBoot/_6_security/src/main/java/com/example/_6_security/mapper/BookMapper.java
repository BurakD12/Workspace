package com.example._6_security.mapper;

import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.example._6_security.dto.BookRequestDto;
import com.example._6_security.dto.BookResponseDto;
import com.example._6_security.model.Book;

/**
 * MapStruct mapper for Book. It composes the summary mappers (uses = {...}) so
 * the
 * nested author / publisher / categories are shaped by AuthorMapper,
 * PublisherMapper
 * and CategoryMapper respectively.
 *
 * <p>
 * Deliberate asymmetry carried over from Lab 3: the request -> entity mapping
 * IGNORES
 * every relationship (author, publisher, categories) and the id, because
 * resolving
 * authorId / publisherId / categoryIds to managed entities is a repository
 * lookup the
 * SERVICE still owns — never the mapper's job.
 */
@Mapper(componentModel = "spring", uses = { AuthorMapper.class, PublisherMapper.class,
        CategoryMapper.class }, injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface BookMapper {

    // Detail view: expands the fully-initialised categories collection.
    BookResponseDto toDetailResponse(Book book);

    // List view: author + publisher come from the entity graph, but categories are
    // NOT
    // fetched while paginating (Lab 3 rule). MapStruct would leave the ignored
    // collection
    // null, so we force an empty list to keep the JSON contract ("categories": []).
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
