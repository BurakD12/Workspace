package com.example._9_docker.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example._9_docker.dto.SupplierRequestDto;
import com.example._9_docker.dto.SupplierResponseDto;
import com.example._9_docker.exception.DuplicateResourceException;
import com.example._9_docker.exception.ResourceInUseException;
import com.example._9_docker.exception.ResourceNotFoundException;
import com.example._9_docker.model.Supplier;
import com.example._9_docker.repository.ProductRepository;
import com.example._9_docker.repository.SupplierRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("SupplierServiceImpl (business logic; authorization annotations are INERT here)")
class SupplierServiceImplTest {

    @Mock private SupplierRepository supplierRepository;
    @Mock private ProductRepository productRepository;
    @InjectMocks private SupplierServiceImpl service;

    private Supplier acme() {
        return Supplier.builder().id(1L).name("Acme Metals").email("sales@acme.io").phone("+1 555 0100").build();
    }

    @Test
    @DisplayName("create saves and maps all three fields")
    void create() {
        given(supplierRepository.existsByNameIgnoreCase("Acme Metals")).willReturn(false);
        given(supplierRepository.save(any(Supplier.class))).willAnswer(inv -> {
            Supplier s = inv.getArgument(0);
            s.setId(1L);
            return s;
        });

        SupplierResponseDto res = service.create(new SupplierRequestDto("Acme Metals", "sales@acme.io", "+1 555 0100"));

        assertThat(res.getId()).isEqualTo(1L);
        assertThat(res.getEmail()).isEqualTo("sales@acme.io");
        assertThat(res.getPhone()).isEqualTo("+1 555 0100");
    }

    @Test
    @DisplayName("create with a duplicate name → 409")
    void createDuplicate() {
        given(supplierRepository.existsByNameIgnoreCase("acme metals")).willReturn(true);

        assertThatThrownBy(() -> service.create(new SupplierRequestDto("acme metals", null, null)))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("supplier name");
        verify(supplierRepository, never()).save(any());
    }

    @Test
    @DisplayName("update rewrites contact details and excludes itself from the duplicate check")
    void update() {
        given(supplierRepository.findById(1L)).willReturn(Optional.of(acme()));
        given(supplierRepository.existsByNameIgnoreCaseAndIdNot("Acme Metals", 1L)).willReturn(false);
        given(supplierRepository.save(any(Supplier.class))).willAnswer(inv -> inv.getArgument(0));

        SupplierResponseDto res = service.update(1L, new SupplierRequestDto("Acme Metals", "orders@acme.io", null));

        assertThat(res.getEmail()).isEqualTo("orders@acme.io");
        assertThat(res.getPhone()).isNull();
    }

    @Test
    @DisplayName("getById 404s for an unknown id")
    void getByIdUnknown() {
        given(supplierRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(99L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("delete removes an unreferenced supplier")
    void delete() {
        given(supplierRepository.existsById(1L)).willReturn(true);
        given(productRepository.existsBySupplierId(1L)).willReturn(false);

        service.delete(1L);

        verify(supplierRepository).deleteById(1L);
    }

    @Test
    @DisplayName("delete of a supplier that products still reference → 409")
    void deleteInUse() {
        given(supplierRepository.existsById(1L)).willReturn(true);
        given(productRepository.existsBySupplierId(1L)).willReturn(true);

        assertThatThrownBy(() -> service.delete(1L)).isInstanceOf(ResourceInUseException.class);
        verify(supplierRepository, never()).deleteById(any());
    }
}
