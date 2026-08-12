package com.example._6_security.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.example._6_security.dto.AuthorSummaryDto;
import com.example._6_security.dto.BookResponseDto;
import com.example._6_security.dto.CategorySummaryDto;
import com.example._6_security.dto.PublisherSummaryDto;
import com.example._6_security.model.Author;
import com.example._6_security.model.Book;
import com.example._6_security.model.Category;
import com.example._6_security.model.Publisher;

/**
 * Proves the generated MapStruct BookMapper produces output identical to the
 * hand-written
 * mapping helpers used through Labs 1-4, so the Lab 3 API contract does not
 * change. The
 * "expected" values below reproduce exactly what the old manual
 * mapToDetailResponse /
 * mapToListResponse methods returned.
 */
@DisplayName("BookMapper (MapStruct) matches the old manual mapping")
@SpringBootTest
class BookMapperTest {

    @Autowired
    private BookMapper bookMapper;

    private Book representativeBook() {
        Author author = Author.builder().id(1L)
                .firstName("Robert").lastName("Martin").bio("Uncle Bob").build();
        Publisher publisher = Publisher.builder().id(2L).name("Prentice Hall").country("USA").build();
        Category category = Category.builder().id(3L).name("Software").build();
        return Book.builder()
                .id(10L).title("Clean Code").isbn("9780132350884")
                .publicationYear(2008).price(new BigDecimal("42.50"))
                .author(author).publisher(publisher)
                .categories(Set.of(category))
                .build();
    }

    // The manual mapToDetailResponse produced this exact object.
    private BookResponseDto expectedDetail(Book book) {
        Category category = book.getCategories().iterator().next();
        return new BookResponseDto(
                book.getId(), book.getTitle(), book.getIsbn(), book.getPublicationYear(),
                book.getPrice(),
                new AuthorSummaryDto(book.getAuthor().getId(),
                        book.getAuthor().getFirstName(), book.getAuthor().getLastName()),
                new PublisherSummaryDto(book.getPublisher().getId(), book.getPublisher().getName()),
                List.of(new CategorySummaryDto(category.getId(), category.getName())));
    }

    @Test
    @DisplayName("toDetailResponse maps every scalar plus nested author/publisher/categories")
    void detailMatchesManual() {
        Book book = representativeBook();

        BookResponseDto actual = bookMapper.toDetailResponse(book);
        BookResponseDto expected = expectedDetail(book);

        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
        // Spell out the load-bearing nested pieces explicitly too.
        assertThat(actual.getAuthor().getFirstName()).isEqualTo("Robert");
        assertThat(actual.getAuthor().getLastName()).isEqualTo("Martin");
        assertThat(actual.getPublisher().getName()).isEqualTo("Prentice Hall");
        assertThat(actual.getCategories())
                .extracting(CategorySummaryDto::getName).containsExactly("Software");
    }

    @Test
    @DisplayName("toListResponse emits categories = [] (empty list, never null) like the old list view")
    void listEmitsEmptyCategories() {
        Book book = representativeBook();

        BookResponseDto actual = bookMapper.toListResponse(book);

        assertThat(actual.getId()).isEqualTo(10L);
        assertThat(actual.getTitle()).isEqualTo("Clean Code");
        assertThat(actual.getAuthor().getLastName()).isEqualTo("Martin");
        assertThat(actual.getPublisher().getName()).isEqualTo("Prentice Hall");
        // Byte-for-byte contract: the list view serialises "categories": [] not null.
        assertThat(actual.getCategories()).isNotNull().isEmpty();
    }
}
