package com.se201411.project.mooncake.order.service;

import com.se201411.project.mooncake.common.exception.BusinessRuleViolationException;
import com.se201411.project.mooncake.common.exception.ConcurrentOperationException;
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
import com.se201411.project.mooncake.order.dto.DeliveryAddressParts;
import com.se201411.project.mooncake.order.dto.OrderDeliveryUpdateForm;
import com.se201411.project.mooncake.order.repository.OrderItemComponentRepository;
import com.se201411.project.mooncake.order.repository.OrderItemRepository;
import com.se201411.project.mooncake.order.repository.PaymentRepository;
import com.se201411.project.mooncake.order.repository.SalesOrderRepository;
import com.se201411.project.mooncake.order.service.impl.CodPaymentProcessor;
import com.se201411.project.mooncake.order.service.impl.CustomerOrderServiceImpl;
import com.se201411.project.mooncake.order.service.impl.PaymentProcessorRegistryImpl;
import com.se201411.project.mooncake.security.dto.ShopperIdentity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

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
    @Mock
    private DeliveryAddressService deliveryAddressService;

    private CustomerOrderService customerOrderService;

    @BeforeEach
    void setUp() {
        customerOrderService = new CustomerOrderServiceImpl(
            orderRepository,
            itemRepository,
            componentRepository,
            paymentRepository,
            deliveryAddressService,
            new PaymentProcessorRegistryImpl(List.of(new CodPaymentProcessor(paymentRepository))),
            Clock.fixed(Instant.parse("2026-10-21T08:00:00Z"), ZoneOffset.UTC)
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
        assertThat(view.deliveryEditable()).isTrue();
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
        assertThat(view.deliveryEditable()).isFalse();
    }

    @Test
    void unpaidVnpayOrderInListShowsAwaitingPayment() {
        SalesOrder waiting = order(3L, "MC-WAIT", OrderStatus.PLACED, LocalDateTime.of(2026, 11, 8, 13, 0), 135_000);
        SalesOrder cod = order(4L, "MC-COD", OrderStatus.PLACED, LocalDateTime.of(2026, 11, 8, 12, 0), 620_000);
        Payment vnpayPayment = new Payment();
        vnpayPayment.setOrder(waiting);
        vnpayPayment.setMethod(PaymentMethod.VNPAY);
        vnpayPayment.setStatus(PaymentStatus.UNPAID);
        Payment codPayment = new Payment();
        codPayment.setOrder(cod);
        codPayment.setMethod(PaymentMethod.COD);
        codPayment.setStatus(PaymentStatus.UNPAID);
        when(orderRepository.findAllOwnedByCustomerId(7L)).thenReturn(List.of(waiting, cod));
        when(paymentRepository.findByOrderIds(List.of(3L, 4L))).thenReturn(List.of(vnpayPayment, codPayment));

        List<CustomerOrderSummaryView> views = customerOrderService.findOrders(CUSTOMER);

        assertThat(views.get(0).statusLabel()).isEqualTo("Chờ thanh toán");
        assertThat(views.get(0).awaitingPayment()).isTrue();
        assertThat(views.get(1).statusLabel()).isEqualTo("Đã tiếp nhận");
        assertThat(views.get(1).awaitingPayment()).isFalse();
    }

    @Test
    void unpaidVnpayOrderShowsOnlinePaymentActions() {
        CustomerOrderDetailView view = placedOrderView(
            PaymentMethod.VNPAY,
            PaymentStatus.UNPAID
        );

        assertThat(view.statusLabel()).isEqualTo("Chờ thanh toán");
        assertThat(view.paymentMethodLabel()).isEqualTo("VNPay");
        assertThat(view.awaitingOnlinePayment()).isTrue();
        assertThat(view.cancellable()).isTrue();
        assertThat(view.deliveryEditable()).isTrue();
    }

    @Test
    void paidVnpayOrderCannotBeCancelledByCustomerButDeliveryIsEditable() {
        CustomerOrderDetailView view = placedOrderView(
            PaymentMethod.VNPAY,
            PaymentStatus.PAID
        );

        assertThat(view.awaitingOnlinePayment()).isFalse();
        assertThat(view.cancellable()).isFalse();
        assertThat(view.deliveryEditable()).isTrue();
        assertThat(view.paymentStatusLabel()).isEqualTo("Đã thanh toán");
    }

    @Test
    void codOrderNeverShowsOnlinePaymentActions() {
        CustomerOrderDetailView view = placedOrderView(
            PaymentMethod.COD,
            PaymentStatus.UNPAID
        );

        assertThat(view.paymentMethodLabel()).isEqualTo("COD");
        assertThat(view.awaitingOnlinePayment()).isFalse();
        assertThat(view.cancellable()).isTrue();
    }

    @Test
    void codOrderCannotStartOnlinePayment() {
        placedOrderView(PaymentMethod.COD, PaymentStatus.UNPAID);
        SalesOrder order = orderRepository.findOwnedByOrderCode("MC-PLACED", 7L).get();
        when(orderRepository.findByIdForUpdate(12L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> customerOrderService.startOnlinePayment("MC-PLACED", CUSTOMER, "127.0.0.1"))
            .isInstanceOf(BusinessRuleViolationException.class);
    }

    private CustomerOrderDetailView placedOrderView(
        PaymentMethod method,
        PaymentStatus status
    ) {
        SalesOrder order = order(
            12L,
            "MC-PLACED",
            OrderStatus.PLACED,
            LocalDateTime.of(2026, 11, 8, 4, 0),
            620_000
        );
        order.setSubtotalAmount(BigDecimal.valueOf(590_000));
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setShippingFee(BigDecimal.valueOf(30_000));

        Payment payment = new Payment();
        payment.setMethod(method);
        payment.setStatus(status);

        when(orderRepository.findOwnedByOrderCode("MC-PLACED", 7L))
            .thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderId(12L)).thenReturn(List.of(payment));
        when(itemRepository.findByOrderId(12L)).thenReturn(List.of());

        return customerOrderService.requireOrder("MC-PLACED", CUSTOMER);
    }

    @Test
    void preparesCurrentDeliveryInformationForEditing() {
        SalesOrder order = order(
            11L,
            "MC-EDIT",
            OrderStatus.PLACED,
            LocalDateTime.of(2026, 10, 21, 4, 0),
            620_000
        );
        order.setRecipientName("Người nhận");
        order.setRecipientPhone("0901234567");
        order.setShippingAddressSnapshot("12 Lê Lợi, Phường Bến Nghé, Hồ Chí Minh");
        order.setOrderNote("Gọi trước khi giao");

        when(orderRepository.findOwnedByOrderCode("MC-EDIT", 7L))
            .thenReturn(Optional.of(order));
        when(deliveryAddressService.parseShippingAddress(
            "12 Lê Lợi, Phường Bến Nghé, Hồ Chí Minh"
        )).thenReturn(Optional.of(new DeliveryAddressParts(
            "12 Lê Lợi",
            "ward-1",
            "province-1"
        )));

        OrderDeliveryUpdateForm form = customerOrderService
            .requireDeliveryUpdateForm("MC-EDIT", CUSTOMER);

        assertThat(form.getRecipientName()).isEqualTo("Người nhận");
        assertThat(form.getRecipientPhone()).isEqualTo("0901234567");
        assertThat(form.getStreetAddress()).isEqualTo("12 Lê Lợi");
        assertThat(form.getWardCode()).isEqualTo("ward-1");
        assertThat(form.getProvinceCode()).isEqualTo("province-1");
        assertThat(form.getCurrentShippingAddress())
            .isEqualTo("12 Lê Lợi, Phường Bến Nghé, Hồ Chí Minh");
    }

    @Test
    void updatesDeliveryInformationOnlyWhileOrderIsPlaced() {
        SalesOrder order = order(
            12L,
            "MC-UPDATE",
            OrderStatus.PLACED,
            LocalDateTime.of(2026, 10, 21, 4, 0),
            620_000
        );
        OrderDeliveryUpdateForm form = deliveryForm();

        when(orderRepository.findOwnedByOrderCode("MC-UPDATE", 7L))
            .thenReturn(Optional.of(order));
        when(deliveryAddressService.buildShippingAddress(
            "25 Nguyễn Huệ",
            "ward-2",
            "province-2"
        )).thenReturn("25 Nguyễn Huệ, Phường Trung Tâm, Đà Nẵng");
        when(orderRepository.updateDeliveryIfStatus(
            12L,
            7L,
            OrderStatus.PLACED,
            "Người nhận mới",
            "0912345678",
            "25 Nguyễn Huệ, Phường Trung Tâm, Đà Nẵng",
            "Giao buổi sáng",
            LocalDateTime.of(2026, 10, 21, 8, 0)
        )).thenReturn(1);

        customerOrderService.updateDeliveryInformation("MC-UPDATE", form, CUSTOMER);

        verify(orderRepository).updateDeliveryIfStatus(
            12L,
            7L,
            OrderStatus.PLACED,
            "Người nhận mới",
            "0912345678",
            "25 Nguyễn Huệ, Phường Trung Tâm, Đà Nẵng",
            "Giao buổi sáng",
            LocalDateTime.of(2026, 10, 21, 8, 0)
        );
    }

    @Test
    void rejectsDeliveryUpdateAfterProcessingStarts() {
        SalesOrder order = order(
            13L,
            "MC-PROCESSING",
            OrderStatus.PROCESSING,
            LocalDateTime.of(2026, 10, 21, 4, 0),
            620_000
        );
        when(orderRepository.findOwnedByOrderCode("MC-PROCESSING", 7L))
            .thenReturn(Optional.of(order));

        assertThatThrownBy(() -> customerOrderService.updateDeliveryInformation(
            "MC-PROCESSING",
            deliveryForm(),
            CUSTOMER
        ))
            .isInstanceOf(BusinessRuleViolationException.class)
            .hasMessageContaining("chưa bắt đầu xử lý");
    }

    @Test
    void reportsConflictWhenOrderStartsProcessingDuringDeliveryUpdate() {
        SalesOrder order = order(
            14L,
            "MC-RACE",
            OrderStatus.PLACED,
            LocalDateTime.of(2026, 10, 21, 4, 0),
            620_000
        );
        OrderDeliveryUpdateForm form = deliveryForm();
        when(orderRepository.findOwnedByOrderCode("MC-RACE", 7L))
            .thenReturn(Optional.of(order));
        when(deliveryAddressService.buildShippingAddress(
            "25 Nguyễn Huệ",
            "ward-2",
            "province-2"
        )).thenReturn("25 Nguyễn Huệ, Phường Trung Tâm, Đà Nẵng");
        when(orderRepository.updateDeliveryIfStatus(
            14L,
            7L,
            OrderStatus.PLACED,
            "Người nhận mới",
            "0912345678",
            "25 Nguyễn Huệ, Phường Trung Tâm, Đà Nẵng",
            "Giao buổi sáng",
            LocalDateTime.of(2026, 10, 21, 8, 0)
        )).thenReturn(0);

        assertThatThrownBy(() -> customerOrderService.updateDeliveryInformation(
            "MC-RACE",
            form,
            CUSTOMER
        ))
            .isInstanceOf(ConcurrentOperationException.class)
            .hasMessageContaining("chuyển sang xử lý");
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

    private OrderDeliveryUpdateForm deliveryForm() {
        OrderDeliveryUpdateForm form = new OrderDeliveryUpdateForm();
        form.setRecipientName(" Người nhận mới ");
        form.setRecipientPhone("0912345678");
        form.setProvinceCode("province-2");
        form.setWardCode("ward-2");
        form.setStreetAddress("25 Nguyễn Huệ");
        form.setOrderNote(" Giao buổi sáng ");
        return form;
    }
}
