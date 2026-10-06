package com.banque.order.domain.model;

import java.time.Instant;

/**
 * Événement métier : « une commande a été créée ». C'est un fait passé, immuable.
 * Le domaine ne sait pas qu'il partira dans Kafka : c'est l'adaptateur de messagerie qui s'en charge.
 */
public record OrderCreatedEvent(OrderId orderId, String customerId, Money total, Instant occurredAt) {

    public static OrderCreatedEvent from(Order order) {
        return new OrderCreatedEvent(order.id(), order.customerId(), order.total(), order.createdAt());
    }
}
