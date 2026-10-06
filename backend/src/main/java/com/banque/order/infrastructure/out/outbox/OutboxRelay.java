package com.banque.order.infrastructure.out.outbox;

import java.time.Clock;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * RELAIS OUTBOX (le « facteur ») : toutes les secondes, il lit les événements pas encore publiés
 * et les envoie dans Kafka, dans l'ordre de création.
 *
 * - Kafka indisponible : l'envoi échoue, la ligne reste avec published_at = NULL, on réessaie au passage suivant.
 *   Aucun événement n'est perdu.
 * - Crash entre l'envoi et la mise à jour de published_at : l'événement sera renvoyé.
 *   Livraison « au moins une fois » : les consommateurs doivent être idempotents.
 * - Plusieurs instances de l'application : elles publieraient les mêmes lignes.
 *   En production : SELECT ... FOR UPDATE SKIP LOCKED, ShedLock, ou Debezium (CDC) à la place de ce relais.
 */
@Component
@ConditionalOnProperty(name = "app.events.publication", havingValue = "outbox", matchIfMissing = true)
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);

    private final SpringDataOutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final Clock clock;

    public OutboxRelay(SpringDataOutboxRepository outboxRepository, KafkaTemplate<String, String> kafkaTemplate,
                       Clock clock) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${app.outbox.relay-interval-ms:1000}")
    public void publishPendingEvents() {
        for (OutboxEventJpaEntity event : outboxRepository.findTop100ByPublishedAtIsNullOrderByCreatedAtAsc()) {
            try {
                // Envoi SYNCHRONE : on attend l'accusé de réception de Kafka avant de marquer l'événement publié.
                kafkaTemplate.send(event.getTopic(), event.getAggregateId(), event.getPayload())
                        .get(10, TimeUnit.SECONDS);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception exception) {
                log.warn("Kafka indisponible : l'événement {} ({}) sera republié au prochain passage",
                        event.getId(), event.getEventType());
                return; // on s'arrête pour ne pas publier les événements suivants dans le désordre
            }
            event.markPublished(clock.instant());
            outboxRepository.save(event);
            log.info("Événement {} publié sur {} pour la commande {}",
                    event.getEventType(), event.getTopic(), event.getAggregateId());
        }
    }
}
