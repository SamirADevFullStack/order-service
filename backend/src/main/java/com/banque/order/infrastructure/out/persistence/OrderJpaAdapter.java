package com.banque.order.infrastructure.out.persistence;

import com.banque.order.application.pagination.PageQuery;
import com.banque.order.application.pagination.PageResult;
import com.banque.order.application.port.out.OrderRepository;
import com.banque.order.application.search.OrderSearchCriteria;
import com.banque.order.application.search.SortOrder;
import com.banque.order.domain.model.Order;
import com.banque.order.domain.model.OrderId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * ADAPTATEUR SORTANT DE PERSISTANCE : implémente le port OrderRepository avec JPA.
 * Pour passer de H2 à Oracle, ou de JPA à MongoDB, on ne change que cette classe et ses voisines.
 */
@Component
public class OrderJpaAdapter implements OrderRepository {

    private final SpringDataOrderRepository jpaRepository;
    private final OrderPersistenceMapper mapper;

    public OrderJpaAdapter(SpringDataOrderRepository jpaRepository, OrderPersistenceMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public Order save(Order order) {
        OrderJpaEntity saved = jpaRepository.save(mapper.toEntity(order));
        return mapper.toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Order> findById(OrderId id) {
        return jpaRepository.findById(id.value()).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Order> findAll(OrderSearchCriteria criteria, PageQuery query) {
        // 1. PageQuery (à nous) → PageRequest (Spring Data), AVEC le tri demandé dans les critères
        PageRequest pageable = PageRequest.of(query.page(), query.size(), toSort(criteria.sort()));

        // 2. la requête : Spring Data fait le SELECT paginé (avec le WHERE des critères) ET le COUNT(*) pour le total
        Page<OrderJpaEntity> page = jpaRepository.findAll(OrderSpecifications.matching(criteria), pageable);

        // 3. entités JPA → objets du domaine, avec le mapper existant
        List<Order> orders = page.getContent().stream().map(mapper::toDomain).toList();

        // 4. Page (Spring) → PageResult (à nous)
        return new PageResult<>(orders, query.page(), query.size(), page.getTotalElements());
    }

    /**
     * SortOrder (vocabulaire du métier) → Sort (Spring Data, noms des champs JPA).
     * Le tri secondaire sur l'id rend l'ordre STABLE : sans lui, deux commandes de même montant pourraient
     * changer de place d'une requête à l'autre, et apparaître sur deux pages, ou sur aucune.
     */
    private static Sort toSort(SortOrder sortOrder) {
        Sort primary = switch (sortOrder) {
            case NEWEST_FIRST -> Sort.by(Sort.Direction.DESC, "createdAt");
            case OLDEST_FIRST -> Sort.by(Sort.Direction.ASC, "createdAt");
            case HIGHEST_TOTAL_FIRST -> Sort.by(Sort.Direction.DESC, "totalAmount");
            case LOWEST_TOTAL_FIRST -> Sort.by(Sort.Direction.ASC, "totalAmount");
        };
        return primary.and(Sort.by("id"));
    }
}
