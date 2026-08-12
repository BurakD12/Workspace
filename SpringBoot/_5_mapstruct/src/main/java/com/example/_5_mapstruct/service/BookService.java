package com.example._5_mapstruct.service;

import org.springframework.data.domain.Pageable;

import com.example._5_mapstruct.dto.BookRequestDto;
import com.example._5_mapstruct.dto.BookResponseDto;
import com.example._5_mapstruct.dto.BookSearchCriteria;
import com.example._5_mapstruct.dto.PagedResponse;
import com.example._5_mapstruct.projection.BookSummaryView;





public interface BookService {
    BookResponseDto createBook(BookRequestDto requestDto);
    PagedResponse<BookResponseDto> getAllBooks(Pageable pageable);
    PagedResponse<BookResponseDto> getBooksByCategory(Long categoryId, Pageable pageable);
    PagedResponse<BookResponseDto> getBooksByAuthor(Long authorId, Pageable pageable);
    PagedResponse<BookResponseDto> getBooksByPublisher(Long publisherId, Pageable pageable);
    BookResponseDto getBookById(Long id);
    BookResponseDto updateBook(Long id, BookRequestDto requestDto);
    void deleteBook(Long id);
    PagedResponse<BookResponseDto> searchBooks(BookSearchCriteria criteria, Pageable pageable);
    PagedResponse<BookSummaryView> getBookSummaries(Pageable pageable);
}

