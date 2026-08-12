package com.example._6_security.service;

import java.util.List;

import com.example._6_security.dto.PublisherRequestDto;
import com.example._6_security.dto.PublisherResponseDto;

public interface PublisherService {
    PublisherResponseDto createPublisher(PublisherRequestDto requestDto);

    List<PublisherResponseDto> getAllPublishers();

    PublisherResponseDto getPublisherById(Long id);

    PublisherResponseDto updatePublisher(Long id, PublisherRequestDto requestDto);

    void deletePublisher(Long id);
}
