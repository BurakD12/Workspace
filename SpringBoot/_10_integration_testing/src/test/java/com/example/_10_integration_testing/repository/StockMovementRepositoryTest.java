package com.example._10_integration_testing.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;

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

import com.example._10_integration_testing.model.Category;
import com.example._10_integration_testing.model.MovementType;
import com.example._10_integration_testing.model.Product;
import com.example._10_integration_testing.model.Role;
import com.example._10_integration_testing.model.StockMovement;
import com.example._10_integration_testing.model.User;

@DataJpaTest
@DisplayName("StockMovementRepository (ledger read + reference probe)")
class StockMovementRepositoryTest {

        @Autowired
        private StockMovementRepository stockMovementRepository;
        @Autowired
        private TestEntityManager em;

        private Long boltId;
        private Long nutId;

        @BeforeEach
        void seed() {
                User bob = User.builder().username("bob").email("b@x.io").password("h").roles(Set.of(Role.CLERK))
                                .build();
                em.persist(bob);
                Category fasteners = Category.builder().name("Fasteners").build();
                em.persist(fasteners);
                Product bolt = Product.builder().sku("BOLT-M8-50").name("M8 bolt").unitPrice(BigDecimal.ONE)
                                .category(fasteners).build();
                Product nut = Product.builder().sku("NUT-M8").name("M8 nut").unitPrice(BigDecimal.ONE)
                                .category(fasteners).build();
                em.persist(bolt);
                em.persist(nut);

                // Three lines on the bolt's ledger, persisted OUT of chronological order so the
                // sort is real.
                em.persist(StockMovement.builder().product(bolt).type(MovementType.OUT).quantity(20).quantityAfter(230)
                                .recordedBy(bob).occurredAt(Instant.parse("2026-09-02T10:00:00Z")).build());
                em.persist(StockMovement.builder().product(bolt).type(MovementType.IN).quantity(250).quantityAfter(250)
                                .recordedBy(bob).occurredAt(Instant.parse("2026-09-01T09:00:00Z")).build());
                em.persist(StockMovement.builder().product(bolt).type(MovementType.OUT).quantity(30).quantityAfter(200)
                                .recordedBy(bob).occurredAt(Instant.parse("2026-09-03T11:00:00Z")).build());

                em.flush();
                em.clear();
                boltId = bolt.getId();
                nutId = nut.getId();
        }

        @Test
        @DisplayName("findByProductId pages one product's ledger; the Pageable's sort decides the order")
        void ledgerNewestFirst() {
                Page<StockMovement> page = stockMovementRepository.findByProductId(
                                boltId, PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "occurredAt")));

                assertThat(page.getTotalElements()).isEqualTo(3);
                assertThat(page.getContent()).extracting(StockMovement::getQuantityAfter).containsExactly(200, 230,
                                250);
                // The entity graph fetched both LAZY sides: they are initialised entities, not
                // proxies
                // (checked BEFORE reading them — inside a @DataJpaTest a proxy would initialise
                // on access).
                StockMovement newest = page.getContent().get(0);
                assertThat(Hibernate.isInitialized(newest.getRecordedBy())).isTrue();
                assertThat(Hibernate.isInitialized(newest.getProduct())).isTrue();
                assertThat(newest.getRecordedBy().getUsername()).isEqualTo("bob");
                assertThat(newest.getProduct().getSku()).isEqualTo("BOLT-M8-50");
        }

        @Test
        @DisplayName("findByProductId is empty for a product with no history")
        void emptyLedger() {
                assertThat(stockMovementRepository.findByProductId(nutId, PageRequest.of(0, 10)).getContent())
                                .isEmpty();
        }

        @Test
        @DisplayName("existsByProductId is the 'has history?' probe a delete asks first")
        void referenceProbe() {
                assertThat(stockMovementRepository.existsByProductId(boltId)).isTrue();
                assertThat(stockMovementRepository.existsByProductId(nutId)).isFalse();
        }

        @Test
        @DisplayName("@PrePersist stamps occurredAt when the caller did not set it")
        void occurredAtIsStamped() {
                User carol = User.builder().username("carol").email("c@x.io").password("h").roles(Set.of(Role.CLERK))
                                .build();
                em.persist(carol);
                Product nut = em.find(Product.class, nutId);

                StockMovement saved = stockMovementRepository.save(StockMovement.builder()
                                .product(nut).type(MovementType.IN).quantity(1).quantityAfter(1).recordedBy(carol)
                                .build());
                em.flush();

                assertThat(saved.getOccurredAt()).isNotNull();
        }
}
