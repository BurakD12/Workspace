package com.example._6_security.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example._6_security.dto.BookRequestDto;
import com.example._6_security.dto.BookResponseDto;
import com.example._6_security.dto.BookSearchCriteria;
import com.example._6_security.dto.PagedResponse;
import com.example._6_security.exception.DuplicateResourceException;
import com.example._6_security.exception.ResourceNotFoundException;
import com.example._6_security.mapper.BookMapper;
import com.example._6_security.model.Author;
import com.example._6_security.model.Book;
import com.example._6_security.model.Category;
import com.example._6_security.model.Publisher;
import com.example._6_security.projection.BookSummaryView;
import com.example._6_security.repository.AuthorRepository;
import com.example._6_security.repository.BookRepository;
import com.example._6_security.repository.CategoryRepository;
import com.example._6_security.repository.PublisherRepository;
import com.example._6_security.specification.BookSpecificationBuilder;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookServiceImpl implements BookService {

    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;
    private final PublisherRepository publisherRepository;
    private final CategoryRepository categoryRepository;
    private final BookMapper bookMapper;

    @Override
    @Transactional
    public BookResponseDto createBook(BookRequestDto requestDto) {
        if (bookRepository.existsByIsbn(requestDto.getIsbn())) {
            throw new DuplicateResourceException("isbn", requestDto.getIsbn());
        }
        Book book = new Book();
        applyRequest(book, requestDto);
        return bookMapper.toDetailResponse(bookRepository.save(book));
    }

    @Override
    public PagedResponse<BookResponseDto> getAllBooks(Pageable pageable) {
        return toPagedResponse(bookRepository.findAll(pageable));
    }

    @Override
    public PagedResponse<BookResponseDto> getBooksByCategory(Long categoryId, Pageable pageable) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new ResourceNotFoundException("Category", categoryId);
        }
        return toPagedResponse(bookRepository.findByCategoriesId(categoryId, pageable));
    }

    @Override
    public PagedResponse<BookResponseDto> getBooksByAuthor(Long authorId, Pageable pageable) {
        if (!authorRepository.existsById(authorId)) {
            throw new ResourceNotFoundException("Author", authorId);
        }
        return toPagedResponse(bookRepository.findByAuthorId(authorId, pageable));
    }

    @Override
    public PagedResponse<BookResponseDto> getBooksByPublisher(Long publisherId, Pageable pageable) {
        if (!publisherRepository.existsById(publisherId)) {
            throw new ResourceNotFoundException("Publisher", publisherId);
        }
        return toPagedResponse(bookRepository.findByPublisherId(publisherId, pageable));
    }

    @Override
    public BookResponseDto getBookById(Long id) {
        Book book = bookRepository.findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book", id));
        return bookMapper.toDetailResponse(book);
    }

    @Override
    @Transactional
    public BookResponseDto updateBook(Long id, BookRequestDto requestDto) {
        Book book = bookRepository.findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book", id));
        if (!book.getIsbn().equals(requestDto.getIsbn())
                && bookRepository.existsByIsbn(requestDto.getIsbn())) {
            throw new DuplicateResourceException("isbn", requestDto.getIsbn());
        }
        applyRequest(book, requestDto);
        return bookMapper.toDetailResponse(bookRepository.save(book));
    }

    @Override
    @Transactional
    public void deleteBook(Long id) {
        if (!bookRepository.existsById(id)) {
            throw new ResourceNotFoundException("Book", id);
        }
        bookRepository.deleteById(id);
    }

    // Resolves related ids to entities and copies scalar fields onto the book.
    private void applyRequest(Book book, BookRequestDto dto) {
        Author author = authorRepository.findById(dto.getAuthorId())
                .orElseThrow(() -> new ResourceNotFoundException("Author", dto.getAuthorId()));
        Publisher publisher = publisherRepository.findById(dto.getPublisherId())
                .orElseThrow(() -> new ResourceNotFoundException("Publisher", dto.getPublisherId()));

        book.setTitle(dto.getTitle());
        book.setIsbn(dto.getIsbn());
        book.setPublicationYear(dto.getPublicationYear());
        book.setPrice(dto.getPrice());
        book.setAuthor(author);
        book.setPublisher(publisher);
        book.setCategories(resolveCategories(dto.getCategoryIds()));
    }

    private Set<Category> resolveCategories(List<Long> categoryIds) {
        Set<Category> categories = new HashSet<>();
        if (categoryIds == null) {
            return categories;
        }
        for (Long categoryId : categoryIds) {
            categories.add(categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new ResourceNotFoundException("Category", categoryId)));
        }
        return categories;
    }

    @Override
    public PagedResponse<BookResponseDto> searchBooks(BookSearchCriteria criteria, Pageable pageable) {
        Specification<Book> spec = BookSpecificationBuilder.from(criteria);
        return toPagedResponse(bookRepository.findAll(spec, pageable));

    }

    private PagedResponse<BookResponseDto> toPagedResponse(Page<Book> page) {
        List<BookResponseDto> bookResponseDtos = page.getContent().stream().map(bookMapper::toListResponse).toList();
        return PagedResponse.from(page, bookResponseDtos);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<BookSummaryView> getBookSummaries(Pageable pageable) {
        Page<BookSummaryView> page = bookRepository.findAllProjectedBy(pageable);
        return PagedResponse.from(page, page.getContent());
    }
}
