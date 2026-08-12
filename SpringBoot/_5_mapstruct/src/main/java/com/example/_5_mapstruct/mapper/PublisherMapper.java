package com.example._5_mapstruct.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.example._5_mapstruct.dto.PublisherRequestDto;
import com.example._5_mapstruct.dto.PublisherResponseDto;
import com.example._5_mapstruct.dto.PublisherSummaryDto;
import com.example._5_mapstruct.model.Publisher;

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