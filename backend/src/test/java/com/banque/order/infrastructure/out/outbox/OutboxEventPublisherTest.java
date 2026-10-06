package com.banque.order.infrastructure.out.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.banque.order.domain.model.Money;
import com.banque.order.domain.model.Order;
import com.banque.order.domain.model.OrderCreatedEvent;
import com.banque.order.domain.model.OrderLine;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** L'adaptateur Outbox n'appelle PAS Kafka : il écrit une ligne « à envoyer » dans la table outbox. */
@ExtendWith(MockitoExtension.class)
class OutboxEventPublisherTest {

    private static final Instant NOW = Instant.parse("2026-10-05T09:00:00Z");

    @Mock
    private SpringDataOutboxRepository outboxRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void ecrit_l_evenement_dans_la_table_outbox() throws Exception {
        var publisher = new OutboxEventPublisher(outboxRepository, objectMapper,
                Clock.fixed(NOW, ZoneOffset.UTC), "orders.created");
        Order order = Order.create("C-001",
                List.of(new OrderLine("BOOK", 2, Money.of(new BigDecimal("12.50"), "EUR"))), NOW);

        publisher.publish(OrderCreatedEvent.from(order));

        ArgumentCaptor<OutboxEventJpaEntity> saved = ArgumentCaptor.forClass(OutboxEventJpaEntity.class);
        verify(outboxRepository).save(saved.capture());
        OutboxEventJpaEntity row = saved.getValue();

        assertThat(row.getTopic()).isEqualTo("orders.created");
        assertThat(row.getAggregateId()).isEqualTo(order.id().value().toString());
        assertThat(row.getEventType()).isEqualTo("OrderCreated");
        assertThat(row.getPublishedAt()).isNull();   // pas encore envoyé

        JsonNode payload = objectMapper.readTree(row.getPayload());
        assertThat(payload.get("orderId").asText()).isEqualTo(order.id().value().toString());
        assertThat(payload.get("totalAmount").decimalValue()).isEqualByComparingTo("25.00");
    }
}
