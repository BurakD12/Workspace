package com.example._5_mapstruct.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example._5_mapstruct.model.Book;
import com.example._5_mapstruct.projection.BookSummaryView;
import com.example._5_mapstruct.projection.BookTitleIsbn;

public interface BookRepository extends JpaRepository<Book, Long>, JpaSpecificationExecutor<Book> {

    boolean existsByIsbn(String isbn);

    boolean existsByAuthorId(Long authorId);

    boolean existsByPublisherId(Long publisherId);

    // Paginated list: fetch the @ManyToOne sides via an entity graph to avoid N+1.
    // We deliberately do NOT fetch the categories collection here — fetching a
    // collection together with pagination makes Hibernate paginate in memory.
    @Override
    @EntityGraph(attributePaths = {"author", "publisher"})
    Page<Book> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"author", "publisher"})
    Page<Book> findByAuthorId(Long authorId, Pageable pageable);

    @EntityGraph(attributePaths = {"author", "publisher"})
    Page<Book> findByPublisherId(Long publisherId, Pageable pageable);

    @EntityGraph(attributePaths = {"author", "publisher"})
    Page<Book> findByCategoriesId(Long categoryId, Pageable pageable);

    // Single book with everything initialised, including the categories collection.
    @EntityGraph(attributePaths = {"author", "publisher", "categories"})
    @Query("select b from Book b where b.id = :id")
    Optional<Book> findDetailedById(@Param("id") Long id);

    // Teaching method: the classic JOIN FETCH fix for the N+1 problem.
    @Query("select b from Book b join fetch b.author join fetch b.publisher")
    List<Book> findAllWithAuthorAndPublisher();

    @Query("select new com.example._5_mapstruct.projection.BookTitleIsbn(b.id, b.title, b.isbn) from Book b where b.isbn = :isbn")
    Optional<BookTitleIsbn> findTitleIsbnByIsbn(@Param("isbn") String isbn);

    <T> Optional<T> findByIsbn(String isbn, Class<T> type);

    Page<BookSummaryView> findAllProjectedBy(Pageable pageable);
}
