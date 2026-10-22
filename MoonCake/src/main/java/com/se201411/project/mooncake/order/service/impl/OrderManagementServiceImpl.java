package com.se201411.project.mooncake.order.service.impl;

import com.se201411.project.mooncake.order.service.OrderManagementService;
import com.se201411.project.mooncake.order.dto.AdminOrderView;
import com.se201411.project.mooncake.order.repository.*;
import com.se201411.project.mooncake.entity.*;
import com.se201411.project.mooncake.enums.*;
import com.se201411.project.mooncake.inventory.service.InventoryService;
import com.se201411.project.mooncake.inventory.dto.*;
import com.se201411.project.mooncake.security.service.AdminAccessService;
import com.se201411.project.mooncake.common.exception.BusinessRuleViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class OrderManagementServiceImpl implements OrderManagementService {
    private final OrderAdminRepository orders;
    private final OrderItemRepository items;
    private final OrderItemComponentRepository components;
    private final PaymentRepository payments;
    private final InventoryService inventory;
    private final AdminAccessService access;
    private final Clock clock;

    public OrderManagementServiceImpl(OrderAdminRepository orders, OrderItemRepository items,
                                      OrderItemComponentRepository components, PaymentRepository payments, InventoryService inventory,
                                      AdminAccessService access, Clock clock) {
        this.orders = orders;
        this.items = items;
        this.components = components;
        this.payments = payments;
        this.inventory = inventory;
        this.access = access;
        this.clock = clock;
    }

    public List<AdminOrderView> orders(OrderStatus status, int page) {
        access.requireAdmin();
        var result = new ArrayList<AdminOrderView>();
        for (var o : orders.orders(status, Math.max(0, Math.min(page, 100000))))
            result.add(view(o, List.of(), List.of()));
        return List.copyOf(result);
    }

    public AdminOrderView detail(long id) {
        access.requireAdmin();
        var o = orders.require(id);
        var lines = new ArrayList<AdminOrderView.Item>();
        var orderItems = items.findByOrderId(id);
        var componentMap = loadComponents(orderItems);
        for (var item : orderItems) {
            var parts = new ArrayList<AdminOrderView.Component>();
            for (var c : componentMap.getOrDefault(item.getId(), List.of()))
                parts.add(new AdminOrderView.Component(c.getComponentNameSnapshot(), c.getQuantityPerSet(), c.getComponentRole()));
            lines.add(new AdminOrderView.Item(item.getProductNameSnapshot(), item.getSkuSnapshot(), item.getQuantity(), item.getUnitFinalPriceSnapshot(), item.getLineTotal(), List.copyOf(parts)));
        }
        var cod = new ArrayList<AdminOrderView.Cod>();
        for (var p : payments.findByOrderId(id))
            cod.add(new AdminOrderView.Cod(p.getStatus(), p.getAmount(), p.getPaidAt()));
        return view(o, List.copyOf(lines), List.copyOf(cod));
    }

    @Transactional
    public void transition(long id, OrderStatus target) {
        access.requireAdmin();
        var order = orders.require(id);
        var source = order.getStatus();
        if (!allowed(source, target))
            throw new BusinessRuleViolationException("Không thể chuyển trạng thái " + source + " → " + target);
        var now = LocalDateTime.now(clock);
        if (orders.transition(id, source, target, now) != 1)
            throw new BusinessRuleViolationException("Đơn vừa được xử lý bởi yêu cầu khác. Hãy tải lại.");
        if (target == OrderStatus.CANCELLED)
            inventory.restoreForCancellation(new CancellationInventoryCommand(id, "CANCEL_RETURN:" + id, restoration(id)));
        if (target == OrderStatus.COMPLETED && orders.collectCod(id, now) != 1)
            throw new BusinessRuleViolationException("Không thể xác nhận COD. Đơn phải có đúng một thanh toán COD chưa thu, đúng số tiền.");
    }

    public static boolean allowed(OrderStatus from, OrderStatus to) {
        return (from == OrderStatus.PLACED && (to == OrderStatus.PROCESSING || to == OrderStatus.CANCELLED))
                || (from == OrderStatus.PROCESSING && (to == OrderStatus.SHIPPING || to == OrderStatus.CANCELLED))
                || (from == OrderStatus.SHIPPING && to == OrderStatus.COMPLETED);
    }

    private List<InventoryDemand> restoration(long id) {
        var quantities = new TreeMap<Long, Integer>();
        var orderItems = items.findByOrderId(id);
        var componentMap = loadComponents(orderItems);
        for (var item : orderItems) {
            if (item.getInventorySourceSnapshot() == InventorySource.STOCKED_SKU)
                quantities.merge(item.getVariant().getId(), item.getQuantity(), Math::addExact);
            else {
                var parts = componentMap.getOrDefault(item.getId(), List.of());
                if (parts.isEmpty())
                    throw new BusinessRuleViolationException("Đơn thiếu snapshot thành phần; không thể hoàn kho.");
                for (var c : parts)
                    quantities.merge(c.getComponentVariant().getId(), Math.multiplyExact(c.getQuantityPerSet(), item.getQuantity()), Math::addExact);
            }
        }
        if (quantities.isEmpty()) throw new BusinessRuleViolationException("Đơn thiếu sản phẩm; không thể hoàn kho.");
        var demands = new ArrayList<InventoryDemand>();
        for (var e : quantities.entrySet()) demands.add(new InventoryDemand(e.getKey(), e.getValue()));
        return List.copyOf(demands);
    }

    private Map<Long, List<OrderItemComponent>> loadComponents(List<OrderItem> orderItems) {
        var result = new HashMap<Long, List<OrderItemComponent>>();
        if (orderItems.isEmpty()) return result;
        var ids = new ArrayList<Long>();
        for (var item : orderItems) ids.add(item.getId());
        for (var component : components.findByOrderItemIds(ids)) {
            long itemId = component.getOrderItem().getId();
            result.computeIfAbsent(itemId, ignored -> new ArrayList<>()).add(component);
        }
        return result;
    }

    private AdminOrderView view(SalesOrder o, List<AdminOrderView.Item> lines, List<AdminOrderView.Cod> cod) {
        return new AdminOrderView(o.getId(), o.getOrderCode(), o.getStatus(), o.getRecipientName(), o.getRecipientPhone(), o.getShippingAddressSnapshot(), o.getOrderNote(), o.getSubtotalAmount(), o.getDiscountAmount(), o.getShippingFee(), o.getTotalAmount(), o.getCreatedAt(), lines, cod);
    }
}
