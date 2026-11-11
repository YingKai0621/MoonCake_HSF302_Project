package com.se201411.project.mooncake.order.dto;

import com.se201411.project.mooncake.common.format.MoneyFormat;
import com.se201411.project.mooncake.entity.SalesOrder;
import java.util.List;

public record OrderSuccessView(
    String orderCode,
    String recipientName,
    String recipientPhone,
    String shippingAddress,
    String orderNote,
    List<OrderSuccessItemView> items,
    int totalQuantity,
    String formattedSubtotal,
    String formattedDiscount,
    String formattedShippingFee,
    String formattedRoundingAdjustment,
    String formattedTotal
) {

    /** Entity -> DTO. The service maps the order lines and counts the cakes, then passes them in. */
    public static OrderSuccessView entityToDto(SalesOrder order, List<OrderSuccessItemView> items, int totalQuantity) {
        return new OrderSuccessView(
            order.getOrderCode(),
            order.getRecipientName(),
            order.getRecipientPhone(),
            order.getShippingAddressSnapshot(),
            order.getOrderNote(),
            items,
            totalQuantity,
            MoneyFormat.vnd(order.getSubtotalAmount()),
            MoneyFormat.vnd(order.getDiscountAmount()),
            MoneyFormat.vnd(order.getShippingFee()),
            MoneyFormat.signedVnd(order.getRoundingAdjustment()),
            MoneyFormat.vnd(order.getTotalAmount())
        );
    }

    public OrderSuccessView {
        items = List.copyOf(items);
    }
}
