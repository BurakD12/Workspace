package com.example._6_security.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.example._6_security.dto.PublisherRequestDto;
import com.example._6_security.dto.PublisherResponseDto;
import com.example._6_security.dto.PublisherSummaryDto;
import com.example._6_security.model.Publisher;

/**
 * MapStruct mapper for Publisher. Request -> entity ignores id (DB-generated)
 * and the books back-reference.
 */
@Mapper(componentModel = "spring")
public interface PublisherMapper {

    PublisherResponseDto toResponse(Publisher publisher);

    PublisherSummaryDto toSummary(Publisher publisher);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "books", ignore = true)
    Publisher toEntity(PublisherRequestDto dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "books", ignore = true)
    void updateEntity(PublisherRequestDto dto, @MappingTarget Publisher publisher);
}
