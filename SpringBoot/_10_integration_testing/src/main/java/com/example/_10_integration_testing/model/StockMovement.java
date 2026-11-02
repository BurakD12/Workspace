package com.example._10_integration_testing.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Lab 9 (StockFlow) — one line in the stock ledger: "at this instant, this
 * person moved N units of
 * this product IN or OUT, for this reason, leaving this many on hand".
 *
 * <p>
 * <b>Append-only.</b> A movement is written once and never updated or deleted —
 * that is what makes
 * it a ledger rather than a note. If a movement was wrong, you record a
 * compensating one. So there
 * is no PUT or DELETE endpoint for movements, and the service exposes only
 * {@code record} and a
 * paginated read.
 *
 * <p>
 * {@code quantityAfter} is the product's {@code quantityOnHand} <em>immediately
 * after</em> this
 * movement was applied. Storing it makes each ledger line self-describing (you
 * can read the history
 * without replaying it) and makes "does the ledger agree with the product?" a
 * trivial check.
 *
 * <p>
 * {@code recordedBy} is always the authenticated principal, never a request
 * field — the same
 * server-controlled rule TaskFlow (Lab 7) used for a project's owner.
 * {@code occurredAt} is stamped by a JPA
 * lifecycle callback so no caller has to remember it.
 */
@Entity
@Table(name = "stock_movements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 8)
    private MovementType type;

    /** Always positive; the direction lives in {@code type}. */
    @Column(nullable = false)
    private int quantity;

    @Column(length = 255)
    private String reason;

    /** The product's quantityOnHand right after this movement was applied. */
    @Column(nullable = false)
    private int quantityAfter;

    @Column(nullable = false, updatable = false)
    private Instant occurredAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recorded_by_id", nullable = false)
    private User recordedBy;

    @PrePersist
    void stampOccurredAt() {
        if (occurredAt == null) {
            occurredAt = Instant.now();
        }
    }
}
