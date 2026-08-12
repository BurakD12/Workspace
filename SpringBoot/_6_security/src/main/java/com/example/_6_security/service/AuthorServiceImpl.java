package com.example._6_security.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example._6_security.dto.AuthorRequestDto;
import com.example._6_security.dto.AuthorResponseDto;
import com.example._6_security.exception.RelationshipConflictException;
import com.example._6_security.exception.ResourceNotFoundException;
import com.example._6_security.mapper.AuthorMapper;
import com.example._6_security.model.Author;
import com.example._6_security.repository.AuthorRepository;
import com.example._6_security.repository.BookRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthorServiceImpl implements AuthorService {

    private final AuthorRepository authorRepository;
    private final BookRepository bookRepository;
    private final AuthorMapper authorMapper;

    @Override
    @Transactional
    public AuthorResponseDto createAuthor(AuthorRequestDto requestDto) {
        Author author = Author.builder()
                .firstName(requestDto.getFirstName())
                .lastName(requestDto.getLastName())
                .bio(requestDto.getBio())
                .build();
        return authorMapper.toResponse(authorRepository.save(author));
    }

    @Override
    public List<AuthorResponseDto> getAllAuthors() {
        return authorRepository.findAll().stream().map(authorMapper::toResponse).toList();
    }

    @Override
    public AuthorResponseDto getAuthorById(Long id) {
        return authorMapper.toResponse(getAuthorEntity(id));
    }

    @Override
    @Transactional
    public AuthorResponseDto updateAuthor(Long id, AuthorRequestDto requestDto) {
        Author author = getAuthorEntity(id);
        author.setFirstName(requestDto.getFirstName());
        author.setLastName(requestDto.getLastName());
        author.setBio(requestDto.getBio());
        return authorMapper.toResponse(authorRepository.save(author));
    }

    @Override
    @Transactional
    public void deleteAuthor(Long id) {
        Author author = getAuthorEntity(id);
        if (bookRepository.existsByAuthorId(id)) {
            throw new RelationshipConflictException(
                    "Cannot delete author " + id + " because they still have books");
        }
        authorRepository.delete(author);
    }

    private Author getAuthorEntity(Long id) {
        return authorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Author", id));
    }

}
