package com.banque.order.infrastructure.out.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Une ligne de la table outbox_events : un événement « à envoyer ».
 * Elle est écrite dans la MÊME transaction que la commande, puis publiée dans Kafka par OutboxRelay.
 * published_at reste NULL tant que l'événement n'est pas parti.
 */
@Entity
@Table(name = "outbox_events", indexes = @Index(name = "idx_outbox_pending", columnList = "publishedAt, createdAt"))
public class OutboxEventJpaEntity {

    @Id
    private UUID id;

    private String aggregateType;   // ex. Order

    private String aggregateId;     // id de la commande : sert de clé Kafka (ordre garanti par commande)

    private String eventType;       // ex. OrderCreated

    private String topic;           // topic Kafka de destination

    @Column(length = 4000)
    private String payload;         // le message JSON, prêt à être envoyé

    private Instant createdAt;

    private Instant publishedAt;    // NULL = pas encore publié

    protected OutboxEventJpaEntity() {
        // requis par JPA
    }

    public OutboxEventJpaEntity(UUID id, String aggregateType, String aggregateId, String eventType,
                                String topic, String payload, Instant createdAt) {
        this.id = id;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.topic = topic;
        this.payload = payload;
        this.createdAt = createdAt;
    }

    public void markPublished(Instant when) {
        this.publishedAt = when;
    }

    public UUID getId() {
        return id;
    }

    public String getAggregateType() {
        return aggregateType;
    }

    public String getAggregateId() {
        return aggregateId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getTopic() {
        return topic;
    }

    public String getPayload() {
        return payload;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }
}
