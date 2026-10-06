package com.banque.order.infrastructure.in.rest;

import com.banque.order.application.port.in.CreateOrderUseCase;
import com.banque.order.domain.model.Order;
import com.banque.order.infrastructure.in.rest.api.OrdersApi;
import com.banque.order.infrastructure.in.rest.api.model.CreateOrderRequest;
import com.banque.order.infrastructure.in.rest.api.model.OrderResponse;
import com.banque.order.infrastructure.in.rest.mapper.OrderRestMapper;
import java.net.URI;
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

    public OrderController(CreateOrderUseCase createOrderUseCase, OrderRestMapper mapper) {
        this.createOrderUseCase = createOrderUseCase;
        this.mapper = mapper;
    }

    @Override
    public ResponseEntity<OrderResponse> createOrder(CreateOrderRequest createOrderRequest) {
        Order order = createOrderUseCase.create(mapper.toCommand(createOrderRequest));
        return ResponseEntity
                .created(URI.create("/api/orders/" + order.id().value()))
                .body(mapper.toResponse(order));
    }
}
