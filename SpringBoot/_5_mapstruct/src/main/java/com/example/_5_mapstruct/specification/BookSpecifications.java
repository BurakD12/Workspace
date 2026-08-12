package com.example._5_mapstruct.specification;

import java.math.BigDecimal;

import org.springframework.data.jpa.domain.Specification;

import com.example._5_mapstruct.model.Author;
import com.example._5_mapstruct.model.Book;
import com.example._5_mapstruct.model.Category;

import jakarta.persistence.criteria.Join;

public final class BookSpecifications {
    public static Specification<Book> titleContains(String title) {
        String pattern = "%" + title.toLowerCase() + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get("title")), pattern);
    }

    public static Specification<Book> authorNameContains(String author) {
        String pattern = "%" + author.toLowerCase() + "%";
        return (root, query, cb) -> {
            Join<Book, Author> authorJoin = root.join("author");
            return cb.or(
                cb.like(cb.lower(authorJoin.get("firstName")), pattern),
                cb.like(cb.lower(authorJoin.get("lastName")), pattern)
            );
        };
    }

    public static Specification<Book> hasCategory(String category) {
        String pattern = category.toLowerCase();
        return (root, query, cb) -> {
            if(query != null){
                query.distinct(true);
            }
            Join<Book, Category> categoryJoin = root.join("categories");
            return cb.equal(cb.lower(categoryJoin.get("name")), pattern);
        };
    }

    public static Specification<Book> priceBetween(BigDecimal min, BigDecimal max) {
        return (root, query, cb) -> {
            if (min != null && max != null) {
                return cb.between(root.get("price"), min, max);
            }
            if (min != null) {
                return cb.greaterThanOrEqualTo(root.get("price"), min);
            } 
            return cb.lessThanOrEqualTo(root.get("price"), max);
        };
    }

    public static Specification<Book> publicationYearBetween(Integer min, Integer max) {
        return (root, query, cb) -> {
            if (min != null && max != null) {
                return cb.between(root.get("publicationYear"), min, max);
            }
            if (min != null) {
                return cb.greaterThanOrEqualTo(root.get("publicationYear"), min);
            }
            return cb.lessThanOrEqualTo(root.get("publicationYear"), max);
        };
    }
}
