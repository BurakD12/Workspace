package com.example._5_mapstruct.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class BookResponseDto {
    private Long id;
    private String title;
    private String isbn;
    private Integer publicationYear;
    private BigDecimal price;
    private AuthorSummaryDto author;
    private PublisherSummaryDto publisher;
    private List<CategorySummaryDto> categories;
}
