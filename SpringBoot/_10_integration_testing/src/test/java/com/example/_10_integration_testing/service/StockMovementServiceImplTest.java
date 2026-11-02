package com.example._10_integration_testing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.example._10_integration_testing.dto.PagedResponse;
import com.example._10_integration_testing.dto.StockMovementRequestDto;
import com.example._10_integration_testing.dto.StockMovementResponseDto;
import com.example._10_integration_testing.exception.InsufficientStockException;
import com.example._10_integration_testing.exception.ResourceNotFoundException;
import com.example._10_integration_testing.model.Category;
import com.example._10_integration_testing.model.MovementType;
import com.example._10_integration_testing.model.Product;
import com.example._10_integration_testing.model.Role;
import com.example._10_integration_testing.model.StockMovement;
import com.example._10_integration_testing.model.User;
import com.example._10_integration_testing.repository.ProductRepository;
import com.example._10_integration_testing.repository.StockMovementRepository;
import com.example._10_integration_testing.repository.UserRepository;

/**
 * Lab 9 (StockFlow) — the stock arithmetic and the ledger write, in isolation.
 * The
 * {@code @PreAuthorize} on {@code record} is inert here (no proxy);
 * {@code MethodSecurityTest} proves
 * it.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("StockMovementServiceImpl (stock arithmetic; authorization annotation is INERT here)")
class StockMovementServiceImplTest {

    @Mock
    private StockMovementRepository stockMovementRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private UserRepository userRepository;
    @InjectMocks
    private StockMovementServiceImpl service;

    private User bob() {
        return User.builder().id(2L).username("bob").email("b@x.io").password("h").roles(Set.of(Role.CLERK)).build();
    }

    private Product boltWith(int onHand) {
        Product p = Product.builder().id(10L).sku("BOLT-M8-50").name("M8 bolt").unitPrice(BigDecimal.ONE)
                .reorderLevel(100).category(Category.builder().id(1L).name("Fasteners").build()).build();
        p.setQuantityOnHand(onHand);
        return p;
    }

    private StockMovementRequestDto movement(MovementType type, int quantity) {
        return StockMovementRequestDto.builder().type(type).quantity(quantity).reason("test").build();
    }

    private void stubHappyPath(int onHand) {
        given(productRepository.findById(10L)).willReturn(Optional.of(boltWith(onHand)));
        given(userRepository.findByUsername("bob")).willReturn(Optional.of(bob()));
        given(productRepository.save(any(Product.class))).willAnswer(inv -> inv.getArgument(0));
        given(stockMovementRepository.save(any(StockMovement.class))).willAnswer(inv -> {
            StockMovement m = inv.getArgument(0);
            m.setId(500L);
            m.setOccurredAt(Instant.parse("2026-09-04T12:00:00Z"));
            return m;
        });
    }

    @Test
    @DisplayName("IN adds to the product and the ledger line carries the new balance and the recorder")
    void inAdds() {
        stubHappyPath(250);

        StockMovementResponseDto res = service.record(10L, movement(MovementType.IN, 50), "bob");

        assertThat(res.getType()).isEqualTo(MovementType.IN);
        assertThat(res.getQuantity()).isEqualTo(50);
        assertThat(res.getQuantityAfter()).isEqualTo(300);
        assertThat(res.getRecordedBy().getUsername()).isEqualTo("bob");
        assertThat(res.getProduct().getSku()).isEqualTo("BOLT-M8-50");

        ArgumentCaptor<Product> saved = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(saved.capture());
        assertThat(saved.getValue().getQuantityOnHand()).isEqualTo(300);
    }

    @Test
    @DisplayName("OUT subtracts")
    void outSubtracts() {
        stubHappyPath(250);

        StockMovementResponseDto res = service.record(10L, movement(MovementType.OUT, 30), "bob");

        assertThat(res.getQuantityAfter()).isEqualTo(220);
    }

    @Test
    @DisplayName("OUT of exactly the quantity on hand is allowed and leaves zero")
    void outToZero() {
        stubHappyPath(250);

        assertThat(service.record(10L, movement(MovementType.OUT, 250), "bob").getQuantityAfter()).isZero();
    }

    @Test
    @DisplayName("OUT larger than on hand → 409, and NEITHER the product NOR a ledger line is written")
    void outTooLarge() {
        given(productRepository.findById(10L)).willReturn(Optional.of(boltWith(250)));
        given(userRepository.findByUsername("bob")).willReturn(Optional.of(bob()));

        assertThatThrownBy(() -> service.record(10L, movement(MovementType.OUT, 300), "bob"))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("250 on hand")
                .hasMessageContaining("300 requested");

        verify(productRepository, never()).save(any());
        verify(stockMovementRepository, never()).save(any());
    }

    @Test
    @DisplayName("record against an unknown product → 404")
    void unknownProduct() {
        given(productRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.record(99L, movement(MovementType.IN, 1), "bob"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Product");
    }

    @Test
    @DisplayName("record by a username with no User row → 404")
    void unknownRecorder() {
        given(productRepository.findById(10L)).willReturn(Optional.of(boltWith(250)));
        given(userRepository.findByUsername("ghost")).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.record(10L, movement(MovementType.IN, 1), "ghost"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User");
    }

    @Test
    @DisplayName("getByProduct maps the ledger page")
    void ledger() {
        Pageable pageable = PageRequest.of(0, 20);
        StockMovement line = StockMovement.builder().id(500L).product(boltWith(250)).type(MovementType.IN)
                .quantity(250).quantityAfter(250).recordedBy(bob()).occurredAt(Instant.now()).build();
        given(productRepository.existsById(10L)).willReturn(true);
        given(stockMovementRepository.findByProductId(10L, pageable))
                .willReturn(new PageImpl<>(List.of(line), pageable, 1));

        PagedResponse<StockMovementResponseDto> res = service.getByProduct(10L, pageable);

        assertThat(res.getTotalElements()).isEqualTo(1);
        assertThat(res.getContent().get(0).getQuantityAfter()).isEqualTo(250);
    }

    @Test
    @DisplayName("getByProduct 404s for an unknown product")
    void ledgerUnknownProduct() {
        given(productRepository.existsById(99L)).willReturn(false);

        assertThatThrownBy(() -> service.getByProduct(99L, PageRequest.of(0, 20)))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
