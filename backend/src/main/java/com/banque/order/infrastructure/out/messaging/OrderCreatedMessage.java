package com.banque.order.infrastructure.out.messaging;

import com.banque.order.domain.model.OrderCreatedEvent;
import java.math.BigDecimal;

/**
 * DTO de l'adaptateur Kafka sortant : le contrat JSON publié sur le topic orders.created.
 * Séparé de l'événement du domaine : on peut faire évoluer le domaine sans casser les consommateurs.
 */
public record OrderCreatedMessage(
        String orderId,
        String customerId,
        BigDecimal totalAmount,
        String currency,
        String occurredAt) {

    public static OrderCreatedMessage from(OrderCreatedEvent event) {
        return new OrderCreatedMessage(
                event.orderId().value().toString(),
                event.customerId(),
                event.total().amount(),
                event.total().currency(),
                event.occurredAt().toString());
    }
}
