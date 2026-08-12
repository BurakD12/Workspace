package com.example._6_security.service;

import java.util.List;

import com.example._6_security.dto.AuthorRequestDto;
import com.example._6_security.dto.AuthorResponseDto;

public interface AuthorService {
    AuthorResponseDto createAuthor(AuthorRequestDto requestDto);

    List<AuthorResponseDto> getAllAuthors();

    AuthorResponseDto getAuthorById(Long id);

    AuthorResponseDto updateAuthor(Long id, AuthorRequestDto requestDto);

    void deleteAuthor(Long id);
}
