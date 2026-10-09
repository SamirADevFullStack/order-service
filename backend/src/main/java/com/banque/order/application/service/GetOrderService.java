package com.banque.order.application.service;

import com.banque.order.application.port.in.GetOrderUseCase;
import com.banque.order.application.port.out.OrderRepository;
import com.banque.order.domain.exception.OrderNotFoundException;
import com.banque.order.domain.model.Order;
import com.banque.order.domain.model.OrderId;

/** Cas d'usage de lecture : une commande absente devient une exception métier. Aucune annotation Spring. */
public class GetOrderService implements GetOrderUseCase {

    private final OrderRepository orderRepository;

    public GetOrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public Order get(OrderId id) {
        // à toi : findById renvoie un Optional<Order>
        //   → la commande si elle existe, sinon lever une OrderNotFoundException(id)
        return orderRepository.findById(id).orElseThrow(() -> new OrderNotFoundException(id));
    }
}