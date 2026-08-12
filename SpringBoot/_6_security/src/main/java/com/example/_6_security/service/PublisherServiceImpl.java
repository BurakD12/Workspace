package com.example._6_security.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example._6_security.dto.PublisherRequestDto;
import com.example._6_security.dto.PublisherResponseDto;
import com.example._6_security.exception.DuplicateResourceException;
import com.example._6_security.exception.RelationshipConflictException;
import com.example._6_security.exception.ResourceNotFoundException;
import com.example._6_security.mapper.PublisherMapper;
import com.example._6_security.model.Publisher;
import com.example._6_security.repository.BookRepository;
import com.example._6_security.repository.PublisherRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PublisherServiceImpl implements PublisherService {

    private final PublisherRepository publisherRepository;
    private final BookRepository bookRepository;
    private final PublisherMapper publisherMapper;

    @Override
    @Transactional
    public PublisherResponseDto createPublisher(PublisherRequestDto requestDto) {
        if (publisherRepository.existsByNameIgnoreCase(requestDto.getName())) {
            throw new DuplicateResourceException("name", requestDto.getName());
        }
        Publisher publisher = Publisher.builder()
                .name(requestDto.getName())
                .country(requestDto.getCountry())
                .build();
        return publisherMapper.toResponse(publisherRepository.save(publisher));
    }

    @Override
    public List<PublisherResponseDto> getAllPublishers() {
        return publisherRepository.findAll().stream().map(publisherMapper::toResponse).toList();
    }

    @Override
    public PublisherResponseDto getPublisherById(Long id) {
        return publisherMapper.toResponse(getPublisherEntity(id));
    }

    @Override
    @Transactional
    public PublisherResponseDto updatePublisher(Long id, PublisherRequestDto requestDto) {
        Publisher publisher = getPublisherEntity(id);
        if (!publisher.getName().equalsIgnoreCase(requestDto.getName())
                && publisherRepository.existsByNameIgnoreCase(requestDto.getName())) {
            throw new DuplicateResourceException("name", requestDto.getName());
        }
        publisher.setName(requestDto.getName());
        publisher.setCountry(requestDto.getCountry());
        return publisherMapper.toResponse(publisherRepository.save(publisher));
    }

    @Override
    @Transactional
    public void deletePublisher(Long id) {
        Publisher publisher = getPublisherEntity(id);
        if (bookRepository.existsByPublisherId(id)) {
            throw new RelationshipConflictException(
                    "Cannot delete publisher " + id + " because it still has books");
        }
        publisherRepository.delete(publisher);
    }

    private Publisher getPublisherEntity(Long id) {
        return publisherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Publisher", id));
    }

}
