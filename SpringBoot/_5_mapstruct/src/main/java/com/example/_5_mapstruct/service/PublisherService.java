package com.example._5_mapstruct.service;

import java.util.List;

import com.example._5_mapstruct.dto.PublisherRequestDto;
import com.example._5_mapstruct.dto.PublisherResponseDto;





public interface PublisherService {
    PublisherResponseDto createPublisher(PublisherRequestDto requestDto);
    List<PublisherResponseDto> getAllPublishers();
    PublisherResponseDto getPublisherById(Long id);
    PublisherResponseDto updatePublisher(Long id, PublisherRequestDto requestDto);
    void deletePublisher(Long id);
}
