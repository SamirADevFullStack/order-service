package com.banque.order.infrastructure.out.outbox;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Accès à la table outbox_events. */
public interface SpringDataOutboxRepository extends JpaRepository<OutboxEventJpaEntity, UUID> {

    /** Les 100 plus anciens événements pas encore publiés, dans l'ordre de création. */
    List<OutboxEventJpaEntity> findTop100ByPublishedAtIsNullOrderByCreatedAtAsc();
}
