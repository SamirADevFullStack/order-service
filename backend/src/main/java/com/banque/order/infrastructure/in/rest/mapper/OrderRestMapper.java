package com.banque.order.infrastructure.in.rest.mapper;

import com.banque.order.application.port.in.CreateOrderCommand;
import com.banque.order.domain.model.Money;
import com.banque.order.domain.model.Order;
import com.banque.order.infrastructure.in.rest.api.model.CreateOrderRequest;
import com.banque.order.infrastructure.in.rest.api.model.OrderResponse;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.springframework.stereotype.Component;

/**
 * MAPPER de l'adaptateur REST : DTO GÉNÉRÉS (contrat OpenAPI) <-> cœur de l'application.
 * Les DTO générés restent confinés à l'adaptateur : le domaine ne les voit jamais.
 */
@Component
public class OrderRestMapper {

    public CreateOrderCommand toCommand(CreateOrderRequest request) {
        var lines = request.getLines().stream()
                .map(line -> new CreateOrderCommand.LineItem(
                        line.getProductCode(), line.getQuantity(), line.getUnitPrice()))
                .toList();
        return new CreateOrderCommand(request.getCustomerId(), request.getCurrency(), lines);
    }

    public OrderResponse toResponse(Order order) {
        Money total = order.total();
        return new OrderResponse()
                .id(order.id().value())
                .customerId(order.customerId())
                .status(order.status().name())
                .totalAmount(total.amount())
                .currency(total.currency())
                .createdAt(OffsetDateTime.ofInstant(order.createdAt(), ZoneOffset.UTC));
    }
}
