package com.example._9_docker.service;

import org.springframework.data.domain.Pageable;

import com.example._9_docker.dto.PagedResponse;
import com.example._9_docker.dto.SupplierRequestDto;
import com.example._9_docker.dto.SupplierResponseDto;

public interface SupplierService {
    SupplierResponseDto create(SupplierRequestDto request);
    PagedResponse<SupplierResponseDto> getAll(Pageable pageable);
    SupplierResponseDto getById(Long id);
    SupplierResponseDto update(Long id, SupplierRequestDto request);
    void delete(Long id);
}
