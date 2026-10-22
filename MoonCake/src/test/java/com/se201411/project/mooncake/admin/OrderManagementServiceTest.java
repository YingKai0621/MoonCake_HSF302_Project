package com.se201411.project.mooncake.admin;

import com.se201411.project.mooncake.order.service.impl.OrderManagementServiceImpl;
import com.se201411.project.mooncake.order.repository.*;
import com.se201411.project.mooncake.security.service.AdminAccessService;
import com.se201411.project.mooncake.common.exception.BusinessRuleViolationException;
import com.se201411.project.mooncake.entity.*;
import com.se201411.project.mooncake.enums.*;
import org.junit.jupiter.api.Test;

import java.time.*;

import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

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
        access,
        Clock.systemUTC()
    );

    @Test
    void permitsOnlyThreeNonCancellationTransitions() {
        int allowed = 0;
        for (var from : OrderStatus.values())
            for (var to : OrderStatus.values()) if (OrderManagementServiceImpl.allowed(from, to)) allowed++;
        assertThat(allowed).isEqualTo(3);
        assertThat(OrderManagementServiceImpl.allowed(OrderStatus.PLACED, OrderStatus.CANCELLED)).isFalse();
        assertThat(OrderManagementServiceImpl.allowed(OrderStatus.PROCESSING, OrderStatus.CANCELLED)).isFalse();
        assertThat(OrderManagementServiceImpl.allowed(OrderStatus.SHIPPING, OrderStatus.CANCELLED)).isFalse();
        assertThat(OrderManagementServiceImpl.allowed(OrderStatus.COMPLETED, OrderStatus.PROCESSING)).isFalse();
    }

    @Test
    void deliveryCollectsCodWithoutChangingStock() {
        order(OrderStatus.SHIPPING);
        when(orders.transition(eq(1L), eq(OrderStatus.SHIPPING), eq(OrderStatus.COMPLETED), any())).thenReturn(1);
        when(orders.collectCod(eq(1L), any())).thenReturn(1);
        service.transition(1, OrderStatus.COMPLETED);
        verify(orders).collectCod(eq(1L), any());
    }

    @Test
    void invalidCodRaisesFailureForTransactionRollback() {
        order(OrderStatus.SHIPPING);
        when(orders.transition(eq(1L), eq(OrderStatus.SHIPPING), eq(OrderStatus.COMPLETED), any())).thenReturn(1);
        assertThatThrownBy(() -> service.transition(1, OrderStatus.COMPLETED)).isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void terminalOrderRejectsRepeat() {
        order(OrderStatus.CANCELLED);
        assertThatThrownBy(() -> service.transition(1, OrderStatus.CANCELLED)).isInstanceOf(BusinessRuleViolationException.class);
        verify(orders, never()).transition(anyLong(), any(), any(), any());
    }

    private void order(OrderStatus status) {
        SalesOrder o = new SalesOrder();
        o.setStatus(status);
        when(orders.require(1)).thenReturn(o);
    }
}
