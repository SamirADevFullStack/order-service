package com.banque.order.application.service;

import com.banque.order.application.pagination.PageQuery;
import com.banque.order.application.pagination.PageResult;
import com.banque.order.application.port.in.ListOrdersUseCase;
import com.banque.order.application.port.out.OrderRepository;
import com.banque.order.domain.model.Order;

/** Cas d'usage de lecture : il délègue au port sortant. Aucune annotation Spring. */
public class ListOrdersService implements ListOrdersUseCase {

    private final OrderRepository orderRepository;

    public ListOrdersService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public PageResult<Order> list(PageQuery query) {
        // une seule ligne, qui délègue au port sortant
        return orderRepository.findAll(query);
    }
}