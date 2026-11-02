package com.example._9_docker.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example._9_docker.dto.PagedResponse;
import com.example._9_docker.dto.ProductSummaryDto;
import com.example._9_docker.dto.StockMovementRequestDto;
import com.example._9_docker.dto.StockMovementResponseDto;
import com.example._9_docker.dto.UserSummaryDto;
import com.example._9_docker.exception.InsufficientStockException;
import com.example._9_docker.exception.ResourceNotFoundException;
import com.example._9_docker.model.MovementType;
import com.example._9_docker.model.Product;
import com.example._9_docker.model.StockMovement;
import com.example._9_docker.model.User;
import com.example._9_docker.repository.ProductRepository;
import com.example._9_docker.repository.StockMovementRepository;
import com.example._9_docker.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@Transactional 
@RequiredArgsConstructor 
public class StockMovementServiceImpl implements StockMovementService{
    private final StockMovementRepository stockMovementRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional 
    @PreAuthorize ("isAuthenticated() and #recordedByUsername == authentication.name")
    public StockMovementResponseDto record(Long productId, StockMovementRequestDto request, String recordedByUsername) {
        Product p = productRepository.findById(productId).orElseThrow(() -> new 
                ResourceNotFoundException("Product", productId));
        User u = userRepository.findByUsername(recordedByUsername).orElseThrow(() -> new 
                ResourceNotFoundException("User", recordedByUsername));

        int quantity = request.getQuantity();
        int after;
        if (request.getType() == MovementType.IN) {
            after = p.getQuantityOnHand() + quantity;
        } else {
            if (p.getQuantityOnHand() < quantity) {
                throw new InsufficientStockException(p.getSku(), p.getQuantityOnHand(), quantity);
            }
            after = p.getQuantityOnHand() - quantity;
        }

        p.setQuantityOnHand(after);
        productRepository.save(p);

        StockMovement m = StockMovement.builder()
                .product(p)
                .type(request.getType())
                .quantity(quantity)
                .reason(request.getReason())
                .quantityAfter(after)
                .recordedBy(u)
                .build();

        return toResponse(stockMovementRepository.save(m));
    }

    @Override
    public PagedResponse<StockMovementResponseDto> getByProduct(Long productId, Pageable pageable) {
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product", productId);
        }
        Page<StockMovement> page = stockMovementRepository.findByProductId(productId, pageable);

        return PagedResponse.from(page, page.getContent().stream().map(this::toResponse).toList());

    }
    
    private StockMovementResponseDto toResponse(StockMovement stockMovement) {
        Product p = stockMovement.getProduct();
        ProductSummaryDto productSummaryDto = p == null ? null : new ProductSummaryDto(p.getId(), p.getSku(), p.getName());
        User u = stockMovement.getRecordedBy();
        UserSummaryDto userSummaryDto = u == null ? null : new UserSummaryDto(u.getId(), u.getUsername());
        
        return new StockMovementResponseDto(stockMovement.getId(),
                stockMovement.getType(), 
                stockMovement.getQuantity(), 
                stockMovement.getReason(), 
                stockMovement.getQuantityAfter(), 
                stockMovement.getOccurredAt(),
                productSummaryDto, 
                userSummaryDto);
    }
}
