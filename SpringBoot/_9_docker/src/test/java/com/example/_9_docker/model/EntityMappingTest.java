package com.example._9_docker.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

/**
 * Lab 9 (StockFlow) — proves the entity mappings persist and reload every relationship, and that
 * the entity defaults behave. The first test any new domain should get: if this is red, nothing
 * above the repository layer can be trusted.
 */
@DataJpaTest
@DisplayName("Entity relationship mapping")
class EntityMappingTest {

    @Autowired
    private TestEntityManager em;

    private User clerk(String username) {
        return User.builder().username(username).email(username + "@stockflow.io").password("bcrypt-hash")
                .roles(Set.of(Role.CLERK)).build();
    }

    @Test
    @DisplayName("persists a Product with its Category and Supplier and a StockMovement with its recorder, and reloads them all")
    void persistsAndReloadsRelationships() {
        Category fasteners = Category.builder().name("Fasteners").description("Bolts, nuts, screws").build();
        Supplier acme = Supplier.builder().name("Acme Metals").email("sales@acme.io").phone("+1 555 0100").build();
        User bob = clerk("bob");
        em.persist(fasteners);
        em.persist(acme);
        em.persist(bob);

        Product bolt = Product.builder()
                .sku("BOLT-M8-50").name("M8 x 50 bolt").description("Zinc plated")
                .unitPrice(new BigDecimal("0.35")).reorderLevel(100)
                .category(fasteners).supplier(acme)
                .build();
        em.persist(bolt);

        StockMovement delivery = StockMovement.builder()
                .product(bolt).type(MovementType.IN).quantity(250).reason("Initial delivery")
                .quantityAfter(250).recordedBy(bob)
                .build();
        StockMovement saved = em.persistFlushFind(delivery);
        em.clear(); // detach everything so the reload really hits the database

        StockMovement reloaded = em.find(StockMovement.class, saved.getId());
        assertThat(reloaded.getType()).isEqualTo(MovementType.IN);
        assertThat(reloaded.getQuantity()).isEqualTo(250);
        assertThat(reloaded.getQuantityAfter()).isEqualTo(250);
        assertThat(reloaded.getOccurredAt()).isNotNull();           // stamped by @PrePersist
        assertThat(reloaded.getRecordedBy().getUsername()).isEqualTo("bob");
        assertThat(reloaded.getProduct().getSku()).isEqualTo("BOLT-M8-50");
        assertThat(reloaded.getProduct().getUnitPrice()).isEqualByComparingTo("0.35");
        assertThat(reloaded.getProduct().getCategory().getName()).isEqualTo("Fasteners");
        assertThat(reloaded.getProduct().getSupplier().getName()).isEqualTo("Acme Metals");
    }

    @Test
    @DisplayName("a Product built with only the mandatory fields defaults to 0 on hand, 0 reorder level, no supplier — and is low stock")
    void appliesEntityDefaults() {
        Category tools = Category.builder().name("Power Tools").build();
        em.persist(tools);

        Product drill = Product.builder()
                .sku("DRILL-18V").name("18V cordless drill").unitPrice(new BigDecimal("89.90"))
                .category(tools)
                .build();
        Product reloaded = em.persistFlushFind(drill);

        assertThat(reloaded.getQuantityOnHand()).isZero();
        assertThat(reloaded.getReorderLevel()).isZero();
        assertThat(reloaded.getSupplier()).isNull();
        // 0 <= 0: a brand-new product with no stock and no reorder level counts as low stock.
        assertThat(reloaded.isLowStock()).isTrue();
    }

    @Test
    @DisplayName("isLowStock is the single rule: on hand <= reorder level")
    void lowStockRule() {
        Product p = Product.builder().reorderLevel(10).build();
        p.setQuantityOnHand(11);
        assertThat(p.isLowStock()).isFalse();
        p.setQuantityOnHand(10);
        assertThat(p.isLowStock()).isTrue();
        p.setQuantityOnHand(0);
        assertThat(p.isLowStock()).isTrue();
    }
}
