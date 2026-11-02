package com.example._9_docker.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.example._9_docker.dto.PagedResponse;
import com.example._9_docker.dto.ProductRequestDto;
import com.example._9_docker.dto.ProductResponseDto;
import com.example._9_docker.exception.DuplicateResourceException;
import com.example._9_docker.exception.ResourceInUseException;
import com.example._9_docker.exception.ResourceNotFoundException;
import com.example._9_docker.model.Category;
import com.example._9_docker.model.Product;
import com.example._9_docker.model.Supplier;
import com.example._9_docker.repository.CategoryRepository;
import com.example._9_docker.repository.ProductRepository;
import com.example._9_docker.repository.StockMovementRepository;
import com.example._9_docker.repository.SupplierRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductServiceImpl (business logic; authorization annotations are INERT here)")
class ProductServiceImplTest {

    @Mock private ProductRepository productRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private SupplierRepository supplierRepository;
    @Mock private StockMovementRepository stockMovementRepository;
    @InjectMocks private ProductServiceImpl service;

    private final Pageable page = PageRequest.of(0, 20);

    private Category fasteners() {
        return Category.builder().id(1L).name("Fasteners").build();
    }

    private Supplier acme() {
        return Supplier.builder().id(5L).name("Acme Metals").build();
    }

    private Product bolt() {
        Product p = Product.builder().id(10L).sku("BOLT-M8-50").name("M8 bolt").unitPrice(new BigDecimal("0.35"))
                .reorderLevel(100).category(fasteners()).supplier(acme()).build();
        p.setQuantityOnHand(250);
        return p;
    }

    private ProductRequestDto request(Long categoryId, Long supplierId) {
        return ProductRequestDto.builder().sku("BOLT-M8-50").name("M8 bolt").unitPrice(new BigDecimal("0.35"))
                .reorderLevel(100).categoryId(categoryId).supplierId(supplierId).build();
    }

    // --- create ---------------------------------------------------------------------------------

    @Test
    @DisplayName("create resolves category + supplier and starts at 0 on hand (low stock by definition)")
    void createResolvesAndStartsAtZero() {
        given(productRepository.existsBySkuIgnoreCase("BOLT-M8-50")).willReturn(false);
        given(categoryRepository.findById(1L)).willReturn(Optional.of(fasteners()));
        given(supplierRepository.findById(5L)).willReturn(Optional.of(acme()));
        given(productRepository.save(any(Product.class))).willAnswer(inv -> {
            Product p = inv.getArgument(0);
            p.setId(10L);
            return p;
        });

        ProductResponseDto res = service.create(request(1L, 5L));

        assertThat(res.getId()).isEqualTo(10L);
        assertThat(res.getQuantityOnHand()).isZero();
        assertThat(res.isLowStock()).isTrue();
        assertThat(res.getCategory().getName()).isEqualTo("Fasteners");
        assertThat(res.getSupplier().getName()).isEqualTo("Acme Metals");
        assertThat(res.getUnitPrice()).isEqualByComparingTo("0.35");
    }

    @Test
    @DisplayName("create without a supplierId leaves supplier null and never asks the supplier repository")
    void createWithoutSupplier() {
        given(productRepository.existsBySkuIgnoreCase("BOLT-M8-50")).willReturn(false);
        given(categoryRepository.findById(1L)).willReturn(Optional.of(fasteners()));
        given(productRepository.save(any(Product.class))).willAnswer(inv -> inv.getArgument(0));

        ProductResponseDto res = service.create(request(1L, null));

        assertThat(res.getSupplier()).isNull();
        verifyNoInteractions(supplierRepository);
    }

    @Test
    @DisplayName("create with an unknown category → 404, nothing saved")
    void createUnknownCategory() {
        given(productRepository.existsBySkuIgnoreCase("BOLT-M8-50")).willReturn(false);
        given(categoryRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(request(99L, null)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Category");
        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("create with an unknown supplierId → 404")
    void createUnknownSupplier() {
        given(productRepository.existsBySkuIgnoreCase("BOLT-M8-50")).willReturn(false);
        given(categoryRepository.findById(1L)).willReturn(Optional.of(fasteners()));
        given(supplierRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(request(1L, 99L)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Supplier");
    }

    @Test
    @DisplayName("create with a duplicate SKU → 409 before any lookup")
    void createDuplicateSku() {
        given(productRepository.existsBySkuIgnoreCase("BOLT-M8-50")).willReturn(true);

        assertThatThrownBy(() -> service.create(request(1L, 5L)))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("sku");
        verifyNoInteractions(categoryRepository, supplierRepository);
    }

    // --- getAll: the four filter branches -------------------------------------------------------

    @Test
    @DisplayName("getAll with no filters → findAll")
    void getAllNoFilter() {
        given(productRepository.findAll(page)).willReturn(new PageImpl<>(List.of(bolt()), page, 1));

        assertThat(service.getAll(null, false, page).getContent()).hasSize(1);
        verify(productRepository).findAll(page);
    }

    @Test
    @DisplayName("getAll with a category → findByCategoryId")
    void getAllByCategory() {
        given(productRepository.findByCategoryId(1L, page)).willReturn(new PageImpl<>(List.of(bolt()), page, 1));

        assertThat(service.getAll(1L, false, page).getContent()).hasSize(1);
        verify(productRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    @DisplayName("getAll with lowStock → findLowStock")
    void getAllLowStock() {
        given(productRepository.findLowStock(page)).willReturn(new PageImpl<>(List.of(), page, 0));

        assertThat(service.getAll(null, true, page).getContent()).isEmpty();
        verify(productRepository).findLowStock(page);
    }

    @Test
    @DisplayName("getAll with both → findLowStockByCategoryId")
    void getAllLowStockByCategory() {
        given(productRepository.findLowStockByCategoryId(1L, page)).willReturn(new PageImpl<>(List.of(), page, 0));

        service.getAll(1L, true, page);
        verify(productRepository).findLowStockByCategoryId(1L, page);
    }

    // --- getById / update / delete --------------------------------------------------------------

    @Test
    @DisplayName("getById maps lowStock from the entity rule (250 on hand, reorder 100 → not low)")
    void getById() {
        given(productRepository.findById(10L)).willReturn(Optional.of(bolt()));

        ProductResponseDto res = service.getById(10L);

        assertThat(res.getQuantityOnHand()).isEqualTo(250);
        assertThat(res.isLowStock()).isFalse();
    }

    @Test
    @DisplayName("update rewrites catalogue fields and NEVER touches quantityOnHand")
    void updateKeepsStock() {
        given(productRepository.findById(10L)).willReturn(Optional.of(bolt()));
        given(productRepository.existsBySkuIgnoreCaseAndIdNot("BOLT-M8-50", 10L)).willReturn(false);
        given(categoryRepository.findById(1L)).willReturn(Optional.of(fasteners()));
        given(productRepository.save(any(Product.class))).willAnswer(inv -> inv.getArgument(0));

        ProductRequestDto req = request(1L, null);
        req.setName("M8 x 50 bolt, zinc");
        req.setReorderLevel(50);
        ProductResponseDto res = service.update(10L, req);

        assertThat(res.getName()).isEqualTo("M8 x 50 bolt, zinc");
        assertThat(res.getReorderLevel()).isEqualTo(50);
        assertThat(res.getSupplier()).isNull();                 // supplierId omitted → cleared
        assertThat(res.getQuantityOnHand()).isEqualTo(250);     // untouched
    }

    @Test
    @DisplayName("update to a SKU another product has → 409")
    void updateDuplicateSku() {
        given(productRepository.findById(10L)).willReturn(Optional.of(bolt()));
        given(productRepository.existsBySkuIgnoreCaseAndIdNot("BOLT-M8-50", 10L)).willReturn(true);

        assertThatThrownBy(() -> service.update(10L, request(1L, null)))
                .isInstanceOf(DuplicateResourceException.class);
        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("delete removes a product with no ledger history")
    void delete() {
        given(productRepository.existsById(10L)).willReturn(true);
        given(stockMovementRepository.existsByProductId(10L)).willReturn(false);

        service.delete(10L);

        verify(productRepository).deleteById(10L);
    }

    @Test
    @DisplayName("delete of a product with ledger history → 409 (the audit trail is never cascaded away)")
    void deleteWithHistory() {
        given(productRepository.existsById(10L)).willReturn(true);
        given(stockMovementRepository.existsByProductId(10L)).willReturn(true);

        assertThatThrownBy(() -> service.delete(10L))
                .isInstanceOf(ResourceInUseException.class)
                .hasMessageContaining("stock movements");
        verify(productRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("delete 404s for an unknown id")
    void deleteUnknown() {
        given(productRepository.existsById(99L)).willReturn(false);

        assertThatThrownBy(() -> service.delete(99L)).isInstanceOf(ResourceNotFoundException.class);
    }
}
