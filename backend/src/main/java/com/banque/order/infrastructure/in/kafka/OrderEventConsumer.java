package com.banque.order.infrastructure.in.kafka;

import com.banque.order.application.port.in.CreateOrderUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * ADAPTATEUR ENTRANT KAFKA : un second point d'entrée vers le MÊME cas d'usage que le contrôleur REST.
 * C'est tout l'intérêt de l'hexagonale : le cœur ne sait pas s'il est appelé en HTTP ou par un message.
 */
@Component
public class OrderEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderEventConsumer.class);

    private final CreateOrderUseCase createOrderUseCase;

    public OrderEventConsumer(CreateOrderUseCase createOrderUseCase) {
        this.createOrderUseCase = createOrderUseCase;
    }

    @KafkaListener(topics = "${app.kafka.topics.order-requests}")
    public void onOrderRequest(OrderRequestMessage message) {
        log.info("Demande de commande reçue par Kafka pour le client {}", message.customerId());
        createOrderUseCase.create(message.toCommand());
    }
}
