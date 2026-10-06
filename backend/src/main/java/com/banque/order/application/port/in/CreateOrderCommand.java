package com.banque.order.application.port.in;

import java.math.BigDecimal;
import java.util.List;

/**
 * Les données d'entrée du cas d'usage « créer une commande ».
 * Format neutre : ni DTO REST, ni message Kafka. Chaque adaptateur entrant traduit son format vers celui-ci.
 */
public record CreateOrderCommand(String customerId, String currency, List<LineItem> lines) {

    public record LineItem(String productCode, int quantity, BigDecimal unitPrice) {
    }
}
