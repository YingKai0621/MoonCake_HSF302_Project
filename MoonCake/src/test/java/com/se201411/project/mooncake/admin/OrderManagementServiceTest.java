package com.se201411.project.mooncake.admin;

import java.time.LocalDate;
import com.se201411.project.mooncake.order.dto.ShipmentForm;
import com.se201411.project.mooncake.enums.ShippingCarrier;
import com.se201411.project.mooncake.common.exception.BusinessRuleViolationException;
import com.se201411.project.mooncake.entity.Payment;
import com.se201411.project.mooncake.entity.SalesOrder;
import com.se201411.project.mooncake.enums.OrderStatus;
import com.se201411.project.mooncake.enums.PaymentMethod;
import com.se201411.project.mooncake.enums.PaymentStatus;
import com.se201411.project.mooncake.order.repository.OrderAdminRepository;
import com.se201411.project.mooncake.order.repository.OrderItemComponentRepository;
import com.se201411.project.mooncake.order.repository.OrderItemRepository;
import com.se201411.project.mooncake.order.repository.PaymentRepository;
import com.se201411.project.mooncake.order.repository.VnpayRepository;
import com.se201411.project.mooncake.order.service.impl.VnpayGateway;
import com.se201411.project.mooncake.order.service.impl.CodPaymentProcessor;
import com.se201411.project.mooncake.order.service.impl.OrderManagementServiceImpl;
import com.se201411.project.mooncake.order.service.impl.PaymentProcessorRegistryImpl;
import com.se201411.project.mooncake.order.service.impl.VnpayPaymentProcessor;
import com.se201411.project.mooncake.security.service.AdminAccessService;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderManagementServiceTest {
    OrderAdminRepository orders = mock(OrderAdminRepository.class);
    OrderItemRepository items = mock(OrderItemRepository.class);
    OrderItemComponentRepository components = mock(OrderItemComponentRepository.class);
    PaymentRepository payments = mock(PaymentRepository.class);
    AdminAccessService access = mock(AdminAccessService.class);
    OrderManagementServiceImpl service = new OrderManagementServiceImpl(
        orders,
        items,
        components,
        payments,
        new PaymentProcessorRegistryImpl(List.of(
            new CodPaymentProcessor(payments),
            new VnpayPaymentProcessor(
                mock(VnpayRepository.class),
                payments,
                mock(VnpayGateway.class),
                Clock.systemUTC()
            )
        )),
        access,
        Clock.systemUTC()
    );

    @Test
    void permitsOnlyThreeNonCancellationTransitions() {
        int allowed = 0;
        for (OrderStatus from : OrderStatus.values()) {
            for (OrderStatus to : OrderStatus.values()) {
                if (OrderManagementServiceImpl.allowed(from, to)) {
                    allowed++;
                }
            }
        }
        assertThat(allowed).isEqualTo(3);
        assertThat(OrderManagementServiceImpl.allowed(OrderStatus.PLACED, OrderStatus.CANCELLED)).isFalse();
        assertThat(OrderManagementServiceImpl.allowed(OrderStatus.PROCESSING, OrderStatus.CANCELLED)).isFalse();
        assertThat(OrderManagementServiceImpl.allowed(OrderStatus.SHIPPING, OrderStatus.CANCELLED)).isFalse();
        assertThat(OrderManagementServiceImpl.allowed(OrderStatus.COMPLETED, OrderStatus.PROCESSING)).isFalse();
    }

    @Test
    void deliveryCollectsCodWithoutChangingStock() {
        order(OrderStatus.SHIPPING, PaymentMethod.COD, PaymentStatus.UNPAID);
        when(orders.transition(eq(1L), eq(OrderStatus.SHIPPING), eq(OrderStatus.COMPLETED), any())).thenReturn(1);
        when(payments.collectCashOnDelivery(eq(1L), any())).thenReturn(1);

        service.transition(1, OrderStatus.COMPLETED);

        verify(payments).collectCashOnDelivery(eq(1L), any());
    }

    @Test
    void invalidCodRaisesFailureForTransactionRollback() {
        order(OrderStatus.SHIPPING, PaymentMethod.COD, PaymentStatus.UNPAID);
        when(orders.transition(eq(1L), eq(OrderStatus.SHIPPING), eq(OrderStatus.COMPLETED), any())).thenReturn(1);

        assertThatThrownBy(() -> service.transition(1, OrderStatus.COMPLETED))
            .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void terminalOrderRejectsRepeat() {
        order(OrderStatus.CANCELLED, PaymentMethod.COD, PaymentStatus.UNPAID);

        assertThatThrownBy(() -> service.transition(1, OrderStatus.CANCELLED))
            .isInstanceOf(BusinessRuleViolationException.class);
        verify(orders, never()).transition(anyLong(), any(), any(), any());
    }

    @Test
    void unpaidVnpayOrderCannotBeProcessed() {
        order(OrderStatus.PLACED, PaymentMethod.VNPAY, PaymentStatus.UNPAID);

        assertThatThrownBy(() -> service.transition(1, OrderStatus.PROCESSING))
            .isInstanceOf(BusinessRuleViolationException.class)
            .hasMessageContaining("VNPay");
        verify(orders, never()).transition(anyLong(), any(), any(), any());
    }

    @Test
    void paidVnpayOrderCompletesWithoutCollectingCash() {
        order(OrderStatus.SHIPPING, PaymentMethod.VNPAY, PaymentStatus.PAID);
        when(orders.transition(eq(1L), eq(OrderStatus.SHIPPING), eq(OrderStatus.COMPLETED), any())).thenReturn(1);

        service.transition(1, OrderStatus.COMPLETED);

        verify(payments, never()).collectCashOnDelivery(anyLong(), any());
    }

    @Test
    void transitionLocksTheOrderRow() {
        order(OrderStatus.PLACED, PaymentMethod.COD, PaymentStatus.UNPAID);
        when(orders.transition(eq(1L), eq(OrderStatus.PLACED), eq(OrderStatus.PROCESSING), any())).thenReturn(1);

        service.transition(1, OrderStatus.PROCESSING);

        verify(orders).requireForUpdate(1L);
        verify(orders, never()).require(anyLong());
    }

    private void order(OrderStatus status, PaymentMethod method, PaymentStatus paymentStatus) {
        SalesOrder order = new SalesOrder();
        order.setStatus(status);
        when(orders.requireForUpdate(1)).thenReturn(order);

        Payment payment = new Payment();
        payment.setMethod(method);
        payment.setStatus(paymentStatus);
        when(payments.findByOrderId(1L)).thenReturn(List.of(payment));
    }

    @Test
    void shippingRequiresTheShipmentForm() {
        order(OrderStatus.PROCESSING, PaymentMethod.COD, PaymentStatus.UNPAID);

        assertThatThrownBy(() -> service.transition(1, OrderStatus.SHIPPING))
            .isInstanceOf(BusinessRuleViolationException.class)
            .hasMessageContaining("thông tin giao hàng");
        verify(orders, never()).transition(anyLong(), any(), any(), any());
    }

    @Test
    void carrierShipmentStoresUppercaseTrackingCodeAndDate() {
        order(OrderStatus.PROCESSING, PaymentMethod.COD, PaymentStatus.UNPAID);
        LocalDate expected = LocalDate.now().plusDays(2);
        when(orders.ship(eq(1L), eq(ShippingCarrier.GHN), eq("GHN123"), eq(expected), any())).thenReturn(1);

        service.ship(1, new ShipmentForm(ShippingCarrier.GHN, " ghn123 ", expected));

        verify(orders).ship(eq(1L), eq(ShippingCarrier.GHN), eq("GHN123"), eq(expected), any());
    }

    @Test
    void carrierWithoutTrackingCodeIsRejected() {
        order(OrderStatus.PROCESSING, PaymentMethod.COD, PaymentStatus.UNPAID);

        assertThatThrownBy(() -> service.ship(1, new ShipmentForm(ShippingCarrier.GHTK, "  ", LocalDate.now().plusDays(2))))
            .isInstanceOf(BusinessRuleViolationException.class)
            .hasMessageContaining("mã vận đơn");
        verify(orders, never()).ship(anyLong(), any(), any(), any(), any());
    }

    @Test
    void storeDeliveryNeedsNoTrackingCode() {
        order(OrderStatus.PROCESSING, PaymentMethod.COD, PaymentStatus.UNPAID);
        LocalDate expected = LocalDate.now().plusDays(1);
        when(orders.ship(eq(1L), eq(ShippingCarrier.STORE), eq(null), eq(expected), any())).thenReturn(1);

        service.ship(1, new ShipmentForm(ShippingCarrier.STORE, null, expected));

        verify(orders).ship(eq(1L), eq(ShippingCarrier.STORE), eq(null), eq(expected), any());
    }

    @Test
    void pastDeliveryDateIsRejected() {
        order(OrderStatus.PROCESSING, PaymentMethod.COD, PaymentStatus.UNPAID);

        assertThatThrownBy(() -> service.ship(1, new ShipmentForm(ShippingCarrier.STORE, null, LocalDate.now().minusDays(2))))
            .isInstanceOf(BusinessRuleViolationException.class)
            .hasMessageContaining("trước hôm nay");
    }

    @Test
    void onlyProcessingOrdersCanBeShipped() {
        order(OrderStatus.PLACED, PaymentMethod.COD, PaymentStatus.UNPAID);

        assertThatThrownBy(() -> service.ship(1, new ShipmentForm(ShippingCarrier.STORE, null, LocalDate.now().plusDays(2))))
            .isInstanceOf(BusinessRuleViolationException.class);
    }
}
