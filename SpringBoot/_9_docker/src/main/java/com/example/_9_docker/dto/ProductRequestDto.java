package com.example._9_docker.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductRequestDto {

    @NotBlank(message = "SKU is required")
    @Size(max = 40, message = "SKU must be at most 40 characters")
    private String sku;

    @NotBlank(message = "Name is required")
    @Size(max = 150, message = "Name must be at most 150 characters")
    private String name;

    @Size(max = 1000, message = "Description must be at most 1000 characters")
    private String description;

    @NotNull(message = "Unit price is required")
    @DecimalMin(value = "0.00", message = "Unit price cannot be negative")
    @Digits(integer = 10, fraction = 2, message = "Unit price must have at most 10 integer and 2 decimal digits")
    private BigDecimal unitPrice;

    /** Optional; a missing value binds to 0 (never flag as low stock). */
    @Min(value = 0, message = "Reorder level cannot be negative")
    private int reorderLevel;

    @NotNull(message = "Category id is required")
    private Long categoryId;

    private Long supplierId;
}
