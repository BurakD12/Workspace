package com.example._10_integration_testing.service;

import org.springframework.data.domain.Pageable;

import com.example._10_integration_testing.dto.PagedResponse;
import com.example._10_integration_testing.dto.StockMovementRequestDto;
import com.example._10_integration_testing.dto.StockMovementResponseDto;

public interface StockMovementService {

    /**
     * Append one ledger line for {@code productId}, recorded by the authenticated
     * principal, and
     * adjust the product's quantity on hand. An OUT larger than the stock on hand
     * is a 409.
     */
    StockMovementResponseDto record(Long productId, StockMovementRequestDto request, String recordedByUsername);

    /** The product's ledger, paginated (404 if the product does not exist). */
    PagedResponse<StockMovementResponseDto> getByProduct(Long productId, Pageable pageable);
}