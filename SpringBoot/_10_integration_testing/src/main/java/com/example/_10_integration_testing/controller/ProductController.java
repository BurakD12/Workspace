package com.example._10_integration_testing.controller;

import java.net.URI;
import java.security.Principal;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.example._10_integration_testing.dto.PagedResponse;
import com.example._10_integration_testing.dto.ProductRequestDto;
import com.example._10_integration_testing.dto.ProductResponseDto;
import com.example._10_integration_testing.dto.StockMovementRequestDto;
import com.example._10_integration_testing.dto.StockMovementResponseDto;
import com.example._10_integration_testing.service.ProductService;
import com.example._10_integration_testing.service.StockMovementService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final StockMovementService stockMovementService;

    @PostMapping
    public ResponseEntity<ProductResponseDto> create(@Valid @RequestBody ProductRequestDto request) {
        ProductResponseDto created = productService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.getId()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    /**
     * Optional filters: {@code ?categoryId=} and {@code ?lowStock=true}; they
     * combine.
     */
    @GetMapping
    public ResponseEntity<PagedResponse<ProductResponseDto>> getAll(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "false") boolean lowStock,
            @PageableDefault(size = 20, sort = "sku") Pageable pageable) {
        return ResponseEntity.ok(productService.getAll(categoryId, lowStock, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductResponseDto> update(
            @PathVariable Long id, @Valid @RequestBody ProductRequestDto request) {
        return ResponseEntity.ok(productService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/movements")
    public ResponseEntity<StockMovementResponseDto> record(@PathVariable Long id,
            @Valid @RequestBody StockMovementRequestDto request,
            Principal principal) {
        StockMovementResponseDto recorded = stockMovementService.record(id, request, principal.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(recorded);
    }

    @GetMapping("/{id}/movements")
    public ResponseEntity<PagedResponse<StockMovementResponseDto>> getMovements(
            @PathVariable Long id,
            @PageableDefault(size = 20, sort = "occurredAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok().body(stockMovementService.getByProduct(id, pageable));
    }
}
