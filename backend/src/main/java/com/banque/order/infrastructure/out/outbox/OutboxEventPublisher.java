package com.banque.order.infrastructure.out.outbox;

import com.banque.order.application.port.out.EventPublisher;
import com.banque.order.domain.model.OrderCreatedEvent;
import com.banque.order.infrastructure.out.messaging.OrderCreatedMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * ADAPTATEUR SORTANT « OUTBOX » : implémente le port EventPublisher SANS appeler Kafka.
 * Il écrit l'événement dans la table outbox_events, dans la transaction de la commande.
 * C'est OutboxRelay qui l'enverra ensuite dans Kafka.
 *
 * Le cœur (port, service, domaine) n'a pas changé : on a seulement remplacé l'adaptateur.
 * Actif par défaut (app.events.publication=outbox).
 */
@Component
@ConditionalOnProperty(name = "app.events.publication", havingValue = "outbox", matchIfMissing = true)
public class OutboxEventPublisher implements EventPublisher {

    private final SpringDataOutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final String topic;

    public OutboxEventPublisher(SpringDataOutboxRepository outboxRepository, ObjectMapper objectMapper, Clock clock,
                                @Value("${app.kafka.topics.order-created}") String topic) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.topic = topic;
    }

    /**
     * MANDATORY : refuse de s'exécuter hors transaction.
     * Garde-fou : si quelqu'un appelle publish() sans transaction, on le sait tout de suite au lieu de perdre l'atomicité.
     */
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void publish(OrderCreatedEvent event) {
        OrderCreatedMessage message = OrderCreatedMessage.from(event);
        outboxRepository.save(new OutboxEventJpaEntity(
                UUID.randomUUID(),
                "Order",
                message.orderId(),
                "OrderCreated",
                topic,
                toJson(message),
                clock.instant()));
    }

    private String toJson(OrderCreatedMessage message) {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Impossible de sérialiser l'événement " + message.orderId(), exception);
        }
    }
}
