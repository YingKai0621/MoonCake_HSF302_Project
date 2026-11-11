package com.se201411.project.mooncake.order.dto;

import com.se201411.project.mooncake.common.format.VietnamTime;
import com.se201411.project.mooncake.entity.SalesOrder;
import com.se201411.project.mooncake.enums.ShippingCarrier;

public record OrderFulfilmentView(
    String placedAt,
    String processingAt,
    String shippingAt,
    String deliveredAt,
    String completedAt,
    String customerConfirmedAt,
    String cancelledAt,
    String carrierLabel,
    String trackingCode,
    String estimatedDelivery
) {
    public static OrderFulfilmentView entityToDto(SalesOrder order) {
        ShippingCarrier carrier = order.getShippingCarrier();
        String carrierLabel = null;
        if (carrier != null) {
            carrierLabel = carrier.label();
        }
        return new OrderFulfilmentView(
            VietnamTime.dateTime(order.getCreatedAt()),
            VietnamTime.dateTime(order.getProcessingAt()),
            VietnamTime.dateTime(order.getShippingAt()),
            VietnamTime.dateTime(order.getDeliveredAt()),
            VietnamTime.dateTime(order.getCompletedAt()),
            VietnamTime.dateTime(order.getCustomerConfirmedAt()),
            VietnamTime.dateTime(order.getCancelledAt()),
            carrierLabel,
            order.getTrackingCode(),
            VietnamTime.date(order.getEstimatedDeliveryDate())
        );
    }

    public boolean hasShipment() {
        return carrierLabel != null;
    }
}
