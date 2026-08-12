package com.example._6_security.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import com.example._6_security.model.Author;
import com.example._6_security.model.Book;
import com.example._6_security.model.Publisher;
import com.example._6_security.projection.BookSummaryView;
import com.example._6_security.projection.BookTitleIsbn;

@DataJpaTest
@DisplayName("BookRepository projections (Lab 5)")
class BookProjectionTest {

    @Autowired private BookRepository bookRepository;
    @Autowired private TestEntityManager em;

    @BeforeEach
    void seed() {
        Author martin = em.persist(Author.builder().firstName("Robert").lastName("Martin").build());
        Publisher pub = em.persist(Publisher.builder().name("Prentice Hall").country("USA").build());
        em.persist(Book.builder()
                .title("Clean Code").isbn("9780132350884").publicationYear(2008)
                .price(new BigDecimal("42.50")).author(martin).publisher(pub).build());
        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("interface projection returns only id, title, isbn and nested author last name")
    void interfaceProjection() {
        Page<BookSummaryView> page =
                bookRepository.findAllProjectedBy(PageRequest.of(0, 10, Sort.by("title")));

        assertThat(page.getTotalElements()).isEqualTo(1);
        BookSummaryView view = page.getContent().get(0);
        assertThat(view.getId()).isNotNull();
        assertThat(view.getTitle()).isEqualTo("Clean Code");
        assertThat(view.getIsbn()).isEqualTo("9780132350884");
        assertThat(view.getAuthor().getLastName()).isEqualTo("Martin");
    }

    @Test
    @DisplayName("class-based record projection returns its three fields via a JPQL constructor expression")
    void recordProjection() {
        BookTitleIsbn dto = bookRepository.findTitleIsbnByIsbn("9780132350884").orElseThrow();

        assertThat(dto.id()).isNotNull();
        assertThat(dto.title()).isEqualTo("Clean Code");
        assertThat(dto.isbn()).isEqualTo("9780132350884");
    }

    @Test
    @DisplayName("dynamic projection returns whichever shape the caller requests")
    void dynamicProjection() {
        // full entity
        Book full = bookRepository.findByIsbn("9780132350884", Book.class).orElseThrow();
        assertThat(full.getTitle()).isEqualTo("Clean Code");

        // interface projection
        BookSummaryView view =
                bookRepository.findByIsbn("9780132350884", BookSummaryView.class).orElseThrow();
        assertThat(view.getTitle()).isEqualTo("Clean Code");
        assertThat(view.getAuthor().getLastName()).isEqualTo("Martin");

        // class-based (record) projection
        BookTitleIsbn record =
                bookRepository.findByIsbn("9780132350884", BookTitleIsbn.class).orElseThrow();
        assertThat(record.isbn()).isEqualTo("9780132350884");
    }
}
