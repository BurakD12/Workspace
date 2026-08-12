package com.example._5_mapstruct.dto;
import java.math.BigDecimal;

public record BookSearchCriteria(
        String title,
        String author,
        String category,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        Integer minYear,
        Integer maxYear) {
}