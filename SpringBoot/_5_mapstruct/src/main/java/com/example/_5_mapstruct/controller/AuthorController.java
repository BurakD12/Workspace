package com.example._5_mapstruct.controller;

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

import com.example._5_mapstruct.dto.AuthorRequestDto;
import com.example._5_mapstruct.dto.AuthorResponseDto;
import com.example._5_mapstruct.dto.BookResponseDto;
import com.example._5_mapstruct.dto.PagedResponse;
import com.example._5_mapstruct.service.AuthorService;
import com.example._5_mapstruct.service.BookService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/authors")
@RequiredArgsConstructor
public class AuthorController {

    private final AuthorService authorService;
    private final BookService bookService;

    @PostMapping
    public ResponseEntity<AuthorResponseDto> create(@Valid @RequestBody AuthorRequestDto requestDto) {
        AuthorResponseDto created = authorService.createAuthor(requestDto);
        // Why below code is used? It is used to create a URI for the newly created resource. 
        // The URI is constructed based on the current request's URI and appending the ID of the 
        // newly created author. This is a common practice in RESTful APIs to provide the client with
        // the location of the newly created resource. The ResponseEntity.created(location) method
        // is then used to return a response with a 201 Created status and the Location header
        // set to the URI of the newly created resource. This allows clients to easily access the
        // newly created resource by following the provided URI.
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.getId()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    public ResponseEntity<List<AuthorResponseDto>> getAll() {
        return ResponseEntity.ok(authorService.getAllAuthors());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AuthorResponseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(authorService.getAuthorById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AuthorResponseDto> update(
            @PathVariable Long id, @Valid @RequestBody AuthorRequestDto requestDto) {
        return ResponseEntity.ok(authorService.updateAuthor(id, requestDto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        authorService.deleteAuthor(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/books")
    public ResponseEntity<PagedResponse<BookResponseDto>> getBooks(
            @PathVariable Long id,
            @PageableDefault(size = 20, sort = "title") Pageable pageable) {
        return ResponseEntity.ok(bookService.getBooksByAuthor(id, pageable));
    }

}
