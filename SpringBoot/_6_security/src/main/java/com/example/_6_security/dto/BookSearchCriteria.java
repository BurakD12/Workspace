package com.example._6_security.dto;

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