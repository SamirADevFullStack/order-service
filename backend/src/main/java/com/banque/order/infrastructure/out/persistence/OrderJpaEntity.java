package com.banque.order.infrastructure.out.persistence;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entité JPA : la commande telle qu'elle est stockée en base (table orders).
 * Elle porte les annotations techniques, pour que la classe Order du domaine n'en ait aucune.
 */
@Entity
@Table(name = "orders")
public class OrderJpaEntity {

    @Id
    private UUID id;

    private String customerId;

    private String status;

    private String currency;

    private Instant createdAt;

    /**
     * Montant total, DÉNORMALISÉ : il se déduit des lignes, mais on le stocke pour pouvoir trier (et filtrer) en SQL.
     * Il est calculé par le domaine (Order.total()) au moment de l'enregistrement, jamais saisi à la main.
     */
    @Column(precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "order_id", nullable = false)
    private List<OrderLineJpaEntity> lines = new ArrayList<>();

    protected OrderJpaEntity() {
        // requis par JPA
    }

    public OrderJpaEntity(UUID id, String customerId, String status, String currency,
                          Instant createdAt, BigDecimal totalAmount, List<OrderLineJpaEntity> lines) {
        this.id = id;
        this.customerId = customerId;
        this.status = status;
        this.currency = currency;
        this.createdAt = createdAt;
        this.totalAmount = totalAmount;
        this.lines = new ArrayList<>(lines);
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public UUID getId() {
        return id;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getStatus() {
        return status;
    }

    public String getCurrency() {
        return currency;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<OrderLineJpaEntity> getLines() {
        return lines;
    }
}
