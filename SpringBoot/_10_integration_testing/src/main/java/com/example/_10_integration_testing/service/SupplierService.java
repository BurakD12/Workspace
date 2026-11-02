package com.example._10_integration_testing.service;

import org.springframework.data.domain.Pageable;

import com.example._10_integration_testing.dto.PagedResponse;
import com.example._10_integration_testing.dto.SupplierRequestDto;
import com.example._10_integration_testing.dto.SupplierResponseDto;

public interface SupplierService {
    SupplierResponseDto create(SupplierRequestDto request);

    PagedResponse<SupplierResponseDto> getAll(Pageable pageable);

    SupplierResponseDto getById(Long id);

    SupplierResponseDto update(Long id, SupplierRequestDto request);

    void delete(Long id);
}
