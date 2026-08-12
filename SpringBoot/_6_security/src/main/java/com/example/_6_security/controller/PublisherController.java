package com.example._6_security.controller;

import java.net.URI;
import java.util.List;

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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.example._6_security.dto.BookResponseDto;
import com.example._6_security.dto.PagedResponse;
import com.example._6_security.dto.PublisherRequestDto;
import com.example._6_security.dto.PublisherResponseDto;
import com.example._6_security.service.BookService;
import com.example._6_security.service.PublisherService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/publishers")
@RequiredArgsConstructor
public class PublisherController {

    private final PublisherService publisherService;
    private final BookService bookService;

    @PostMapping
    public ResponseEntity<PublisherResponseDto> create(@Valid @RequestBody PublisherRequestDto requestDto) {
        PublisherResponseDto created = publisherService.createPublisher(requestDto);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.getId()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    public ResponseEntity<List<PublisherResponseDto>> getAll() {
        return ResponseEntity.ok(publisherService.getAllPublishers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PublisherResponseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(publisherService.getPublisherById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PublisherResponseDto> update(
            @PathVariable Long id, @Valid @RequestBody PublisherRequestDto requestDto) {
        return ResponseEntity.ok(publisherService.updatePublisher(id, requestDto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        publisherService.deletePublisher(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/books")
    public ResponseEntity<PagedResponse<BookResponseDto>> getBooks(
            @PathVariable Long id,
            @PageableDefault(size = 20, sort = "title") Pageable pageable) {
        return ResponseEntity.ok(bookService.getBooksByPublisher(id, pageable));
    }
}
