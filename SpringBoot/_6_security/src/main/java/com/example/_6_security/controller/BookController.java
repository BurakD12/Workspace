package com.example._6_security.controller;

import java.math.BigDecimal;
import java.net.URI;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.example._6_security.dto.BookRequestDto;
import com.example._6_security.dto.BookResponseDto;
import com.example._6_security.dto.BookSearchCriteria;
import com.example._6_security.dto.PagedResponse;
import com.example._6_security.projection.BookSummaryView;
import com.example._6_security.service.BookService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;

    @PostMapping
    public ResponseEntity<BookResponseDto> create(@Valid @RequestBody BookRequestDto requestDto) {
        BookResponseDto created = bookService.createBook(requestDto);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.getId()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    public ResponseEntity<PagedResponse<BookResponseDto>> getAll(
            @RequestParam(required = false) Long categoryId,
            @PageableDefault(size = 20, sort = "title") Pageable pageable) {
        if (categoryId != null) {
            return ResponseEntity.ok(bookService.getBooksByCategory(categoryId, pageable));
        }
        return ResponseEntity.ok(bookService.getAllBooks(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookResponseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(bookService.getBookById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BookResponseDto> update(
            @PathVariable Long id, @Valid @RequestBody BookRequestDto requestDto) {
        return ResponseEntity.ok(bookService.updateBook(id, requestDto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        bookService.deleteBook(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    public ResponseEntity<PagedResponse<BookResponseDto>> search(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String author,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Integer minYear,
            @RequestParam(required = false) Integer maxYear,
            @PageableDefault(size = 20, sort = "title") Pageable pageable) {
        BookSearchCriteria criteria = new BookSearchCriteria(title, author, category, minPrice, maxPrice, minYear,
                maxYear);
        return ResponseEntity.ok(bookService.searchBooks(criteria, pageable));
    }

    @GetMapping("/summaries")
    public ResponseEntity<PagedResponse<BookSummaryView>> getSummaryView(
            @PageableDefault(size = 20, sort = "title") Pageable pageable) {
        return ResponseEntity.ok(bookService.getBookSummaries(pageable));
    }
}
