package com.se201411.project.mooncake.order.service.impl;

import com.se201411.project.mooncake.common.exception.BusinessRuleViolationException;
import com.se201411.project.mooncake.common.exception.ResourceNotFoundException;
import com.se201411.project.mooncake.entity.OrderItem;
import com.se201411.project.mooncake.entity.OrderItemComponent;
import com.se201411.project.mooncake.entity.Payment;
import com.se201411.project.mooncake.entity.SalesOrder;
import com.se201411.project.mooncake.enums.ComponentRole;
import com.se201411.project.mooncake.enums.OrderStatus;
import com.se201411.project.mooncake.enums.PaymentMethod;
import com.se201411.project.mooncake.enums.PaymentStatus;
import com.se201411.project.mooncake.order.dto.CustomerOrderComponentView;
import com.se201411.project.mooncake.order.dto.CustomerOrderDetailView;
import com.se201411.project.mooncake.order.dto.CustomerOrderItemView;
import com.se201411.project.mooncake.order.dto.CustomerOrderSummaryView;
import com.se201411.project.mooncake.order.repository.OrderItemComponentRepository;
import com.se201411.project.mooncake.order.repository.OrderItemRepository;
import com.se201411.project.mooncake.order.repository.PaymentRepository;
import com.se201411.project.mooncake.order.repository.SalesOrderRepository;
import com.se201411.project.mooncake.order.service.CustomerOrderService;
import com.se201411.project.mooncake.security.dto.ShopperIdentity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class CustomerOrderServiceImpl implements CustomerOrderService {

    private static final Locale VIETNAMESE = Locale.forLanguageTag("vi-VN");
    private static final ZoneId VIETNAM_TIME_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter DATE_TIME_FORMAT =
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final SalesOrderRepository orderRepository;
    private final OrderItemRepository itemRepository;
    private final OrderItemComponentRepository componentRepository;
    private final PaymentRepository paymentRepository;

    public CustomerOrderServiceImpl(
        SalesOrderRepository orderRepository,
        OrderItemRepository itemRepository,
        OrderItemComponentRepository componentRepository,
        PaymentRepository paymentRepository
    ) {
        this.orderRepository = orderRepository;
        this.itemRepository = itemRepository;
        this.componentRepository = componentRepository;
        this.paymentRepository = paymentRepository;
    }

    @Override
    public List<CustomerOrderSummaryView> findOrders(
        ShopperIdentity customerIdentity
    ) {
        requireIdentity(customerIdentity);
        List<SalesOrder> orders = orderRepository.findAllOwnedByCustomerId(
            customerIdentity.customerId()
        );

        List<CustomerOrderSummaryView> views = new ArrayList<>();
        for (SalesOrder order : orders) {
            views.add(new CustomerOrderSummaryView(
                order.getOrderCode(),
                orderStatusLabel(order.getStatus()),
                formatDateTime(order.getCreatedAt()),
                formatVnd(order.getTotalAmount())
            ));
        }
        return List.copyOf(views);
    }

    @Override
    public CustomerOrderDetailView requireOrder(
        String orderCode,
        ShopperIdentity customerIdentity
    ) {
        requireIdentity(customerIdentity);
        if (orderCode == null || orderCode.isBlank()) {
            throw new ResourceNotFoundException("Không tìm thấy đơn hàng của bạn");
        }

        Optional<SalesOrder> result = orderRepository.findOwnedByOrderCode(
            orderCode.trim(),
            customerIdentity.customerId()
        );
        if (result.isEmpty()) {
            throw new ResourceNotFoundException("Không tìm thấy đơn hàng của bạn");
        }

        SalesOrder order = result.get();
        Payment payment = requirePayment(order.getId());
        List<OrderItem> items = itemRepository.findByOrderId(order.getId());
        Map<Long, List<CustomerOrderComponentView>> components = loadComponents(items);

        List<CustomerOrderItemView> itemViews = new ArrayList<>();
        int totalQuantity = 0;
        for (OrderItem item : items) {
            totalQuantity = Math.addExact(totalQuantity, item.getQuantity());
            String sku = item.getSkuSnapshot();
            if (sku == null) {
                sku = item.getBoxSkuSnapshot();
            }

            itemViews.add(new CustomerOrderItemView(
                item.getProductNameSnapshot(),
                sku,
                item.getQuantity(),
                discountLabel(item.getDiscountPercentSnapshot()),
                formatVnd(item.getUnitFinalPriceSnapshot()),
                formatVnd(item.getLineTotal()),
                components.getOrDefault(item.getId(), List.of())
            ));
        }

        return new CustomerOrderDetailView(
            order.getOrderCode(),
            orderStatusLabel(order.getStatus()),
            order.getStatus() == OrderStatus.PLACED,
            formatDateTime(order.getCreatedAt()),
            order.getRecipientName(),
            order.getRecipientPhone(),
            order.getShippingAddressSnapshot(),
            order.getOrderNote(),
            paymentStatusLabel(payment.getStatus()),
            itemViews,
            totalQuantity,
            formatVnd(order.getSubtotalAmount()),
            formatVnd(order.getDiscountAmount()),
            formatVnd(order.getShippingFee()),
            formatVnd(order.getTotalAmount())
        );
    }

    private Payment requirePayment(long orderId) {
        List<Payment> payments = paymentRepository.findByOrderId(orderId);
        if (payments.size() != 1 || payments.get(0).getMethod() != PaymentMethod.COD) {
            throw new BusinessRuleViolationException(
                "Dữ liệu thanh toán của đơn hàng không hợp lệ"
            );
        }
        return payments.get(0);
    }

    private Map<Long, List<CustomerOrderComponentView>> loadComponents(
        List<OrderItem> items
    ) {
        Map<Long, List<CustomerOrderComponentView>> result = new LinkedHashMap<>();
        if (items.isEmpty()) {
            return result;
        }

        List<Long> itemIds = new ArrayList<>();
        for (OrderItem item : items) {
            itemIds.add(item.getId());
        }

        List<OrderItemComponent> components = componentRepository.findByOrderItemIds(itemIds);
        for (OrderItemComponent component : components) {
            long itemId = component.getOrderItem().getId();
            List<CustomerOrderComponentView> itemComponents = result.get(itemId);
            if (itemComponents == null) {
                itemComponents = new ArrayList<>();
                result.put(itemId, itemComponents);
            }
            itemComponents.add(new CustomerOrderComponentView(
                componentRoleLabel(component.getComponentRole()),
                component.getComponentNameSnapshot(),
                component.getQuantityPerSet()
            ));
        }
        return result;
    }

    private void requireIdentity(ShopperIdentity customerIdentity) {
        if (customerIdentity == null) {
            throw new ResourceNotFoundException("Không tìm thấy khách hàng hiện tại");
        }
    }

    private String orderStatusLabel(OrderStatus status) {
        if (status == OrderStatus.PLACED) {
            return "Đã tiếp nhận";
        }
        if (status == OrderStatus.PROCESSING) {
            return "Đang xử lý";
        }
        if (status == OrderStatus.SHIPPING) {
            return "Đang giao hàng";
        }
        if (status == OrderStatus.COMPLETED) {
            return "Hoàn thành";
        }
        return "Đã hủy";
    }

    private String paymentStatusLabel(PaymentStatus status) {
        if (status == PaymentStatus.PAID) {
            return "Đã thanh toán";
        }
        return "Chưa thanh toán";
    }

    private String componentRoleLabel(ComponentRole role) {
        if (role == ComponentRole.BOX) {
            return "Vỏ hộp";
        }
        return "Bánh";
    }

    private String discountLabel(BigDecimal discountPercent) {
        if (discountPercent == null || discountPercent.signum() == 0) {
            return null;
        }
        return "Giảm " + discountPercent.stripTrailingZeros().toPlainString() + "%";
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime
            .atZone(ZoneOffset.UTC)
            .withZoneSameInstant(VIETNAM_TIME_ZONE)
            .format(DATE_TIME_FORMAT);
    }

    private String formatVnd(BigDecimal amount) {
        return NumberFormat.getNumberInstance(VIETNAMESE).format(amount) + "đ";
    }
}
