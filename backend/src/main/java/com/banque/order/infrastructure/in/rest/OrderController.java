package com.banque.order.infrastructure.in.rest;

import com.banque.order.application.port.in.CreateOrderUseCase;
import com.banque.order.application.port.in.ListOrdersUseCase;
import com.banque.order.domain.model.Order;
import com.banque.order.infrastructure.in.rest.api.OrdersApi;
import com.banque.order.infrastructure.in.rest.api.model.CreateOrderRequest;
import com.banque.order.infrastructure.in.rest.api.model.OrderDetail;
import com.banque.order.infrastructure.in.rest.api.model.OrderPage;
import com.banque.order.infrastructure.in.rest.api.model.OrderResponse;
import com.banque.order.infrastructure.in.rest.mapper.OrderRestMapper;
import java.net.URI;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * ADAPTATEUR ENTRANT REST, en API-first.
 * L'interface OrdersApi est GÉNÉRÉE depuis src/main/resources/static/openapi/order-api.yaml :
 * elle porte la route, le verbe HTTP, @RequestBody et @Valid. Le contrôleur se contente de l'implémenter.
 * Si le contrat change, le build échoue tant que le contrôleur ne suit pas : le code ne peut pas dériver du contrat.
 */
@RestController
public class OrderController implements OrdersApi {

    private final CreateOrderUseCase createOrderUseCase;
    private final OrderRestMapper mapper;
    private final ListOrdersUseCase listOrdersUseCase;

    public OrderController(CreateOrderUseCase createOrderUseCase, OrderRestMapper mapper, ListOrdersUseCase listOrdersUseCase) {
        this.createOrderUseCase = createOrderUseCase;
        this.mapper = mapper;
        this.listOrdersUseCase = listOrdersUseCase;
    }

    // TODO 2c : implémentation provisoire, pour que le projet compile pendant 2b
    @Override
    public ResponseEntity<OrderPage> listOrders(Integer page, Integer size) {
        throw new UnsupportedOperationException("À implémenter en 2c");
    }

    @Override
    public ResponseEntity<OrderDetail> getOrder(UUID orderId) {
        throw new UnsupportedOperationException("À implémenter en 2c");
    }

    @Override
    public ResponseEntity<OrderResponse> createOrder(CreateOrderRequest createOrderRequest) {
        Order order = createOrderUseCase.create(mapper.toCommand(createOrderRequest));
        return ResponseEntity
                .created(URI.create("/api/orders/" + order.id().value()))
                .body(mapper.toResponse(order));
    }
}


