package com.banque.order.domain.model;

import com.banque.order.domain.exception.InvalidOrderException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Entité et racine d'agrégat : la commande et ses lignes.
 * Toutes les règles métier de la commande vivent ici, en Java pur :
 * aucune annotation Spring, JPA ou Jackson.
 */
public class Order {

    /** Règle métier : plafond d'une commande, quelle que soit la devise. */
    public static final BigDecimal MAX_TOTAL = new BigDecimal("10000.00");

    private final OrderId id;
    private final String customerId;
    private final List<OrderLine> lines;
    private final OrderStatus status;
    private final Instant createdAt;

    private Order(OrderId id, String customerId, List<OrderLine> lines, OrderStatus status, Instant createdAt) {
        this.id = Objects.requireNonNull(id);
        this.customerId = Objects.requireNonNull(customerId);
        this.lines = List.copyOf(lines);
        this.status = Objects.requireNonNull(status);
        this.createdAt = Objects.requireNonNull(createdAt);
    }

    /** Crée une NOUVELLE commande en appliquant les règles métier. */
    public static Order create(String customerId, List<OrderLine> lines, Instant now) {
        if (customerId == null || customerId.isBlank()) {
            throw new InvalidOrderException("Le client est obligatoire");
        }
        if (lines == null || lines.isEmpty()) {
            throw new InvalidOrderException("Une commande doit contenir au moins une ligne");
        }
        long currencies = lines.stream().map(line -> line.unitPrice().currency()).distinct().count();
        if (currencies > 1) {
            throw new InvalidOrderException("Toutes les lignes doivent être dans la même devise");
        }
        Order order = new Order(OrderId.newId(), customerId, lines, OrderStatus.CREATED, now);
        if (order.total().amount().compareTo(MAX_TOTAL) > 0) {
            throw new InvalidOrderException("Le montant d'une commande ne peut pas dépasser " + MAX_TOTAL);
        }
        return order;
    }

    /** Reconstruit une commande EXISTANTE, par exemple relue en base, sans rejouer la création. */
    public static Order restore(OrderId id, String customerId, List<OrderLine> lines,
                                OrderStatus status, Instant createdAt) {
        return new Order(id, customerId, lines, status, createdAt);
    }

    /** Règle métier : le total est la somme des sous-totaux des lignes. */
    public Money total() {
        return lines.stream()
                .map(OrderLine::subtotal)
                .reduce(Money::add)
                .orElseThrow();
    }

    public OrderId id() {
        return id;
    }

    public String customerId() {
        return customerId;
    }

    public List<OrderLine> lines() {
        return lines;
    }

    public OrderStatus status() {
        return status;
    }

    public Instant createdAt() {
        return createdAt;
    }

    /** Une entité est identifiée par son id, pas par ses valeurs. */
    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof Order other && id.equals(other.id));
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
