package com.se201411.project.mooncake.order.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.se201411.project.mooncake.enums.*;

public record AdminOrderView(long id, String code, OrderStatus status, String recipient, String phone,
                             String address, String note, BigDecimal subtotal, BigDecimal discount, BigDecimal shipping,
                             BigDecimal roundingAdjustment, BigDecimal total, LocalDateTime createdAt,
                             List<Item> items, List<Cod> payments) {
    public AdminOrderView {
        items = List.copyOf(items);
        payments = List.copyOf(payments);
    }

    public record Item(String name, String sku, int quantity, BigDecimal price, BigDecimal total,
                       List<Component> components) {
        public Item {
            components = List.copyOf(components);
        }
    }

    public record Component(String name, int quantity, ComponentRole role) {
    }

    public record Cod(PaymentStatus status, BigDecimal amount, LocalDateTime paidAt) {
    }
}
