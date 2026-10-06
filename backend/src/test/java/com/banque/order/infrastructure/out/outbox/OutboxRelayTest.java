package com.banque.order.infrastructure.out.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

/** Le « facteur » : publie ce qui est en attente, et garde pour plus tard ce que Kafka n'a pas accepté. */
class OutboxRelayTest {

    private static final Instant NOW = Instant.parse("2026-10-05T09:00:00Z");

    private final SpringDataOutboxRepository outboxRepository = mock(SpringDataOutboxRepository.class);

    @SuppressWarnings("unchecked")
    private final KafkaTemplate<String, String> kafkaTemplate = mock(KafkaTemplate.class);

    private OutboxRelay relay;

    @BeforeEach
    void setUp() {
        relay = new OutboxRelay(outboxRepository, kafkaTemplate, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void publie_les_evenements_en_attente_puis_les_marque_publies() {
        OutboxEventJpaEntity first = pendingEvent("A", "{\"orderId\":\"A\"}");
        OutboxEventJpaEntity second = pendingEvent("B", "{\"orderId\":\"B\"}");
        when(outboxRepository.findTop100ByPublishedAtIsNullOrderByCreatedAtAsc()).thenReturn(List.of(first, second));
        when(kafkaTemplate.send("orders.created", "A", first.getPayload())).thenReturn(CompletableFuture.completedFuture(null));
        when(kafkaTemplate.send("orders.created", "B", second.getPayload())).thenReturn(CompletableFuture.completedFuture(null));

        relay.publishPendingEvents();

        assertThat(first.getPublishedAt()).isEqualTo(NOW);
        assertThat(second.getPublishedAt()).isEqualTo(NOW);
        verify(outboxRepository).save(first);
        verify(outboxRepository).save(second);
    }

    @Test
    void kafka_indisponible_l_evenement_reste_en_attente_et_rien_n_est_perdu() {
        OutboxEventJpaEntity first = pendingEvent("A", "{\"orderId\":\"A\"}");
        OutboxEventJpaEntity second = pendingEvent("B", "{\"orderId\":\"B\"}");
        when(outboxRepository.findTop100ByPublishedAtIsNullOrderByCreatedAtAsc()).thenReturn(List.of(first, second));
        when(kafkaTemplate.send("orders.created", "A", first.getPayload()))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("Kafka down")));

        relay.publishPendingEvents();

        assertThat(first.getPublishedAt()).isNull();     // sera republié au prochain passage
        assertThat(second.getPublishedAt()).isNull();    // pas envoyé, pour préserver l'ordre
        verify(outboxRepository, never()).save(first);
        verify(kafkaTemplate, never()).send("orders.created", "B", second.getPayload());
    }

    private static OutboxEventJpaEntity pendingEvent(String aggregateId, String payload) {
        return new OutboxEventJpaEntity(UUID.randomUUID(), "Order", aggregateId, "OrderCreated",
                "orders.created", payload, NOW);
    }
}
