package com.example._5_mapstruct.service;

import java.util.List;

import com.example._5_mapstruct.dto.AuthorRequestDto;
import com.example._5_mapstruct.dto.AuthorResponseDto;





public interface AuthorService {
    AuthorResponseDto createAuthor(AuthorRequestDto requestDto);
    List<AuthorResponseDto> getAllAuthors();
    AuthorResponseDto getAuthorById(Long id);
    AuthorResponseDto updateAuthor(Long id, AuthorRequestDto requestDto);
    void deleteAuthor(Long id);
}
