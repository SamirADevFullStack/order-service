package com.banque.order.infrastructure.out.persistence;

import com.banque.order.application.pagination.PageQuery;
import com.banque.order.application.pagination.PageResult;
import com.banque.order.application.port.out.OrderRepository;
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
    public PageResult<Order> findAll(PageQuery query) {
        // 1. PageQuery (à nous) → PageRequest (Spring Data), AVEC le tri promis par le port
        PageRequest pageable = PageRequest.of(query.page(), query.size(), Sort.by(Sort.Direction.DESC, "createdAt"));

        // 2. la requête : Spring Data fait le SELECT paginé ET le COUNT(*) pour le total
        Page<OrderJpaEntity> page = jpaRepository.findAll(pageable);

        // 3. entités JPA → objets du domaine, avec le mapper existant
        List<Order> orders = page.getContent().stream().map(mapper::toDomain).toList();

        // 4. Page (Spring) → PageResult (à nous)
        return new PageResult<>(orders, query.page(), query.size(), page.getTotalElements());
    }
}
