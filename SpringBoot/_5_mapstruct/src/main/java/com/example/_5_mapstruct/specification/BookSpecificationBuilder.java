package com.example._5_mapstruct.specification;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import com.example._5_mapstruct.dto.BookSearchCriteria;
import com.example._5_mapstruct.model.Book;


public class BookSpecificationBuilder {
    private BookSpecificationBuilder() {
        // Private constructor to prevent instantiation
    }

    public static Specification<Book> from(BookSearchCriteria criteria) {
        Specification<Book> spec = Specification.unrestricted();
        
        if (StringUtils.hasText(criteria.title())) {
            spec = spec.and(BookSpecifications.titleContains(criteria.title()));            
        }
        if (StringUtils.hasText(criteria.author())) {
            spec = spec.and(BookSpecifications.authorNameContains(criteria.author()));
        }
        if (StringUtils.hasText(criteria.category())) {
            spec = spec.and(BookSpecifications.hasCategory(criteria.category()));
        }
        if (criteria.minPrice() != null || criteria.maxPrice() != null) {
            spec = spec.and(BookSpecifications.priceBetween(criteria.minPrice(), criteria.maxPrice()));
        }
        if (criteria.minYear() != null || criteria.maxYear() != null) {
            spec = spec.and(BookSpecifications.publicationYearBetween(criteria.minYear(), criteria.maxYear()));
        }

        return spec;
    }
}
