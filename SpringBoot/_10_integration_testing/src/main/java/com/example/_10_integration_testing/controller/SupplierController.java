package com.example._10_integration_testing.controller;

import java.net.URI;

import org.springframework.data.domain.Pageable;
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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.example._10_integration_testing.dto.PagedResponse;
import com.example._10_integration_testing.dto.SupplierRequestDto;
import com.example._10_integration_testing.dto.SupplierResponseDto;
import com.example._10_integration_testing.service.SupplierService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/suppliers")
public class SupplierController {
    private final SupplierService supplierService;

    @PostMapping
    public ResponseEntity<SupplierResponseDto> create(@Valid @RequestBody SupplierRequestDto request) {
        SupplierResponseDto supplier = supplierService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(supplier.getId()).toUri();

        return ResponseEntity.created(location).body(supplier);
    }

    @GetMapping
    public ResponseEntity<PagedResponse<SupplierResponseDto>> getAll(
            @PageableDefault(sort = "name") Pageable pageable) {
        return ResponseEntity.ok(supplierService.getAll(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SupplierResponseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(supplierService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SupplierResponseDto> update(@PathVariable Long id,
            @Valid @RequestBody SupplierRequestDto request) {
        return ResponseEntity.ok(supplierService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        supplierService.delete(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
