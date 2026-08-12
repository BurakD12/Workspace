package com.example._3_advanced.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example._3_advanced.dto.CategoryRequestDto;
import com.example._3_advanced.dto.CategoryResponseDto;
import com.example._3_advanced.exception.DuplicateResourceException;
import com.example._3_advanced.exception.ResourceNotFoundException;
import com.example._3_advanced.model.Book;
import com.example._3_advanced.model.Category;
import com.example._3_advanced.repository.CategoryRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("CategoryServiceImpl")
class CategoryServiceImplTest {

    @Mock private CategoryRepository categoryRepository;
    @InjectMocks private CategoryServiceImpl service;

    @Test
    @DisplayName("createCategory saves and returns the mapped response")
    void createCategory() {
        given(categoryRepository.existsByNameIgnoreCase("Software")).willReturn(false);
        given(categoryRepository.save(any(Category.class))).willAnswer(inv -> {
            Category c = inv.getArgument(0);
            c.setId(1L);
            return c;
        });

        CategoryResponseDto res = service.createCategory(new CategoryRequestDto("Software"));

        assertThat(res.getId()).isEqualTo(1L);
        assertThat(res.getName()).isEqualTo("Software");
    }

    @Test
    @DisplayName("createCategory throws DuplicateResourceException for duplicate name")
    void createCategoryDuplicate() {
        given(categoryRepository.existsByNameIgnoreCase("Software")).willReturn(true);

        assertThatThrownBy(() -> service.createCategory(new CategoryRequestDto("Software")))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("name");
    }

    @Test
    @DisplayName("getCategoryById throws ResourceNotFoundException when missing")
    void getCategoryByIdMissing() {
        given(categoryRepository.findById(9999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.getCategoryById(9999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("9999");
    }

    @Test
    @DisplayName("deleteCategory deletes and verifies repository call")
    void deleteCategorySuccess() {
        Category category = Category.builder().id(1L).name("Software").build();
        given(categoryRepository.findById(1L)).willReturn(Optional.of(category));

        service.deleteCategory(1L);

        verify(categoryRepository).delete(category);
    }

    @Test
    @DisplayName("deleteCategory detaches the category from each book (owning side) before deleting")
    void deleteCategoryDetachesBooks() {
        Category category = Category.builder().id(1L).name("Software").build();
        Book book = Book.builder().id(2L).title("Clean Code").isbn("9780132350884")
                .publicationYear(2008).price(new BigDecimal("42.50")).build();
        // wire both sides as they would be in a loaded graph
        book.getCategories().add(category);
        category.getBooks().add(book);
        given(categoryRepository.findById(1L)).willReturn(Optional.of(category));

        service.deleteCategory(1L);

        assertThat(book.getCategories()).doesNotContain(category);
        verify(categoryRepository).delete(category);
    }
}
