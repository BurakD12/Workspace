package com.example._9_docker.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter 
@Setter 
@AllArgsConstructor 
@NoArgsConstructor 
public class ProductResponseDto {
    private Long id;
    private String sku;
    private String name;
    private String description;
    private BigDecimal unitPrice;
    private int quantityOnHand;
    private int reorderLevel;
    private boolean lowStock;
    private CategorySummaryDto category;
    private SupplierSummaryDto supplier;
}
