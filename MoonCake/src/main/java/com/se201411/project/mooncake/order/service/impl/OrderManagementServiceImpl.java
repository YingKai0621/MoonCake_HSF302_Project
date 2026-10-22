package com.se201411.project.mooncake.order.service.impl;

import com.se201411.project.mooncake.common.exception.BusinessRuleViolationException;
import com.se201411.project.mooncake.entity.OrderItem;
import com.se201411.project.mooncake.entity.OrderItemComponent;
import com.se201411.project.mooncake.entity.Payment;
import com.se201411.project.mooncake.entity.SalesOrder;
import com.se201411.project.mooncake.enums.OrderStatus;
import com.se201411.project.mooncake.order.dto.AdminOrderView;
import com.se201411.project.mooncake.order.repository.OrderAdminRepository;
import com.se201411.project.mooncake.order.repository.OrderItemComponentRepository;
import com.se201411.project.mooncake.order.repository.OrderItemRepository;
import com.se201411.project.mooncake.order.repository.PaymentRepository;
import com.se201411.project.mooncake.order.service.OrderManagementService;
import com.se201411.project.mooncake.security.service.AdminAccessService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class OrderManagementServiceImpl implements OrderManagementService {
    private final OrderAdminRepository orders;
    private final OrderItemRepository items;
    private final OrderItemComponentRepository components;
    private final PaymentRepository payments;
    private final AdminAccessService access;
    private final Clock clock;

    public OrderManagementServiceImpl(OrderAdminRepository orders, OrderItemRepository items,
                                      OrderItemComponentRepository components, PaymentRepository payments,
                                      AdminAccessService access, Clock clock) {
        this.orders = orders;
        this.items = items;
        this.components = components;
        this.payments = payments;
        this.access = access;
        this.clock = clock;
    }

    public List<AdminOrderView> orders(OrderStatus status, int page) {
        access.requireAdmin();
        int safePage = Math.max(0, Math.min(page, 100000));
        List<SalesOrder> orderPage = orders.orders(status, safePage);
        List<AdminOrderView> result = new ArrayList<>();
        for (SalesOrder order : orderPage) {
            result.add(view(order, List.of(), List.of()));
        }
        return List.copyOf(result);
    }

    public AdminOrderView detail(long id) {
        access.requireAdmin();
        SalesOrder order = orders.require(id);
        List<AdminOrderView.Item> lines = new ArrayList<>();
        List<OrderItem> orderItems = items.findByOrderId(id);
        Map<Long, List<OrderItemComponent>> componentMap = loadComponents(orderItems);
        for (OrderItem item : orderItems) {
            List<AdminOrderView.Component> parts = new ArrayList<>();
            List<OrderItemComponent> itemComponents = componentMap.getOrDefault(
                item.getId(),
                List.of()
            );
            for (OrderItemComponent component : itemComponents) {
                parts.add(new AdminOrderView.Component(
                    component.getComponentNameSnapshot(),
                    component.getQuantityPerSet(),
                    component.getComponentRole()
                ));
            }
            lines.add(new AdminOrderView.Item(
                item.getProductNameSnapshot(),
                item.getSkuSnapshot(),
                item.getQuantity(),
                item.getUnitFinalPriceSnapshot(),
                item.getLineTotal(),
                List.copyOf(parts)
            ));
        }
        List<AdminOrderView.Cod> codPayments = new ArrayList<>();
        for (Payment payment : payments.findByOrderId(id)) {
            codPayments.add(new AdminOrderView.Cod(
                payment.getStatus(),
                payment.getAmount(),
                payment.getPaidAt()
            ));
        }
        return view(order, List.copyOf(lines), List.copyOf(codPayments));
    }

    @Transactional
    public void transition(long id, OrderStatus target) {
        access.requireAdmin();
        SalesOrder order = orders.require(id);
        OrderStatus source = order.getStatus();
        if (!allowed(source, target)) {
            throw new BusinessRuleViolationException("Không thể chuyển trạng thái " + source + " → " + target);
        }
        LocalDateTime now = LocalDateTime.now(clock);
        if (orders.transition(id, source, target, now) != 1) {
            throw new BusinessRuleViolationException("Đơn vừa được xử lý bởi yêu cầu khác. Hãy tải lại.");
        }
        if (target == OrderStatus.COMPLETED && orders.collectCod(id, now) != 1) {
            throw new BusinessRuleViolationException("Không thể xác nhận COD. Đơn phải có đúng một thanh toán COD chưa thu, đúng số tiền.");
        }
    }

    public static boolean allowed(OrderStatus from, OrderStatus to) {
        return (from == OrderStatus.PLACED && to == OrderStatus.PROCESSING)
                || (from == OrderStatus.PROCESSING && to == OrderStatus.SHIPPING)
                || (from == OrderStatus.SHIPPING && to == OrderStatus.COMPLETED);
    }

    private Map<Long, List<OrderItemComponent>> loadComponents(List<OrderItem> orderItems) {
        Map<Long, List<OrderItemComponent>> result = new HashMap<>();
        if (orderItems.isEmpty()) {
            return result;
        }
        List<Long> ids = new ArrayList<>();
        for (OrderItem item : orderItems) {
            ids.add(item.getId());
        }
        for (OrderItemComponent component : components.findByOrderItemIds(ids)) {
            long itemId = component.getOrderItem().getId();
            List<OrderItemComponent> itemComponents = result.get(itemId);
            if (itemComponents == null) {
                itemComponents = new ArrayList<>();
                result.put(itemId, itemComponents);
            }
            itemComponents.add(component);
        }
        return result;
    }

    private AdminOrderView view(
        SalesOrder order,
        List<AdminOrderView.Item> lines,
        List<AdminOrderView.Cod> codPayments
    ) {
        return new AdminOrderView(
            order.getId(),
            order.getOrderCode(),
            order.getStatus(),
            order.getRecipientName(),
            order.getRecipientPhone(),
            order.getShippingAddressSnapshot(),
            order.getOrderNote(),
            order.getSubtotalAmount(),
            order.getDiscountAmount(),
            order.getShippingFee(),
            order.getTotalAmount(),
            order.getCreatedAt(),
            lines,
            codPayments
        );
    }
}
