package com.banque.order.infrastructure.out.persistence;

import com.banque.order.application.pagination.PageQuery;
import com.banque.order.application.port.out.OrderRepository;
import com.banque.order.domain.model.Order;
import com.banque.order.domain.model.OrderId;
import java.util.Optional;

import com.banque.order.application.pagination.PageResult;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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

    // TODO 2c : implémentation provisoire, pour que le projet compile pendant 2b
    @Override
    public PageResult<Order> findAll(PageQuery query) {
        throw new UnsupportedOperationException("À implémenter en 2c");
    }
}
