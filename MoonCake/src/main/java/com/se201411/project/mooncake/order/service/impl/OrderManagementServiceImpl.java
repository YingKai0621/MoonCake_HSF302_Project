package com.se201411.project.mooncake.order.service.impl;

import com.se201411.project.mooncake.common.exception.BusinessRuleViolationException;
import com.se201411.project.mooncake.entity.OrderItem;
import com.se201411.project.mooncake.entity.OrderItemComponent;
import com.se201411.project.mooncake.entity.Payment;
import com.se201411.project.mooncake.entity.SalesOrder;
import com.se201411.project.mooncake.enums.OrderStatus;
import com.se201411.project.mooncake.enums.PaymentStatus;
import com.se201411.project.mooncake.order.dto.AdminOrderTab;
import com.se201411.project.mooncake.order.dto.AdminOrderView;
import com.se201411.project.mooncake.order.repository.OrderAdminRepository;
import com.se201411.project.mooncake.order.repository.OrderItemComponentRepository;
import com.se201411.project.mooncake.order.repository.OrderItemRepository;
import com.se201411.project.mooncake.order.repository.PaymentRepository;
import com.se201411.project.mooncake.order.service.OrderManagementService;
import com.se201411.project.mooncake.order.service.PaymentProcessor;
import com.se201411.project.mooncake.order.service.PaymentProcessorRegistry;
import com.se201411.project.mooncake.security.service.AdminAccessService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class OrderManagementServiceImpl implements OrderManagementService {
    private static final ZoneId VIETNAM = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final OrderAdminRepository orders;
    private final OrderItemRepository items;
    private final OrderItemComponentRepository components;
    private final PaymentRepository payments;
    private final PaymentProcessorRegistry paymentProcessorRegistry;
    private final AdminAccessService access;
    private final Clock clock;

    public OrderManagementServiceImpl(OrderAdminRepository orders, OrderItemRepository items,
                                      OrderItemComponentRepository components, PaymentRepository payments,
                                      PaymentProcessorRegistry paymentProcessorRegistry,
                                      AdminAccessService access, Clock clock) {
        this.orders = orders;
        this.items = items;
        this.components = components;
        this.payments = payments;
        this.paymentProcessorRegistry = paymentProcessorRegistry;
        this.access = access;
        this.clock = clock;
    }

    public List<AdminOrderView> orders(OrderStatus status, int page) {
        access.requireAdmin();
        int safePage = Math.max(0, Math.min(page, 100000));
        List<SalesOrder> orderPage = orders.orders(status, safePage);
        Map<Long, List<Payment>> paymentsByOrder = loadPayments(orderPage);
        List<AdminOrderView> result = new ArrayList<>();
        for (SalesOrder order : orderPage) {
            List<Payment> orderPayments = paymentsByOrder.getOrDefault(order.getId(), List.of());
            result.add(view(order, List.of(), orderPayments));
        }
        return List.copyOf(result);
    }

    @Override
    public List<AdminOrderTab> statusTabs(OrderStatus selected) {
        access.requireAdmin();
        List<AdminOrderTab> tabs = new ArrayList<>();
        tabs.add(new AdminOrderTab(null, "Tất cả", orders.count(null), selected == null));
        for (OrderStatus status : OrderStatus.values()) {
            tabs.add(new AdminOrderTab(status, statusLabel(status), orders.count(status), status == selected));
        }
        return tabs;
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
                parts.add(AdminOrderView.Component.entityToDto(component));
            }
            lines.add(AdminOrderView.Item.entityToDto(item, parts));
        }
        return view(order, List.copyOf(lines), payments.findByOrderId(id));
    }

    @Transactional
    public void transition(long id, OrderStatus target) {
        access.requireAdmin();
        SalesOrder order = orders.requireForUpdate(id);
        OrderStatus source = order.getStatus();
        if (!allowed(source, target)) {
            throw new BusinessRuleViolationException("Không thể chuyển trạng thái " + source + " → " + target);
        }

        Payment payment = requireSinglePayment(id);
        PaymentProcessor paymentProcessor = paymentProcessorRegistry.forMethod(payment.getMethod());
        paymentProcessor.checkCanFulfil(payment);

        LocalDateTime now = LocalDateTime.now(clock);
        if (orders.transition(id, source, target, now) != 1) {
            throw new BusinessRuleViolationException("Đơn vừa được xử lý bởi yêu cầu khác. Hãy tải lại.");
        }
        if (target == OrderStatus.COMPLETED) {
            paymentProcessor.completeDelivery(id, payment, now);
        }
    }

    @Transactional
    public void refreshPaymentStatus(long id) {
        access.requireAdmin();
        orders.requireForUpdate(id);
        Payment payment = requireSinglePayment(id);
        PaymentProcessor paymentProcessor = paymentProcessorRegistry.forMethod(payment.getMethod());
        paymentProcessor.refreshPaymentStatus(id);
    }

    private Payment requireSinglePayment(long orderId) {
        List<Payment> orderPayments = payments.findByOrderId(orderId);
        if (orderPayments.size() != 1) {
            throw new BusinessRuleViolationException(
                "Đơn phải có đúng một bản ghi thanh toán. Không thể chuyển trạng thái."
            );
        }
        return orderPayments.get(0);
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

    private Map<Long, List<Payment>> loadPayments(List<SalesOrder> orderPage) {
        Map<Long, List<Payment>> result = new HashMap<>();
        if (orderPage.isEmpty()) {
            return result;
        }
        List<Long> ids = new ArrayList<>();
        for (SalesOrder order : orderPage) {
            ids.add(order.getId());
        }
        for (Payment payment : payments.findByOrderIds(ids)) {
            long orderId = payment.getOrder().getId();
            List<Payment> orderPayments = result.get(orderId);
            if (orderPayments == null) {
                orderPayments = new ArrayList<>();
                result.put(orderId, orderPayments);
            }
            orderPayments.add(payment);
        }
        return result;
    }

    private AdminOrderView view(SalesOrder order, List<AdminOrderView.Item> lines, List<Payment> orderPayments) {
        List<AdminOrderView.PaymentLine> paymentLines = new ArrayList<>();
        for (Payment payment : orderPayments) {
            PaymentProcessor processor = paymentProcessorRegistry.forMethod(payment.getMethod());
            String paidAt = null;
            if (payment.getPaidAt() != null) {
                paidAt = formatTime(payment.getPaidAt());
            }
            paymentLines.add(AdminOrderView.PaymentLine.entityToDto(payment, processor.displayName(), paidAt));
        }
        return new AdminOrderView(
            order.getId(),
            order.getOrderCode(),
            order.getStatus(),
            statusLabel(order.getStatus()),
            statusTone(order.getStatus()),
            order.getRecipientName(),
            order.getRecipientPhone(),
            order.getShippingAddressSnapshot(),
            order.getOrderNote(),
            order.getSubtotalAmount(),
            order.getDiscountAmount(),
            order.getShippingFee(),
            order.getRoundingAdjustment(),
            order.getTotalAmount(),
            formatTime(order.getCreatedAt()),
            order.getUpdatedAt() == null ? null : formatTime(order.getUpdatedAt()),
            lines,
            paymentLines,
            actions(order.getStatus(), orderPayments)
        );
    }

    private AdminOrderView.Actions actions(OrderStatus status, List<Payment> orderPayments) {
        if (orderPayments.size() != 1) {
            return new AdminOrderView.Actions(false, false, false, false, false, false, false);
        }
        Payment payment = orderPayments.get(0);
        PaymentProcessor processor = paymentProcessorRegistry.forMethod(payment.getMethod());
        boolean paid = payment.getStatus() == PaymentStatus.PAID;
        boolean placed = status == OrderStatus.PLACED;
        return new AdminOrderView.Actions(
            placed && processor.canFulfil(payment),
            status == OrderStatus.PROCESSING,
            status == OrderStatus.SHIPPING,
            placed || status == OrderStatus.PROCESSING,
            placed && !paid && processor.isOnline(),
            status == OrderStatus.CANCELLED && paid,
            paid && processor.isOnline()
        );
    }

    private String formatTime(LocalDateTime utcTime) {
        return utcTime.atOffset(ZoneOffset.UTC).atZoneSameInstant(VIETNAM).format(DATE_TIME);
    }

    private String statusLabel(OrderStatus status) {
        if (status == OrderStatus.PLACED) {
            return "Chờ xử lý";
        }
        if (status == OrderStatus.PROCESSING) {
            return "Đang chuẩn bị";
        }
        if (status == OrderStatus.SHIPPING) {
            return "Đang giao";
        }
        if (status == OrderStatus.COMPLETED) {
            return "Hoàn tất";
        }
        return "Đã hủy";
    }

    private String statusTone(OrderStatus status) {
        if (status == OrderStatus.PLACED) {
            return "warning";
        }
        if (status == OrderStatus.COMPLETED) {
            return "success";
        }
        if (status == OrderStatus.CANCELLED) {
            return "danger";
        }
        return "info";
    }
}
