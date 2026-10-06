package com.banque.order.infrastructure.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/** Entité JPA : une ligne de commande telle qu'elle est stockée en base (table order_lines). */
@Entity
@Table(name = "order_lines")
public class OrderLineJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String productCode;

    private int quantity;

    @Column(precision = 19, scale = 2)
    private BigDecimal unitPrice;

    protected OrderLineJpaEntity() {
        // requis par JPA
    }

    public OrderLineJpaEntity(String productCode, int quantity, BigDecimal unitPrice) {
        this.productCode = productCode;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public String getProductCode() {
        return productCode;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }
}
