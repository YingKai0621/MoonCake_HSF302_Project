package com.se201411.project.mooncake.order.service.impl;

import com.se201411.project.mooncake.common.exception.BusinessRuleViolationException;
import com.se201411.project.mooncake.common.exception.ResourceNotFoundException;
import com.se201411.project.mooncake.entity.OrderItem;
import com.se201411.project.mooncake.entity.OrderItemComponent;
import com.se201411.project.mooncake.entity.SalesOrder;
import com.se201411.project.mooncake.enums.InventorySource;
import com.se201411.project.mooncake.enums.OrderStatus;
import com.se201411.project.mooncake.inventory.dto.CancellationInventoryCommand;
import com.se201411.project.mooncake.inventory.dto.InventoryDemand;
import com.se201411.project.mooncake.inventory.service.InventoryService;
import com.se201411.project.mooncake.order.repository.OrderItemComponentRepository;
import com.se201411.project.mooncake.order.repository.OrderItemRepository;
import com.se201411.project.mooncake.order.repository.SalesOrderRepository;
import com.se201411.project.mooncake.order.service.OrderCancellationService;
import com.se201411.project.mooncake.security.dto.ShopperIdentity;
import com.se201411.project.mooncake.security.service.AdminAccessService;
import com.se201411.project.mooncake.security.service.CurrentCustomerService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
public class OrderCancellationServiceImpl implements OrderCancellationService {

    private final SalesOrderRepository orderRepository;
    private final OrderItemRepository itemRepository;
    private final OrderItemComponentRepository componentRepository;
    private final InventoryService inventoryService;
    private final CurrentCustomerService currentCustomerService;
    private final AdminAccessService adminAccessService;
    private final Clock clock;

    public OrderCancellationServiceImpl(
        SalesOrderRepository orderRepository,
        OrderItemRepository itemRepository,
        OrderItemComponentRepository componentRepository,
        InventoryService inventoryService,
        CurrentCustomerService currentCustomerService,
        AdminAccessService adminAccessService,
        Clock clock
    ) {
        this.orderRepository = orderRepository;
        this.itemRepository = itemRepository;
        this.componentRepository = componentRepository;
        this.inventoryService = inventoryService;
        this.currentCustomerService = currentCustomerService;
        this.adminAccessService = adminAccessService;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void cancelCurrentCustomerOrder(String orderCode) {
        ShopperIdentity customer = currentCustomerService.requireCurrentCustomer();
        String normalizedOrderCode = normalizeOrderCode(orderCode);

        SalesOrder order = orderRepository.findOwnedByOrderCode(
            normalizedOrderCode,
            customer.customerId()
        ).orElseThrow(() -> new ResourceNotFoundException(
            "Không tìm thấy đơn hàng của bạn"
        ));

        if (order.getStatus() != OrderStatus.PLACED) {
            throw new BusinessRuleViolationException(
                "Khách hàng chỉ có thể tự hủy đơn đang ở trạng thái PLACED."
            );
        }

        cancelOrder(order, OrderStatus.PLACED);
    }

    @Override
    @Transactional
    public void cancelForAdmin(long orderId) {
        adminAccessService.requireAdmin();
        SalesOrder order = orderRepository.findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Không tìm thấy đơn hàng."
            ));

        OrderStatus currentStatus = order.getStatus();
        if (currentStatus != OrderStatus.PLACED
            && currentStatus != OrderStatus.PROCESSING) {
            throw new BusinessRuleViolationException(
                "Admin chỉ có thể hủy đơn PLACED hoặc PROCESSING."
            );
        }

        cancelOrder(order, currentStatus);
    }

    private void cancelOrder(SalesOrder order, OrderStatus expectedStatus) {
        LocalDateTime now = LocalDateTime.now(clock);
        int changedRows = orderRepository.cancelFromStatus(
            order.getId(),
            expectedStatus,
            OrderStatus.CANCELLED,
            now
        );
        if (changedRows != 1) {
            throw new BusinessRuleViolationException(
                "Đơn vừa được xử lý bởi yêu cầu khác. Hãy tải lại."
            );
        }

        List<InventoryDemand> demands = calculateRestoration(order.getId());
        CancellationInventoryCommand command = new CancellationInventoryCommand(
            order.getId(),
            "CANCEL_RETURN:" + order.getId(),
            demands
        );
        inventoryService.restoreForCancellation(command);
    }

    private List<InventoryDemand> calculateRestoration(long orderId) {
        Map<Long, Integer> quantities = new TreeMap<>();
        List<OrderItem> orderItems = itemRepository.findByOrderId(orderId);
        Map<Long, List<OrderItemComponent>> componentsByItem = loadComponents(orderItems);

        for (OrderItem item : orderItems) {
            if (item.getInventorySourceSnapshot() == InventorySource.STOCKED_SKU) {
                if (item.getVariant() == null) {
                    throw new BusinessRuleViolationException(
                        "Đơn thiếu SKU snapshot; không thể hoàn kho."
                    );
                }
                addQuantity(
                    quantities,
                    item.getVariant().getId(),
                    item.getQuantity()
                );
                continue;
            }

            List<OrderItemComponent> components = componentsByItem.get(item.getId());
            if (components == null || components.isEmpty()) {
                throw new BusinessRuleViolationException(
                    "Đơn thiếu snapshot thành phần; không thể hoàn kho."
                );
            }

            for (OrderItemComponent component : components) {
                int quantity = Math.multiplyExact(
                    component.getQuantityPerSet(),
                    item.getQuantity()
                );
                addQuantity(
                    quantities,
                    component.getComponentVariant().getId(),
                    quantity
                );
            }
        }

        if (quantities.isEmpty()) {
            throw new BusinessRuleViolationException(
                "Đơn thiếu sản phẩm; không thể hoàn kho."
            );
        }

        List<InventoryDemand> demands = new ArrayList<>();
        for (Map.Entry<Long, Integer> quantity : quantities.entrySet()) {
            demands.add(new InventoryDemand(quantity.getKey(), quantity.getValue()));
        }
        return List.copyOf(demands);
    }

    private Map<Long, List<OrderItemComponent>> loadComponents(
        List<OrderItem> orderItems
    ) {
        Map<Long, List<OrderItemComponent>> result = new HashMap<>();
        if (orderItems.isEmpty()) {
            return result;
        }

        List<Long> itemIds = new ArrayList<>();
        for (OrderItem item : orderItems) {
            itemIds.add(item.getId());
        }

        List<OrderItemComponent> components = componentRepository.findByOrderItemIds(
            itemIds
        );
        for (OrderItemComponent component : components) {
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

    private void addQuantity(
        Map<Long, Integer> quantities,
        long variantId,
        int quantity
    ) {
        Integer currentQuantity = quantities.get(variantId);
        if (currentQuantity == null) {
            quantities.put(variantId, quantity);
            return;
        }
        quantities.put(variantId, Math.addExact(currentQuantity, quantity));
    }

    private String normalizeOrderCode(String orderCode) {
        if (orderCode == null || orderCode.isBlank()) {
            throw new ResourceNotFoundException("Không tìm thấy đơn hàng của bạn");
        }
        return orderCode.trim();
    }
}
