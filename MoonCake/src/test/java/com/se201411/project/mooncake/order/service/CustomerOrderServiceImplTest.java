package com.se201411.project.mooncake.order.service;

import com.se201411.project.mooncake.common.exception.ResourceNotFoundException;
import com.se201411.project.mooncake.entity.OrderItem;
import com.se201411.project.mooncake.entity.OrderItemComponent;
import com.se201411.project.mooncake.entity.Payment;
import com.se201411.project.mooncake.entity.SalesOrder;
import com.se201411.project.mooncake.enums.ComponentRole;
import com.se201411.project.mooncake.enums.OrderStatus;
import com.se201411.project.mooncake.enums.PaymentMethod;
import com.se201411.project.mooncake.enums.PaymentStatus;
import com.se201411.project.mooncake.order.dto.CustomerOrderDetailView;
import com.se201411.project.mooncake.order.dto.CustomerOrderSummaryView;
import com.se201411.project.mooncake.order.repository.OrderItemComponentRepository;
import com.se201411.project.mooncake.order.repository.OrderItemRepository;
import com.se201411.project.mooncake.order.repository.PaymentRepository;
import com.se201411.project.mooncake.order.repository.SalesOrderRepository;
import com.se201411.project.mooncake.order.service.impl.CustomerOrderServiceImpl;
import com.se201411.project.mooncake.security.dto.ShopperIdentity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerOrderServiceImplTest {

    private static final ShopperIdentity CUSTOMER = ShopperIdentity.forCustomer(7L);

    @Mock
    private SalesOrderRepository orderRepository;
    @Mock
    private OrderItemRepository itemRepository;
    @Mock
    private OrderItemComponentRepository componentRepository;
    @Mock
    private PaymentRepository paymentRepository;

    private CustomerOrderService customerOrderService;

    @BeforeEach
    void setUp() {
        customerOrderService = new CustomerOrderServiceImpl(
            orderRepository,
            itemRepository,
            componentRepository,
            paymentRepository
        );
    }

    @Test
    void listsOwnedOrdersWithVietnameseStatusAndLocalTime() {
        SalesOrder newest = order(
            2L,
            "MC-20261021-NEWEST",
            OrderStatus.PROCESSING,
            LocalDateTime.of(2026, 10, 21, 1, 30),
            620_000
        );
        SalesOrder older = order(
            1L,
            "MC-20261020-OLDER",
            OrderStatus.COMPLETED,
            LocalDateTime.of(2026, 10, 20, 2, 0),
            500_000
        );
        when(orderRepository.findAllOwnedByCustomerId(7L)).thenReturn(
            List.of(newest, older)
        );

        List<CustomerOrderSummaryView> views = customerOrderService.findOrders(CUSTOMER);

        assertThat(views).hasSize(2);
        assertThat(views.get(0).orderCode()).isEqualTo("MC-20261021-NEWEST");
        assertThat(views.get(0).statusLabel()).isEqualTo("Đang xử lý");
        assertThat(views.get(0).formattedCreatedAt()).isEqualTo("21/10/2026 08:30");
        assertThat(views.get(0).formattedTotal()).isEqualTo("620.000đ");
    }

    @Test
    void returnsOwnedOrderDetailWithCodAndComponentSnapshots() {
        SalesOrder order = order(
            9L,
            "MC-20261021-DETAIL",
            OrderStatus.PLACED,
            LocalDateTime.of(2026, 10, 21, 3, 0),
            620_000
        );
        order.setRecipientName("Người nhận");
        order.setRecipientPhone("0901234567");
        order.setShippingAddressSnapshot("123 Đường Trung Thu, Sài Gòn, Hồ Chí Minh");
        order.setSubtotalAmount(BigDecimal.valueOf(590_100));
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setShippingFee(BigDecimal.valueOf(30_000));
        order.setRoundingAdjustment(BigDecimal.valueOf(-100));

        Payment payment = new Payment();
        payment.setMethod(PaymentMethod.COD);
        payment.setStatus(PaymentStatus.UNPAID);

        OrderItem item = new OrderItem();
        ReflectionTestUtils.setField(item, "id", 11L);
        item.setProductNameSnapshot("Hộp quà Kim Phúc");
        item.setSkuSnapshot("GIFT-KIM-PHUC-4");
        item.setQuantity(1);
        item.setUnitBasePriceSnapshot(BigDecimal.valueOf(590_000));
        item.setDiscountPercentSnapshot(BigDecimal.ZERO);
        item.setUnitFinalPriceSnapshot(BigDecimal.valueOf(590_000));
        item.setLineTotal(BigDecimal.valueOf(590_000));

        OrderItemComponent component = new OrderItemComponent();
        component.setOrderItem(item);
        component.setComponentRole(ComponentRole.BOX);
        component.setComponentNameSnapshot("Vỏ hộp Kim Phúc");
        component.setQuantityPerSet(1);
        component.setUnitPriceSnapshot(BigDecimal.valueOf(90_000));

        when(orderRepository.findOwnedByOrderCode("MC-20261021-DETAIL", 7L))
            .thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderId(9L)).thenReturn(List.of(payment));
        when(itemRepository.findByOrderId(9L)).thenReturn(List.of(item));
        when(componentRepository.findByOrderItemIds(List.of(11L)))
            .thenReturn(List.of(component));

        CustomerOrderDetailView view = customerOrderService.requireOrder(
            "MC-20261021-DETAIL",
            CUSTOMER
        );

        assertThat(view.statusLabel()).isEqualTo("Đã tiếp nhận");
        assertThat(view.cancellable()).isTrue();
        assertThat(view.paymentStatusLabel()).isEqualTo("Chưa thanh toán");
        assertThat(view.totalQuantity()).isEqualTo(1);
        assertThat(view.formattedRoundingAdjustment()).isEqualTo("- 100đ");
        assertThat(view.items()).hasSize(1);
        assertThat(view.items().get(0).components()).hasSize(1);
        assertThat(view.items().get(0).components().get(0).roleLabel())
            .isEqualTo("Vỏ hộp");
    }

    @Test
    void doesNotAllowCustomerToCancelOrderAfterProcessingStarts() {
        SalesOrder order = order(
            10L,
            "MC-20261021-PROCESSING",
            OrderStatus.PROCESSING,
            LocalDateTime.of(2026, 10, 21, 4, 0),
            620_000
        );
        order.setRecipientName("Người nhận");
        order.setRecipientPhone("0901234567");
        order.setShippingAddressSnapshot("123 Đường Trung Thu, Hồ Chí Minh");
        order.setSubtotalAmount(BigDecimal.valueOf(590_000));
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setShippingFee(BigDecimal.valueOf(30_000));

        Payment payment = new Payment();
        payment.setMethod(PaymentMethod.COD);
        payment.setStatus(PaymentStatus.UNPAID);

        when(orderRepository.findOwnedByOrderCode("MC-20261021-PROCESSING", 7L))
            .thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderId(10L)).thenReturn(List.of(payment));
        when(itemRepository.findByOrderId(10L)).thenReturn(List.of());

        CustomerOrderDetailView view = customerOrderService.requireOrder(
            "MC-20261021-PROCESSING",
            CUSTOMER
        );

        assertThat(view.cancellable()).isFalse();
    }

    @Test
    void rejectsOrderThatDoesNotBelongToCurrentCustomer() {
        when(orderRepository.findOwnedByOrderCode("MC-OTHER", 7L))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerOrderService.requireOrder("MC-OTHER", CUSTOMER))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Không tìm thấy đơn hàng của bạn");
    }

    private SalesOrder order(
        long id,
        String orderCode,
        OrderStatus status,
        LocalDateTime createdAt,
        long total
    ) {
        SalesOrder order = new SalesOrder();
        ReflectionTestUtils.setField(order, "id", id);
        order.setOrderCode(orderCode);
        order.setStatus(status);
        order.setCreatedAt(createdAt);
        order.setTotalAmount(BigDecimal.valueOf(total));
        return order;
    }
}
