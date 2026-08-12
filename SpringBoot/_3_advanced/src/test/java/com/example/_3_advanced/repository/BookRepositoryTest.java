package com.example._3_advanced.repository;

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

import com.example._3_advanced.model.Author;
import com.example._3_advanced.model.Book;
import com.example._3_advanced.model.Category;
import com.example._3_advanced.model.Publisher;

import jakarta.persistence.PersistenceUnitUtil;

@DataJpaTest
@DisplayName("BookRepository")
class BookRepositoryTest {

    @Autowired private BookRepository bookRepository;
    @Autowired private TestEntityManager em;

    private Author martin;
    private Publisher publisher;

    @BeforeEach
    void seed() {
        martin = em.persist(Author.builder().firstName("Robert").lastName("Martin").build());
        Author bloch = em.persist(Author.builder().firstName("Joshua").lastName("Bloch").build());
        publisher = em.persist(Publisher.builder().name("Addison-Wesley").country("USA").build());
        Category sw = em.persist(Category.builder().name("Software").build());

        Book cleanCode = Book.builder()
                .title("Clean Code").isbn("9780132350884").publicationYear(2008)
                .price(new BigDecimal("42.50")).author(martin).publisher(publisher).build();
        cleanCode.addCategory(sw);

        Book effectiveJava = Book.builder()
                .title("Effective Java").isbn("9780134685991").publicationYear(2018)
                .price(new BigDecimal("50.00")).author(bloch).publisher(publisher).build();
        effectiveJava.addCategory(sw);

        em.persist(cleanCode);
        em.persist(effectiveJava);
        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("existsByIsbn is true for a known ISBN and false otherwise")
    void existsByIsbn() {
        assertThat(bookRepository.existsByIsbn("9780132350884")).isTrue();
        assertThat(bookRepository.existsByIsbn("0000000000000")).isFalse();
    }

    @Test
    @DisplayName("findByAuthorId returns only that author's books, paginated")
    void findByAuthorId() {
        Page<Book> page = bookRepository.findByAuthorId(martin.getId(),
                PageRequest.of(0, 10, Sort.by("title")));
        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getTitle()).isEqualTo("Clean Code");
    }

    @Test
    @DisplayName("findDetailedById initialises the categories collection")
    void findDetailedByIdInitialisesCategories() {
        Book any = bookRepository.findAll().get(0);
        Book detailed = bookRepository.findDetailedById(any.getId()).orElseThrow();

        PersistenceUnitUtil util = em.getEntityManager().getEntityManagerFactory()
                .getPersistenceUnitUtil();
        assertThat(util.isLoaded(detailed, "categories")).isTrue();
        assertThat(detailed.getCategories()).extracting(Category::getName).contains("Software");
    }

    @Test
    @DisplayName("findAllWithAuthorAndPublisher initialises author and publisher (the JOIN FETCH N+1 fix)")
    void joinFetchInitialisesAuthorAndPublisher() {
        var books = bookRepository.findAllWithAuthorAndPublisher();
        PersistenceUnitUtil util = em.getEntityManager().getEntityManagerFactory()
                .getPersistenceUnitUtil();
        assertThat(books).isNotEmpty();
        assertThat(util.isLoaded(books.get(0), "author")).isTrue();
        assertThat(util.isLoaded(books.get(0), "publisher")).isTrue();
    }

    @Test
    @DisplayName("existsByAuthorId / existsByPublisherId support protective deletes")
    void existsByRelations() {
        assertThat(bookRepository.existsByAuthorId(martin.getId())).isTrue();
        assertThat(bookRepository.existsByPublisherId(publisher.getId())).isTrue();
    }
}
