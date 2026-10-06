package com.banque.order.infrastructure.out.persistence;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
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

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "order_id", nullable = false)
    private List<OrderLineJpaEntity> lines = new ArrayList<>();

    protected OrderJpaEntity() {
        // requis par JPA
    }

    public OrderJpaEntity(UUID id, String customerId, String status, String currency,
                          Instant createdAt, List<OrderLineJpaEntity> lines) {
        this.id = id;
        this.customerId = customerId;
        this.status = status;
        this.currency = currency;
        this.createdAt = createdAt;
        this.lines = new ArrayList<>(lines);
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
