package com.example._6_security.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import com.example._6_security.dto.BookSearchCriteria;
import com.example._6_security.model.Author;
import com.example._6_security.model.Book;
import com.example._6_security.model.Category;
import com.example._6_security.model.Publisher;
import com.example._6_security.specification.BookSpecificationBuilder;

/**
 * @DataJpaTest exercising the dynamic search end to end: BookSearchCriteria -> BookSpecificationBuilder
 * -> JpaSpecificationExecutor.findAll(spec, ...) against H2. Asserts each filter alone, no filters
 * (returns all), a multi-filter AND, the many-to-many join produces no duplicate rows, and that
 * results stay paginated and sorted.
 */
@DataJpaTest
@DisplayName("Book search Specifications")
class BookSearchSpecificationTest {

    @Autowired private BookRepository bookRepository;
    @Autowired private TestEntityManager em;

    @BeforeEach
    void seed() {
        Author martin = em.persist(Author.builder().firstName("Robert").lastName("Martin").build());
        Author bloch = em.persist(Author.builder().firstName("Joshua").lastName("Bloch").build());
        Author fowler = em.persist(Author.builder().firstName("Martin").lastName("Fowler").build());
        Publisher pub = em.persist(Publisher.builder().name("Addison-Wesley").country("USA").build());
        Category software = em.persist(Category.builder().name("Software").build());
        Category architecture = em.persist(Category.builder().name("Architecture").build());

        Book cleanCode = Book.builder()
                .title("Clean Code").isbn("9780132350884").publicationYear(2008)
                .price(new BigDecimal("42.50")).author(martin).publisher(pub).build();
        cleanCode.addCategory(software);

        Book cleanArch = Book.builder()
                .title("Clean Architecture").isbn("9780134494166").publicationYear(2017)
                .price(new BigDecimal("33.00")).author(martin).publisher(pub).build();
        cleanArch.addCategory(software);
        cleanArch.addCategory(architecture);

        Book effectiveJava = Book.builder()
                .title("Effective Java").isbn("9780134685991").publicationYear(2018)
                .price(new BigDecimal("50.00")).author(bloch).publisher(pub).build();
        effectiveJava.addCategory(software);

        Book refactoring = Book.builder()
                .title("Refactoring").isbn("9780201485677").publicationYear(1999)
                .price(new BigDecimal("47.00")).author(fowler).publisher(pub).build();
        refactoring.addCategory(architecture);

        em.persist(cleanCode);
        em.persist(cleanArch);
        em.persist(effectiveJava);
        em.persist(refactoring);
        em.flush();
        em.clear();
    }

    private List<Book> search(BookSearchCriteria criteria) {
        Specification<Book> spec = BookSpecificationBuilder.from(criteria);
        return bookRepository.findAll(spec, Sort.by("title"));
    }

    private static BookSearchCriteria criteria(String title, String author, String category,
            BigDecimal minPrice, BigDecimal maxPrice, Integer minYear, Integer maxYear) {
        return new BookSearchCriteria(title, author, category, minPrice, maxPrice, minYear, maxYear);
    }

    @Test
    @DisplayName("no filters returns every book")
    void noFiltersReturnsAll() {
        List<Book> result = search(criteria(null, null, null, null, null, null, null));
        assertThat(result).extracting(Book::getTitle)
                .containsExactly("Clean Architecture", "Clean Code", "Effective Java", "Refactoring");
    }

    @Test
    @DisplayName("blank text filters are ignored (treated as absent)")
    void blankFiltersIgnored() {
        List<Book> result = search(criteria("   ", "", null, null, null, null, null));
        assertThat(result).hasSize(4);
    }

    @Test
    @DisplayName("title filter is a case-insensitive contains match")
    void titleFilter() {
        List<Book> result = search(criteria("clean", null, null, null, null, null, null));
        assertThat(result).extracting(Book::getTitle)
                .containsExactly("Clean Architecture", "Clean Code");
    }

    @Test
    @DisplayName("author filter matches the first OR last name (case-insensitive)")
    void authorFilter() {
        // "martin" matches Robert MARTIN (last name) and MARTIN Fowler (first name)
        List<Book> result = search(criteria(null, "martin", null, null, null, null, null));
        assertThat(result).extracting(Book::getTitle)
                .containsExactly("Clean Architecture", "Clean Code", "Refactoring");
    }

    @Test
    @DisplayName("category filter matches books linked to that category name")
    void categoryFilter() {
        List<Book> result = search(criteria(null, null, "Architecture", null, null, null, null));
        assertThat(result).extracting(Book::getTitle)
                .containsExactly("Clean Architecture", "Refactoring");
    }

    @Test
    @DisplayName("price range: both bounds, min-only, and max-only")
    void priceRange() {
        assertThat(search(criteria(null, null, null, new BigDecimal("40"), new BigDecimal("50"), null, null)))
                .extracting(Book::getTitle)
                .containsExactly("Clean Code", "Effective Java", "Refactoring");

        assertThat(search(criteria(null, null, null, new BigDecimal("45"), null, null, null)))
                .extracting(Book::getTitle)
                .containsExactly("Effective Java", "Refactoring");

        assertThat(search(criteria(null, null, null, null, new BigDecimal("40"), null, null)))
                .extracting(Book::getTitle)
                .containsExactly("Clean Architecture");
    }

    @Test
    @DisplayName("publication-year range filter")
    void yearRange() {
        List<Book> result = search(criteria(null, null, null, null, null, 2008, 2018));
        assertThat(result).extracting(Book::getTitle)
                .containsExactly("Clean Architecture", "Clean Code", "Effective Java");
    }

    @Test
    @DisplayName("multiple filters are AND-ed together")
    void multiFilterAnd() {
        // title contains "clean" AND price >= 40  =>  only Clean Code (42.50); Clean Architecture is 33.00
        List<Book> result = search(criteria("clean", null, null, new BigDecimal("40"), null, null, null));
        assertThat(result).extracting(Book::getTitle).containsExactly("Clean Code");
    }

    @Test
    @DisplayName("the many-to-many category join produces no duplicate rows")
    void categoryJoinHasNoDuplicates() {
        // Clean Architecture has TWO categories; filtering by one must still return it exactly once.
        List<Book> result = search(criteria(null, null, "Software", null, null, null, null));
        assertThat(result).extracting(Book::getTitle)
                .containsExactly("Clean Architecture", "Clean Code", "Effective Java");
        assertThat(result).extracting(Book::getId).doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("results stay paginated and sorted")
    void paginatedAndSorted() {
        Specification<Book> spec = BookSpecificationBuilder.from(
                criteria(null, null, null, null, null, null, null));
        Page<Book> page = bookRepository.findAll(spec, PageRequest.of(0, 2, Sort.by("title")));

        assertThat(page.getTotalElements()).isEqualTo(4);
        assertThat(page.getTotalPages()).isEqualTo(2);
        assertThat(page.isLast()).isFalse();
        assertThat(page.getContent()).extracting(Book::getTitle)
                .containsExactly("Clean Architecture", "Clean Code");
    }
}
