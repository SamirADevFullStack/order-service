package com.banque.order.infrastructure.in.rest.mapper;

import com.banque.order.application.pagination.PageResult;
import com.banque.order.application.port.in.CreateOrderCommand;
import com.banque.order.application.search.OrderSearchCriteria;
import com.banque.order.application.search.SortOrder;
import com.banque.order.domain.model.Money;
import com.banque.order.domain.model.Order;
import com.banque.order.infrastructure.in.rest.api.model.*;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

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

    /** Paramètres de la requête (contrat) → critères de recherche (cœur). */
    public OrderSearchCriteria toCriteria(String q, OrderSort sort) {
        return new OrderSearchCriteria(q, toSortOrder(sort));
    }

    /** OrderSort (valeurs du contrat) → SortOrder (vocabulaire du métier). Le switch couvre toutes les valeurs. */
    private SortOrder toSortOrder(OrderSort sort) {
        if (sort == null) {
            return null;   // les critères appliqueront l'ordre par défaut
        }
        return switch (sort) {
            case CREATED_AT_DESC -> SortOrder.NEWEST_FIRST;
            case CREATED_AT_ASC -> SortOrder.OLDEST_FIRST;
            case TOTAL_DESC -> SortOrder.HIGHEST_TOTAL_FIRST;
            case TOTAL_ASC -> SortOrder.LOWEST_TOTAL_FIRST;
        };
    }

    public OrderPage toPage(PageResult<Order> page) {
        PageResult<OrderSummary> summaries = page.map(this::toSummary);
        return new OrderPage()
                .content(summaries.content())
                .page(summaries.page())
                .size(summaries.size())
                .totalElements(summaries.totalElements())
                .totalPages(summaries.totalPages());
    }

    private OrderLine toLine(com.banque.order.domain.model.OrderLine line) {
        return new OrderLine()
                .productCode(line.productCode())
                .quantity(line.quantity())
                .unitPrice(line.unitPrice().amount())
                .lineTotal(line.subtotal().amount());
    }

    public OrderSummary toSummary(Order order) {
        Money total = order.total();
        return new OrderSummary()
                .id(order.id().value())
                .status(order.status().name())
                .totalAmount(total.amount())
                .currency(total.currency())
                .createdAt(OffsetDateTime.ofInstant(order.createdAt(), ZoneOffset.UTC));
    }

    public OrderDetail toDetail(Order order) {
        Money total = order.total();
        return new OrderDetail()
                .id(order.id().value())
                .customerId(order.customerId())
                .status(order.status().name())
                .totalAmount(total.amount())
                .currency(total.currency())
                .createdAt(OffsetDateTime.ofInstant(order.createdAt(), ZoneOffset.UTC))
                .lines(order.lines().stream().map(this::toLine).toList());
    }
}
