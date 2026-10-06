package com.banque.order.infrastructure.in.kafka;

import com.banque.order.application.port.in.CreateOrderCommand;
import java.math.BigDecimal;
import java.util.List;

/**
 * DTO de l'adaptateur Kafka entrant : le format JSON d'une demande de commande reçue sur le topic order-requests
 * (par exemple envoyée par un autre système de la banque).
 */
public record OrderRequestMessage(String customerId, String currency, List<Line> lines) {

    public record Line(String productCode, int quantity, BigDecimal unitPrice) {
    }

    /** Mapping vers la commande du cas d'usage (ici directement dans le message, pour rester court). */
    public CreateOrderCommand toCommand() {
        var items = lines == null ? List.<CreateOrderCommand.LineItem>of() : lines.stream()
                .map(line -> new CreateOrderCommand.LineItem(line.productCode(), line.quantity(), line.unitPrice()))
                .toList();
        return new CreateOrderCommand(customerId, currency, items);
    }
}
