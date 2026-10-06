package com.banque.order.infrastructure.out.messaging;

import com.banque.order.application.port.out.EventPublisher;
import com.banque.order.domain.model.OrderCreatedEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * ADAPTATEUR SORTANT « DIRECT » (version naïve, gardée pour comparer avec l'Outbox).
 * Il publie dans Kafka immédiatement : la base et Kafka sont deux systèmes, l'écriture n'est donc PAS atomique.
 * Si Kafka est indisponible, l'événement est perdu alors que la commande est en base.
 * Activé seulement avec app.events.publication=direct.
 *
 * Clé du message = id de la commande : tous les événements d'une commande vont dans la même partition.
 */
@Component
@ConditionalOnProperty(name = "app.events.publication", havingValue = "direct")
public class KafkaEventPublisher implements EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaEventPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String topic;

    public KafkaEventPublisher(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper,
                               @Value("${app.kafka.topics.order-created}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.topic = topic;
    }

    @Override
    public void publish(OrderCreatedEvent event) {
        OrderCreatedMessage message = OrderCreatedMessage.from(event);
        kafkaTemplate.send(topic, message.orderId(), toJson(message))
                .whenComplete((result, exception) -> {
                    if (exception != null) {
                        log.error("Échec de publication de la commande {} : événement PERDU", message.orderId(), exception);
                    } else {
                        log.info("Commande {} publiée sur {} (partition {}, offset {})", message.orderId(), topic,
                                result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
                    }
                });
    }

    private String toJson(OrderCreatedMessage message) {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Impossible de sérialiser l'événement " + message.orderId(), exception);
        }
    }
}
