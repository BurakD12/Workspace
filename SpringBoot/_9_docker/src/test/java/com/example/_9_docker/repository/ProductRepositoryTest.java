package com.example._9_docker.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.hibernate.Hibernate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import com.example._9_docker.model.Category;
import com.example._9_docker.model.Product;
import com.example._9_docker.model.Supplier;

/**
 * Lab 9 (StockFlow) — {@code ProductRepository}: the filters that back {@code GET /api/products},
 * the uniqueness probes, and the "still referenced?" probes that make delete a 409 instead of a 500.
 *
 * <p>Fixture: two categories, one supplier, four products. Low-stock means
 * {@code quantityOnHand <= reorderLevel}, so BOLT (5 <= 100) and SAW (0 <= 0) are low, DRILL (12 > 5)
 * and NUT (500 > 100) are not.
 */
@DataJpaTest
@DisplayName("ProductRepository (filters + probes)")
class ProductRepositoryTest {

    @Autowired private ProductRepository productRepository;
    @Autowired private TestEntityManager em;

    private Long fastenersId;
    private Long toolsId;
    private Long acmeId;
    private Long boltId;

    private Product product(String sku, String name, int onHand, int reorder, Category c, Supplier s) {
        Product p = Product.builder().sku(sku).name(name).unitPrice(new BigDecimal("1.00"))
                .reorderLevel(reorder).category(c).supplier(s).build();
        p.setQuantityOnHand(onHand);
        return p;
    }

    @BeforeEach
    void seed() {
        Category fasteners = Category.builder().name("Fasteners").build();
        Category tools = Category.builder().name("Power Tools").build();
        Supplier acme = Supplier.builder().name("Acme Metals").build();
        em.persist(fasteners);
        em.persist(tools);
        em.persist(acme);

        Product bolt = product("BOLT-M8-50", "M8 bolt", 5, 100, fasteners, acme);      // low
        em.persist(bolt);
        em.persist(product("NUT-M8", "M8 nut", 500, 100, fasteners, acme));            // ok
        em.persist(product("DRILL-18V", "Cordless drill", 12, 5, tools, null));        // ok, no supplier
        em.persist(product("SAW-CIRC", "Circular saw", 0, 0, tools, null));            // low (0 <= 0)

        em.flush();
        em.clear();

        fastenersId = fasteners.getId();
        toolsId = tools.getId();
        acmeId = acme.getId();
        boltId = bolt.getId();
    }

    @Test
    @DisplayName("findAll paginates and sorts, and the entity graph brings category + supplier (supplier may be null)")
    void findAllPaginated() {
        Page<Product> page = productRepository.findAll(PageRequest.of(0, 3, Sort.by("sku")));

        assertThat(page.getTotalElements()).isEqualTo(4);
        assertThat(page.getTotalPages()).isEqualTo(2);
        assertThat(page.getContent()).extracting(Product::getSku)
                .containsExactly("BOLT-M8-50", "DRILL-18V", "NUT-M8");
        // Proving the @EntityGraph did its job: the association is a REAL, initialised entity on the
        // page, not a lazy proxy. (Reading getName() would pass either way inside a @DataJpaTest — the
        // persistence context is still open, so a proxy would initialise on access. isInitialized
        // asks BEFORE touching it.)
        Product first = page.getContent().get(0);
        assertThat(Hibernate.isInitialized(first.getCategory())).isTrue();
        assertThat(first.getCategory().getName()).isEqualTo("Fasteners");
        assertThat(page.getContent().get(1).getSupplier()).isNull();

        // Contrast: a plain em.find after em.clear() leaves the LAZY category as an uninitialised proxy.
        em.clear();
        Product plain = em.find(Product.class, boltId);
        assertThat(Hibernate.isInitialized(plain.getCategory())).isFalse();
    }

    @Test
    @DisplayName("findByCategoryId returns only that category's products")
    void byCategory() {
        Page<Product> page = productRepository.findByCategoryId(fastenersId, PageRequest.of(0, 10, Sort.by("sku")));
        assertThat(page.getContent()).extracting(Product::getSku).containsExactly("BOLT-M8-50", "NUT-M8");
    }

    @Test
    @DisplayName("findLowStock returns products with quantityOnHand <= reorderLevel, across categories")
    void lowStock() {
        Page<Product> page = productRepository.findLowStock(PageRequest.of(0, 10, Sort.by("sku")));
        assertThat(page.getContent()).extracting(Product::getSku).containsExactly("BOLT-M8-50", "SAW-CIRC");
        assertThat(page.getTotalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("findLowStockByCategoryId combines both filters")
    void lowStockByCategory() {
        Page<Product> page = productRepository.findLowStockByCategoryId(toolsId, PageRequest.of(0, 10));
        assertThat(page.getContent()).extracting(Product::getSku).containsExactly("SAW-CIRC");
    }

    @Test
    @DisplayName("existsBySkuIgnoreCase / AndIdNot: case-insensitive, and excludes the row being updated")
    void skuProbes() {
        assertThat(productRepository.existsBySkuIgnoreCase("bolt-m8-50")).isTrue();
        assertThat(productRepository.existsBySkuIgnoreCase("NOPE")).isFalse();
        assertThat(productRepository.existsBySkuIgnoreCaseAndIdNot("bolt-m8-50", boltId)).isFalse();
        assertThat(productRepository.existsBySkuIgnoreCaseAndIdNot("nut-m8", boltId)).isTrue();
    }

    @Test
    @DisplayName("existsByCategoryId / existsBySupplierId answer the 'still referenced?' question before a delete")
    void referenceProbes() {
        assertThat(productRepository.existsByCategoryId(fastenersId)).isTrue();
        assertThat(productRepository.existsBySupplierId(acmeId)).isTrue();

        Category empty = Category.builder().name("Empty").build();
        Supplier unused = Supplier.builder().name("Unused Co").build();
        em.persist(empty);
        em.persist(unused);
        em.flush();

        assertThat(productRepository.existsByCategoryId(empty.getId())).isFalse();
        assertThat(productRepository.existsBySupplierId(unused.getId())).isFalse();
    }
}
