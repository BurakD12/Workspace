package com.example._5_mapstruct.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BookRequestDto {

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "ISBN is required")
    @Pattern(regexp = "\\d{13}", message = "ISBN must be 13 digits")
    private String isbn;

    @NotNull(message = "Publication year is required")
    @Min(value = 1450, message = "Publication year must be 1450 or later")
    @Max(value = 2100, message = "Publication year must be 2100 or earlier")
    private Integer publicationYear;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    private BigDecimal price;

    @NotNull(message = "Author id is required")
    private Long authorId;

    @NotNull(message = "Publisher id is required")
    private Long publisherId;

    @Builder.Default
    private List<Long> categoryIds = new ArrayList<>();
}
