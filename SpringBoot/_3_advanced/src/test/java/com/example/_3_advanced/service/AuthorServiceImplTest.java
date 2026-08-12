package com.example._3_advanced.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example._3_advanced.dto.AuthorRequestDto;
import com.example._3_advanced.dto.AuthorResponseDto;
import com.example._3_advanced.exception.RelationshipConflictException;
import com.example._3_advanced.exception.ResourceNotFoundException;
import com.example._3_advanced.model.Author;
import com.example._3_advanced.repository.AuthorRepository;
import com.example._3_advanced.repository.BookRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthorServiceImpl")
class AuthorServiceImplTest {

    @Mock private AuthorRepository authorRepository;
    @Mock private BookRepository bookRepository;
    @InjectMocks private AuthorServiceImpl service;

    @Test
    @DisplayName("createAuthor saves and returns the mapped response")
    void createAuthor() {
        AuthorRequestDto req = new AuthorRequestDto("Robert", "Martin", "Author of Clean Code");
        given(authorRepository.save(any(Author.class))).willAnswer(inv -> {
            Author a = inv.getArgument(0);
            a.setId(1L);
            return a;
        });

        AuthorResponseDto res = service.createAuthor(req);

        assertThat(res.getId()).isEqualTo(1L);
        assertThat(res.getFirstName()).isEqualTo("Robert");
        assertThat(res.getLastName()).isEqualTo("Martin");
    }

    @Test
    @DisplayName("getAuthorById throws ResourceNotFoundException when missing")
    void getAuthorByIdMissing() {
        given(authorRepository.findById(9999L)).willReturn(Optional.empty());
        assertThatThrownBy(() -> service.getAuthorById(9999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("9999");
    }

    @Test
    @DisplayName("deleteAuthor throws RelationshipConflictException when the author still has books")
    void deleteAuthorWithBooks() {
        Author author = Author.builder().id(1L).firstName("Robert").lastName("Martin").build();
        given(authorRepository.findById(1L)).willReturn(Optional.of(author));
        given(bookRepository.existsByAuthorId(1L)).willReturn(true);

        assertThatThrownBy(() -> service.deleteAuthor(1L))
                .isInstanceOf(RelationshipConflictException.class);
        verify(authorRepository, never()).delete(any(Author.class));
    }

    @Test
    @DisplayName("deleteAuthor deletes when the author has no books")
    void deleteAuthorWithoutBooks() {
        Author author = Author.builder().id(1L).firstName("Robert").lastName("Martin").build();
        given(authorRepository.findById(1L)).willReturn(Optional.of(author));
        given(bookRepository.existsByAuthorId(1L)).willReturn(false);

        service.deleteAuthor(1L);

        verify(authorRepository).delete(author);
    }
}
