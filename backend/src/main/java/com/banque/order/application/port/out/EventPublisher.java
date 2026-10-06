package com.banque.order.application.port.out;

import com.banque.order.domain.model.OrderCreatedEvent;

/**
 * PORT SORTANT : publier un événement métier.
 * Le cœur ne sait pas si c'est Kafka, RabbitMQ ou un simple log derrière.
 */
public interface EventPublisher {

    void publish(OrderCreatedEvent event);
}
