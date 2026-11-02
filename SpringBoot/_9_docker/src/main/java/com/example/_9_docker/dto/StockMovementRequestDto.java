package com.example._9_docker.dto;

import com.example._9_docker.model.MovementType;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter 
@Setter 
@AllArgsConstructor 
@NoArgsConstructor
@Builder  
public class StockMovementRequestDto {
    @NotNull(message = "Type is required")
    private MovementType type;
    
    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be greater than zero")
    private Integer quantity;

    @Size(max = 255, message = "Reason must be at most 255 characters")
    private String reason;
}