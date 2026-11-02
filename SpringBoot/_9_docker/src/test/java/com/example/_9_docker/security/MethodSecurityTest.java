package com.example._9_docker.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import com.example._9_docker.dto.CategoryRequestDto;
import com.example._9_docker.dto.ProductRequestDto;
import com.example._9_docker.dto.StockMovementRequestDto;
import com.example._9_docker.dto.SupplierRequestDto;
import com.example._9_docker.model.Category;
import com.example._9_docker.model.MovementType;
import com.example._9_docker.model.Product;
import com.example._9_docker.model.Role;
import com.example._9_docker.model.StockMovement;
import com.example._9_docker.model.Supplier;
import com.example._9_docker.model.User;
import com.example._9_docker.repository.CategoryRepository;
import com.example._9_docker.repository.ProductRepository;
import com.example._9_docker.repository.StockMovementRepository;
import com.example._9_docker.repository.SupplierRepository;
import com.example._9_docker.repository.UserRepository;
import com.example._9_docker.service.CategoryService;
import com.example._9_docker.service.CategoryServiceImpl;
import com.example._9_docker.service.ProductService;
import com.example._9_docker.service.ProductServiceImpl;
import com.example._9_docker.service.StockMovementService;
import com.example._9_docker.service.StockMovementServiceImpl;
import com.example._9_docker.service.SupplierService;
import com.example._9_docker.service.SupplierServiceImpl;
import com.example._9_docker.service.UserService;
import com.example._9_docker.service.UserServiceImpl;

/**
 * Lab 9 (StockFlow) — the one layer where the {@code @PreAuthorize} rules on the services actually
 * run (the Lab 8 recipe): a minimal {@code @EnableMethodSecurity} context with the REAL service
 * implementations over {@code @MockitoBean} repositories. Mockito unit tests build the services with
 * {@code new} (no proxy, annotations inert); {@code @WebMvcTest} slices mock the interfaces (rules
 * on the implementation never run). Only this context creates the proxies.
 *
 * <p>StockFlow's rules are role-based, not row-based: the catalogue is ADMIN-owned, the ledger is
 * open to any clerk, and the one argument-comparison rule pins a movement's recorder to the
 * principal.
 */
@SpringJUnitConfig(MethodSecurityTest.Config.class)
@DisplayName("Method security (role rules on the StockFlow services)")
class MethodSecurityTest {

    @Configuration
    @EnableMethodSecurity
    static class Config {

        @Bean
        CategoryService categoryService(CategoryRepository categories, ProductRepository products) {
            return new CategoryServiceImpl(categories, products);
        }

        @Bean
        SupplierService supplierService(SupplierRepository suppliers, ProductRepository products) {
            return new SupplierServiceImpl(suppliers, products);
        }

        @Bean
        ProductService productService(ProductRepository products, CategoryRepository categories,
                SupplierRepository suppliers, StockMovementRepository movements) {
            return new ProductServiceImpl(products, categories, suppliers, movements);
        }

        @Bean
        StockMovementService stockMovementService(StockMovementRepository movements, ProductRepository products,
                UserRepository users) {
            return new StockMovementServiceImpl(movements, products, users);
        }

        @Bean
        UserService userService(UserRepository users) {
            return new UserServiceImpl(users);
        }
    }

    @MockitoBean private CategoryRepository categoryRepository;
    @MockitoBean private SupplierRepository supplierRepository;
    @MockitoBean private ProductRepository productRepository;
    @MockitoBean private StockMovementRepository stockMovementRepository;
    @MockitoBean private UserRepository userRepository;

    @Autowired private CategoryService categoryService;      // the INTERFACES — these are JDK proxies
    @Autowired private SupplierService supplierService;
    @Autowired private ProductService productService;
    @Autowired private StockMovementService stockMovementService;
    @Autowired private UserService userService;

    private Category fasteners() {
        return Category.builder().id(1L).name("Fasteners").build();
    }

    private Product bolt() {
        Product p = Product.builder().id(10L).sku("BOLT-M8-50").name("M8 bolt").unitPrice(BigDecimal.ONE)
                .category(fasteners()).build();
        p.setQuantityOnHand(250);
        return p;
    }

    private User user(String username, Role role) {
        return User.builder().id(2L).username(username).email(username + "@x.io").password("h")
                .roles(Set.of(role)).build();
    }

    // =============================================================================================
    // CATALOGUE — writes are ADMIN; reads are any authenticated user
    // =============================================================================================

    @Test
    @WithMockUser(username = "bob", roles = "CLERK")
    @DisplayName("category create: denied to a CLERK, and the body never runs")
    void categoryCreateDeniedForClerk() {
        assertThatThrownBy(() -> categoryService.create(new CategoryRequestDto("Fasteners", null)))
                .isInstanceOf(AuthorizationDeniedException.class);
        verify(categoryRepository, never()).save(any());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    @DisplayName("category create: allowed for ADMIN")
    void categoryCreateAllowedForAdmin() {
        given(categoryRepository.existsByNameIgnoreCase("Fasteners")).willReturn(false);
        given(categoryRepository.save(any(Category.class))).willAnswer(inv -> inv.getArgument(0));

        assertThat(categoryService.create(new CategoryRequestDto("Fasteners", null)).getName()).isEqualTo("Fasteners");
    }

    @Test
    @WithMockUser(username = "bob", roles = "CLERK")
    @DisplayName("category update and delete: denied to a CLERK")
    void categoryWritesDeniedForClerk() {
        assertThatThrownBy(() -> categoryService.update(1L, new CategoryRequestDto("X", null)))
                .isInstanceOf(AuthorizationDeniedException.class);
        assertThatThrownBy(() -> categoryService.delete(1L))
                .isInstanceOf(AuthorizationDeniedException.class);
        verify(categoryRepository, never()).deleteById(any());
    }

    @Test
    @WithMockUser(username = "bob", roles = "CLERK")
    @DisplayName("category read: allowed for a CLERK")
    void categoryReadAllowedForClerk() {
        given(categoryRepository.findById(1L)).willReturn(Optional.of(fasteners()));

        assertThat(categoryService.getById(1L).getName()).isEqualTo("Fasteners");
    }

    @Test
    @WithMockUser(username = "bob", roles = "CLERK")
    @DisplayName("supplier create: denied to a CLERK")
    void supplierCreateDeniedForClerk() {
        assertThatThrownBy(() -> supplierService.create(new SupplierRequestDto("Acme", null, null)))
                .isInstanceOf(AuthorizationDeniedException.class);
        verify(supplierRepository, never()).save(any());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    @DisplayName("supplier create: allowed for ADMIN")
    void supplierCreateAllowedForAdmin() {
        given(supplierRepository.existsByNameIgnoreCase("Acme")).willReturn(false);
        given(supplierRepository.save(any(Supplier.class))).willAnswer(inv -> inv.getArgument(0));

        assertThat(supplierService.create(new SupplierRequestDto("Acme", null, null)).getName()).isEqualTo("Acme");
    }

    @Test
    @WithMockUser(username = "bob", roles = "CLERK")
    @DisplayName("product create / update / delete: all denied to a CLERK")
    void productWritesDeniedForClerk() {
        ProductRequestDto req = ProductRequestDto.builder().sku("X").name("x").unitPrice(BigDecimal.ONE).categoryId(1L).build();

        assertThatThrownBy(() -> productService.create(req)).isInstanceOf(AuthorizationDeniedException.class);
        assertThatThrownBy(() -> productService.update(10L, req)).isInstanceOf(AuthorizationDeniedException.class);
        assertThatThrownBy(() -> productService.delete(10L)).isInstanceOf(AuthorizationDeniedException.class);
        verify(productRepository, never()).save(any());
        verify(productRepository, never()).deleteById(any());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    @DisplayName("product create: allowed for ADMIN")
    void productCreateAllowedForAdmin() {
        given(productRepository.existsBySkuIgnoreCase("BOLT-M8-50")).willReturn(false);
        given(categoryRepository.findById(1L)).willReturn(Optional.of(fasteners()));
        given(productRepository.save(any(Product.class))).willAnswer(inv -> inv.getArgument(0));

        ProductRequestDto req = ProductRequestDto.builder().sku("BOLT-M8-50").name("M8 bolt")
                .unitPrice(BigDecimal.ONE).categoryId(1L).build();
        assertThat(productService.create(req).getSku()).isEqualTo("BOLT-M8-50");
    }

    @Test
    @WithMockUser(username = "bob", roles = "CLERK")
    @DisplayName("product read: allowed for a CLERK")
    void productReadAllowedForClerk() {
        given(productRepository.findById(10L)).willReturn(Optional.of(bolt()));

        assertThat(productService.getById(10L).getQuantityOnHand()).isEqualTo(250);
    }

    // =============================================================================================
    // LEDGER — any authenticated user may record, but only AS themselves
    // =============================================================================================

    private StockMovementRequestDto in(int quantity) {
        return StockMovementRequestDto.builder().type(MovementType.IN).quantity(quantity).build();
    }

    @Test
    @WithMockUser(username = "bob", roles = "CLERK")
    @DisplayName("record: a CLERK may record a movement as themselves")
    void recordAsSelf() {
        given(productRepository.findById(10L)).willReturn(Optional.of(bolt()));
        given(userRepository.findByUsername("bob")).willReturn(Optional.of(user("bob", Role.CLERK)));
        given(productRepository.save(any(Product.class))).willAnswer(inv -> inv.getArgument(0));
        given(stockMovementRepository.save(any(StockMovement.class))).willAnswer(inv -> inv.getArgument(0));

        assertThat(stockMovementService.record(10L, in(50), "bob").getQuantityAfter()).isEqualTo(300);
    }

    @Test
    @WithMockUser(username = "bob", roles = "CLERK")
    @DisplayName("record: denied when the recorder argument is not the principal (#recordedByUsername == authentication.name)")
    void recordAsSomeoneElseDenied() {
        assertThatThrownBy(() -> stockMovementService.record(10L, in(50), "alice"))
                .isInstanceOf(AuthorizationDeniedException.class);
        verify(productRepository, never()).findById(any());
        verify(stockMovementRepository, never()).save(any());
    }

    @Test
    @WithAnonymousUser
    @DisplayName("record: denied to an anonymous caller even if it names 'anonymousUser'")
    void recordAnonymousDenied() {
        assertThatThrownBy(() -> stockMovementService.record(10L, in(50), "anonymousUser"))
                .isInstanceOf(AuthorizationDeniedException.class);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    @DisplayName("record: an ADMIN records as themselves like anyone else")
    void recordAsAdmin() {
        given(productRepository.findById(10L)).willReturn(Optional.of(bolt()));
        given(userRepository.findByUsername("admin")).willReturn(Optional.of(user("admin", Role.ADMIN)));
        given(productRepository.save(any(Product.class))).willAnswer(inv -> inv.getArgument(0));
        given(stockMovementRepository.save(any(StockMovement.class))).willAnswer(inv -> inv.getArgument(0));

        assertThat(stockMovementService.record(10L, in(1), "admin").getRecordedBy().getUsername()).isEqualTo("admin");
    }

    // =============================================================================================
    // USERS — self or ADMIN (carried over from Lab 8)
    // =============================================================================================

    @Test
    @WithMockUser(username = "bob", roles = "CLERK")
    @DisplayName("getByUsername: allowed for your own profile")
    void ownProfile() {
        given(userRepository.findByUsername("bob")).willReturn(Optional.of(user("bob", Role.CLERK)));

        assertThat(userService.getByUsername("bob").getUsername()).isEqualTo("bob");
    }

    @Test
    @WithMockUser(username = "bob", roles = "CLERK")
    @DisplayName("getByUsername: denied for someone else's profile")
    void foreignProfileDenied() {
        assertThatThrownBy(() -> userService.getByUsername("alice"))
                .isInstanceOf(AuthorizationDeniedException.class);
        verify(userRepository, never()).findByUsername("alice");
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    @DisplayName("getByUsername: ADMIN may read anyone's profile; getAllUsers is ADMIN-only")
    void adminReadsAll() {
        given(userRepository.findByUsername("bob")).willReturn(Optional.of(user("bob", Role.CLERK)));
        given(userRepository.findAll()).willReturn(List.of(user("bob", Role.CLERK)));

        assertThat(userService.getByUsername("bob").getUsername()).isEqualTo("bob");
        assertThat(userService.getAllUsers()).hasSize(1);
    }

    @Test
    @WithMockUser(username = "bob", roles = "CLERK")
    @DisplayName("getAllUsers: denied to a CLERK")
    void listUsersDeniedForClerk() {
        assertThatThrownBy(() -> userService.getAllUsers()).isInstanceOf(AuthorizationDeniedException.class);
    }
}
