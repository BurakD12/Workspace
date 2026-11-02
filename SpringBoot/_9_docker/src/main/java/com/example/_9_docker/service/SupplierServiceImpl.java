package com.example._9_docker.service;

import java.util.DuplicateFormatFlagsException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example._9_docker.dto.PagedResponse;
import com.example._9_docker.dto.SupplierRequestDto;
import com.example._9_docker.dto.SupplierResponseDto;
import com.example._9_docker.exception.DuplicateResourceException;
import com.example._9_docker.exception.ResourceInUseException;
import com.example._9_docker.exception.ResourceNotFoundException;
import com.example._9_docker.model.Supplier;
import com.example._9_docker.repository.ProductRepository;
import com.example._9_docker.repository.SupplierRepository;

import lombok.RequiredArgsConstructor;

@Service 
@RequiredArgsConstructor 
@Transactional(readOnly = true)
public class SupplierServiceImpl implements SupplierService{
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public SupplierResponseDto create(SupplierRequestDto request) {
        if (supplierRepository.existsByNameIgnoreCase(request.getName())) {
            throw new DuplicateResourceException("supplier name", request.getName());
        }
        return toResponse(supplierRepository.save(toEntity(request)));
    }

    @Override
    public PagedResponse<SupplierResponseDto> getAll(Pageable pageable) {
        Page<Supplier> page = supplierRepository.findAll(pageable);
        return PagedResponse.from(page, page.getContent().stream().map(this:: toResponse).toList());
    }

    @Override
    public SupplierResponseDto getById(Long id) {
        return toResponse(supplierRepository.findById(id).orElseThrow(
                () -> new ResourceNotFoundException("Supplier", id)));
    }

    @Override
    @Transactional 
    @PreAuthorize("hasRole('ADMIN')")
    public SupplierResponseDto update(Long id, SupplierRequestDto request) {
        Supplier supplier = supplierRepository.findById(id).orElseThrow(
                    () -> new ResourceNotFoundException("Supplier", id));
        if (supplierRepository.existsByNameIgnoreCaseAndIdNot(request.getName(), id)) {
            throw new DuplicateResourceException("supplier name", request.getName());
        }
        supplier.setName(request.getName());
        supplier.setEmail(request.getEmail());
        supplier.setPhone(request.getPhone());
        return toResponse(supplierRepository.save(supplier));
    }

    @Override
    @Transactional 
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(Long id) {
        if (!supplierRepository.existsById(id)) {
            throw new ResourceNotFoundException("Supplier", id);
        }    
        if (productRepository.existsBySupplierId(id)) {
            throw new ResourceInUseException("Supplier", id, "products");
        }
        supplierRepository.deleteById(id);
    }
    
    private Supplier toEntity(SupplierRequestDto request) {
        return Supplier.builder()
        .name(request.getName())
        .email(request.getEmail())
        .phone(request.getPhone())
        .build();
    }

    private SupplierResponseDto toResponse(Supplier supplier) {
        return new SupplierResponseDto(supplier.getId()
            , supplier.getName()
            , supplier.getEmail()
            , supplier.getPhone());
    }
}
