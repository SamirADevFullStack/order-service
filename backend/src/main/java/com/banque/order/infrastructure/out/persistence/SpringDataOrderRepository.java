package com.banque.order.infrastructure.out.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository Spring Data : l'outil technique qui parle à la base.
 * À ne pas confondre avec le PORT OrderRepository : celui-ci manipule des entités JPA,
 * le port manipule des objets du domaine.
 */
public interface SpringDataOrderRepository extends JpaRepository<OrderJpaEntity, UUID> {
}
