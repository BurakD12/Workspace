package com.example._10_integration_testing.integration.support;

import java.math.BigDecimal;

import com.example._10_integration_testing.dto.CategoryRequestDto;
import com.example._10_integration_testing.dto.ProductRequestDto;
import com.example._10_integration_testing.dto.StockMovementRequestDto;
import com.example._10_integration_testing.dto.SupplierRequestDto;
import com.example._10_integration_testing.model.MovementType;

/**
 * Lab 10 — an <em>object mother</em> for the request DTOs.
 *
 * <p>The request DTOs already carry Lombok {@code @Builder}s (the <em>builder</em> half of the
 * pattern: a fluent way to set fields). What a builder does not give you is a sensible, valid
 * <em>default</em> for every field, so each test would repeat the same six lines to build "some valid
 * product". An object mother is the class that owns those defaults: every method returns a builder
 * that is already valid, and a test overrides only the one field it cares about:
 *
 * <pre>{@code
 * RequestMother.product(categoryId).build();                     // a valid product
 * RequestMother.product(categoryId).unitPrice(new BigDecimal("-1")).build(); // exactly one thing wrong
 * }</pre>
 *
 * <p>Returning the <em>builder</em> rather than the built object is the whole trick — it keeps the
 * defaults in one place while letting each test express only its deviation from "normal".
 */
public final class RequestMother {
    private RequestMother() {

    }

    public static CategoryRequestDto.CategoryRequestDtoBuilder category() {
        return CategoryRequestDto.builder()
                .name("Bearings")
                .description("Ball and roller bearings");
    }

    public static SupplierRequestDto.SupplierRequestDtoBuilder supplier() {
        return SupplierRequestDto.builder()
                .name("Acme Insustrial")
                .email("sales@acme.example")
                .phone("+90 212 000 00 00"); 
    }

    public static ProductRequestDto.ProductRequestDtoBuilder product(Long category_id) {
        return ProductRequestDto.builder()
                .sku("BRG-6204")
                .name("Deep groove ball bearing 6204")
                .description("20x47x14 mm, sealed")
                .unitPrice(new BigDecimal("4.75"))
                .reorderLevel(5)
                .categoryId(category_id);
    }

    public static StockMovementRequestDto.StockMovementRequestDtoBuilder movementIn(int quantity) {
        return StockMovementRequestDto.builder()
                .type(MovementType.IN)
                .quantity(quantity)
                .reason("Goods received");
    }
    
    public static StockMovementRequestDto.StockMovementRequestDtoBuilder movementOut(int quantity) {
        return StockMovementRequestDto.builder()
                .type(MovementType.OUT)
                .quantity(quantity)
                .reason("Sales order");
    }
}
