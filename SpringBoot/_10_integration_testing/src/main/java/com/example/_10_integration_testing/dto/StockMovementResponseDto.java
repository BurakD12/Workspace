package com.example._10_integration_testing.dto;

import java.time.Instant;

import com.example._10_integration_testing.model.MovementType;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class StockMovementResponseDto {
    private Long id;
    private MovementType type;
    private int quantity;
    private String reason;
    private int quantityAfter;
    private Instant occurredAt;
    private ProductSummaryDto product;
    private UserSummaryDto recordedBy;
}
