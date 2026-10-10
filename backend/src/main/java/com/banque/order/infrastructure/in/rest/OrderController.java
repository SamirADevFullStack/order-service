package com.banque.order.infrastructure.in.rest;

import com.banque.order.application.pagination.PageQuery;
import com.banque.order.application.pagination.PageResult;
import com.banque.order.application.port.in.CreateOrderUseCase;
import com.banque.order.application.port.in.GetOrderUseCase;
import com.banque.order.application.port.in.ListOrdersUseCase;
import com.banque.order.domain.model.Order;
import com.banque.order.domain.model.OrderId;
import com.banque.order.infrastructure.in.rest.api.OrdersApi;
import com.banque.order.infrastructure.in.rest.api.model.CreateOrderRequest;
import com.banque.order.infrastructure.in.rest.api.model.OrderDetail;
import com.banque.order.infrastructure.in.rest.api.model.OrderPage;
import com.banque.order.infrastructure.in.rest.api.model.OrderResponse;
import com.banque.order.infrastructure.in.rest.mapper.OrderRestMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

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
    private final GetOrderUseCase getOrderUseCase;

    public OrderController(CreateOrderUseCase createOrderUseCase, OrderRestMapper mapper, ListOrdersUseCase listOrdersUseCase, GetOrderUseCase getOrderUseCase) {
        this.createOrderUseCase = createOrderUseCase;
        this.mapper = mapper;
        this.listOrdersUseCase = listOrdersUseCase;
        this.getOrderUseCase = getOrderUseCase;
    }

    @Override
    public ResponseEntity<OrderPage> listOrders(Integer page, Integer size) {
        PageResult<Order> orders = listOrdersUseCase.list(new PageQuery(page, size));   // 1 + 2
        return ResponseEntity.ok(mapper.toPage(orders));                                // 3
    }

    @Override
    public ResponseEntity<OrderDetail> getOrder(UUID orderId) {
        Order order = getOrderUseCase.get(new OrderId(orderId));
        return ResponseEntity.ok(mapper.toDetail(order));
    }

    @Override
    public ResponseEntity<OrderResponse> createOrder(CreateOrderRequest createOrderRequest) {
        Order order = createOrderUseCase.create(mapper.toCommand(createOrderRequest));
        return ResponseEntity
                .created(URI.create("/api/orders/" + order.id().value()))
                .body(mapper.toResponse(order));
    }
}


