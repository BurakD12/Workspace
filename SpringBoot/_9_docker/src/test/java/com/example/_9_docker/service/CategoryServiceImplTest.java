package com.example._9_docker.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

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

import com.example._9_docker.dto.CategoryRequestDto;
import com.example._9_docker.dto.CategoryResponseDto;
import com.example._9_docker.dto.PagedResponse;
import com.example._9_docker.exception.DuplicateResourceException;
import com.example._9_docker.exception.ResourceInUseException;
import com.example._9_docker.exception.ResourceNotFoundException;
import com.example._9_docker.model.Category;
import com.example._9_docker.repository.CategoryRepository;
import com.example._9_docker.repository.ProductRepository;

/**
 * Lab 9 (StockFlow) — business logic only. The service is built with {@code new} by
 * {@code @InjectMocks}, so the {@code @PreAuthorize} annotations are INERT here; the role rules are
 * proven in {@code MethodSecurityTest}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CategoryServiceImpl (business logic; authorization annotations are INERT here)")
class CategoryServiceImplTest {

    @Mock private CategoryRepository categoryRepository;
    @Mock private ProductRepository productRepository;
    @InjectMocks private CategoryServiceImpl service;

    private Category fasteners() {
        return Category.builder().id(1L).name("Fasteners").description("Bolts and nuts").build();
    }

    @Test
    @DisplayName("create saves and maps")
    void create() {
        given(categoryRepository.existsByNameIgnoreCase("Fasteners")).willReturn(false);
        given(categoryRepository.save(any(Category.class))).willAnswer(inv -> {
            Category c = inv.getArgument(0);
            c.setId(1L);
            return c;
        });

        CategoryResponseDto res = service.create(new CategoryRequestDto("Fasteners", "Bolts and nuts"));

        assertThat(res.getId()).isEqualTo(1L);
        assertThat(res.getName()).isEqualTo("Fasteners");
        assertThat(res.getDescription()).isEqualTo("Bolts and nuts");
    }

    @Test
    @DisplayName("create with a duplicate name (any case) → 409 and nothing saved")
    void createDuplicate() {
        given(categoryRepository.existsByNameIgnoreCase("fasteners")).willReturn(true);

        assertThatThrownBy(() -> service.create(new CategoryRequestDto("fasteners", null)))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("category name");
        verify(categoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("getAll maps a page into a PagedResponse")
    void getAll() {
        given(categoryRepository.findAll(any(PageRequest.class)))
                .willReturn(new PageImpl<>(List.of(fasteners()), PageRequest.of(0, 20), 1));

        PagedResponse<CategoryResponseDto> res = service.getAll(PageRequest.of(0, 20));

        assertThat(res.getTotalElements()).isEqualTo(1);
        assertThat(res.getContent().get(0).getName()).isEqualTo("Fasteners");
    }

    @Test
    @DisplayName("getById 404s for an unknown id")
    void getByIdUnknown() {
        given(categoryRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Category");
    }

    @Test
    @DisplayName("update changes name and description")
    void update() {
        given(categoryRepository.findById(1L)).willReturn(Optional.of(fasteners()));
        given(categoryRepository.existsByNameIgnoreCaseAndIdNot("Fixings", 1L)).willReturn(false);
        given(categoryRepository.save(any(Category.class))).willAnswer(inv -> inv.getArgument(0));

        CategoryResponseDto res = service.update(1L, new CategoryRequestDto("Fixings", "Renamed"));

        assertThat(res.getName()).isEqualTo("Fixings");
        assertThat(res.getDescription()).isEqualTo("Renamed");
    }

    @Test
    @DisplayName("update to a name another category already has → 409")
    void updateDuplicate() {
        given(categoryRepository.findById(1L)).willReturn(Optional.of(fasteners()));
        given(categoryRepository.existsByNameIgnoreCaseAndIdNot("Power Tools", 1L)).willReturn(true);

        assertThatThrownBy(() -> service.update(1L, new CategoryRequestDto("Power Tools", null)))
                .isInstanceOf(DuplicateResourceException.class);
        verify(categoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("delete removes an unreferenced category")
    void delete() {
        given(categoryRepository.existsById(1L)).willReturn(true);
        given(productRepository.existsByCategoryId(1L)).willReturn(false);

        service.delete(1L);

        verify(categoryRepository).deleteById(1L);
    }

    @Test
    @DisplayName("delete 404s for an unknown id")
    void deleteUnknown() {
        given(categoryRepository.existsById(99L)).willReturn(false);

        assertThatThrownBy(() -> service.delete(99L)).isInstanceOf(ResourceNotFoundException.class);
        verify(categoryRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("delete of a category that still has products → 409, decided BEFORE touching the row")
    void deleteInUse() {
        given(categoryRepository.existsById(1L)).willReturn(true);
        given(productRepository.existsByCategoryId(1L)).willReturn(true);

        assertThatThrownBy(() -> service.delete(1L))
                .isInstanceOf(ResourceInUseException.class)
                .hasMessageContaining("products");
        verify(categoryRepository, never()).deleteById(any());
    }
}
