package com.example._9_docker.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter 
@AllArgsConstructor 
@NoArgsConstructor 
public class ProductSummaryDto {
    private Long id;
    private String sku;
    private String name;
}
