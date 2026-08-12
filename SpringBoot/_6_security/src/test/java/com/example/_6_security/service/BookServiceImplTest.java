package com.example._6_security.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import com.example._6_security.dto.BookRequestDto;
import com.example._6_security.dto.BookResponseDto;
import com.example._6_security.dto.BookSearchCriteria;
import com.example._6_security.dto.PagedResponse;
import com.example._6_security.exception.DuplicateResourceException;
import com.example._6_security.exception.ResourceNotFoundException;
import com.example._6_security.mapper.AuthorMapperImpl;
import com.example._6_security.mapper.BookMapperImpl;
import com.example._6_security.mapper.CategoryMapperImpl;
import com.example._6_security.mapper.PublisherMapperImpl;
import com.example._6_security.model.Author;
import com.example._6_security.model.Book;
import com.example._6_security.model.Category;
import com.example._6_security.model.Publisher;
import com.example._6_security.repository.AuthorRepository;
import com.example._6_security.repository.BookRepository;
import com.example._6_security.repository.CategoryRepository;
import com.example._6_security.repository.PublisherRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("BookServiceImpl")
class BookServiceImplTest {

    @Mock private BookRepository bookRepository;
    @Mock private AuthorRepository authorRepository;
    @Mock private PublisherRepository publisherRepository;
    @Mock private CategoryRepository categoryRepository;
    private BookServiceImpl service;

    // Compose the real (generated) MapStruct mappers exactly as Spring would; repositories mocked.
    @BeforeEach
    void setUp() {
        BookMapperImpl bookMapper = new BookMapperImpl(
                new AuthorMapperImpl(), new PublisherMapperImpl(), new CategoryMapperImpl());
        service = new BookServiceImpl(
                bookRepository, authorRepository, publisherRepository, categoryRepository, bookMapper);
    }

    private BookRequestDto validRequest() {
        return BookRequestDto.builder()
                .title("Clean Code")
                .isbn("9780132350884")
                .publicationYear(2008)
                .price(new BigDecimal("42.50"))
                .authorId(1L)
                .publisherId(2L)
                .categoryIds(List.of(3L))
                .build();
    }

    @Test
    @DisplayName("createBook throws DuplicateResourceException for an existing ISBN")
    void createDuplicateIsbn() {
        given(bookRepository.existsByIsbn("9780132350884")).willReturn(true);
        assertThatThrownBy(() -> service.createBook(validRequest()))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("isbn");
    }

    @Test
    @DisplayName("createBook throws ResourceNotFoundException when the author id is unknown")
    void createUnknownAuthor() {
        given(bookRepository.existsByIsbn(any())).willReturn(false);
        given(authorRepository.findById(1L)).willReturn(Optional.empty());
        assertThatThrownBy(() -> service.createBook(validRequest()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Author");
    }

    @Test
    @DisplayName("createBook throws ResourceNotFoundException when a category id is unknown")
    void createUnknownCategory() {
        given(bookRepository.existsByIsbn(any())).willReturn(false);
        given(authorRepository.findById(1L)).willReturn(Optional.of(
                Author.builder().id(1L).firstName("Robert").lastName("Martin").build()));
        given(publisherRepository.findById(2L)).willReturn(Optional.of(
                Publisher.builder().id(2L).name("Prentice Hall").country("USA").build()));
        given(categoryRepository.findById(3L)).willReturn(Optional.empty());
        assertThatThrownBy(() -> service.createBook(validRequest()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Category");
    }

    @Test
    @DisplayName("createBook resolves relations, saves, and returns a fully nested response")
    void createHappyPath() {
        given(bookRepository.existsByIsbn(any())).willReturn(false);
        given(authorRepository.findById(1L)).willReturn(Optional.of(
                Author.builder().id(1L).firstName("Robert").lastName("Martin").build()));
        given(publisherRepository.findById(2L)).willReturn(Optional.of(
                Publisher.builder().id(2L).name("Prentice Hall").country("USA").build()));
        given(categoryRepository.findById(3L)).willReturn(Optional.of(
                Category.builder().id(3L).name("Software").build()));
        given(bookRepository.save(any(Book.class))).willAnswer(inv -> {
            Book b = inv.getArgument(0);
            b.setId(10L);
            return b;
        });

        BookResponseDto res = service.createBook(validRequest());

        assertThat(res.getId()).isEqualTo(10L);
        assertThat(res.getTitle()).isEqualTo("Clean Code");
        assertThat(res.getAuthor().getLastName()).isEqualTo("Martin");
        assertThat(res.getPublisher().getName()).isEqualTo("Prentice Hall");
        assertThat(res.getCategories()).extracting(c -> c.getName()).containsExactly("Software");
    }

    @Test
    @DisplayName("getBookById throws ResourceNotFoundException when missing")
    void getByIdMissing() {
        given(bookRepository.findDetailedById(9999L)).willReturn(Optional.empty());
        assertThatThrownBy(() -> service.getBookById(9999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Book");
    }

    @Test
    @DisplayName("getBooksByAuthor throws ResourceNotFoundException when the author does not exist")
    void getBooksByUnknownAuthor() {
        given(authorRepository.existsById(9999L)).willReturn(false);
        assertThatThrownBy(() -> service.getBooksByAuthor(9999L, PageRequest.of(0, 10)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Author");
    }

    @Test
    @DisplayName("updateBook updates fields and does NOT re-check the ISBN when it is unchanged")
    void updateUnchangedIsbn() {
        Book existing = Book.builder().id(5L).title("Old Title").isbn("9780132350884")
                .publicationYear(2000).price(new BigDecimal("10.00"))
                .author(Author.builder().id(1L).firstName("Robert").lastName("Martin").build())
                .publisher(Publisher.builder().id(2L).name("Prentice Hall").country("USA").build())
                .build();
        given(bookRepository.findDetailedById(5L)).willReturn(Optional.of(existing));
        given(authorRepository.findById(1L)).willReturn(Optional.of(
                Author.builder().id(1L).firstName("Robert").lastName("Martin").build()));
        given(publisherRepository.findById(2L)).willReturn(Optional.of(
                Publisher.builder().id(2L).name("Prentice Hall").country("USA").build()));
        given(categoryRepository.findById(3L)).willReturn(Optional.of(
                Category.builder().id(3L).name("Software").build()));
        given(bookRepository.save(any(Book.class))).willAnswer(inv -> inv.getArgument(0));

        BookResponseDto res = service.updateBook(5L, validRequest());

        assertThat(res.getTitle()).isEqualTo("Clean Code");
        assertThat(res.getIsbn()).isEqualTo("9780132350884");
        verify(bookRepository, never()).existsByIsbn(any()); // unchanged isbn => no uniqueness query
    }

    @Test
    @DisplayName("updateBook throws DuplicateResourceException when the changed ISBN is already taken")
    void updateChangedDuplicateIsbn() {
        Book existing = Book.builder().id(5L).title("Old Title").isbn("0000000000000")
                .publicationYear(2000).price(new BigDecimal("10.00"))
                .author(Author.builder().id(1L).firstName("Robert").lastName("Martin").build())
                .publisher(Publisher.builder().id(2L).name("Prentice Hall").country("USA").build())
                .build();
        given(bookRepository.findDetailedById(5L)).willReturn(Optional.of(existing));
        given(bookRepository.existsByIsbn("9780132350884")).willReturn(true);

        assertThatThrownBy(() -> service.updateBook(5L, validRequest()))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("isbn");
    }

    @Test
    @DisplayName("searchBooks delegates to findAll(Specification, Pageable) and maps the list view (no categories)")
    void searchBooks() {
        Book book = Book.builder().id(10L).title("Clean Code").isbn("9780132350884")
                .publicationYear(2008).price(new BigDecimal("42.50"))
                .author(Author.builder().id(1L).firstName("Robert").lastName("Martin").build())
                .publisher(Publisher.builder().id(2L).name("Prentice Hall").country("USA").build())
                .build();
        Page<Book> page = new PageImpl<>(List.of(book), PageRequest.of(0, 20), 1);
        given(bookRepository.findAll(any(Specification.class), any(PageRequest.class))).willReturn(page);

        BookSearchCriteria criteria =
                new BookSearchCriteria("clean", null, null, null, null, null, null);
        PagedResponse<BookResponseDto> result = service.searchBooks(criteria, PageRequest.of(0, 20));

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.isLast()).isTrue();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Clean Code");
        assertThat(result.getContent().get(0).getAuthor().getLastName()).isEqualTo("Martin");
        // list mapping leaves categories empty (the collection is not fetched while paginating)
        assertThat(result.getContent().get(0).getCategories()).isEmpty();
    }
}
