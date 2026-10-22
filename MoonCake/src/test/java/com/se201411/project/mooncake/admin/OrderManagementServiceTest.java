package com.se201411.project.mooncake.admin;

import com.se201411.project.mooncake.order.service.impl.OrderManagementServiceImpl;
import com.se201411.project.mooncake.order.repository.*;
import com.se201411.project.mooncake.inventory.service.InventoryService;
import com.se201411.project.mooncake.inventory.dto.CancellationInventoryCommand;
import com.se201411.project.mooncake.security.service.AdminAccessService;
import com.se201411.project.mooncake.common.exception.BusinessRuleViolationException;
import com.se201411.project.mooncake.entity.*;
import com.se201411.project.mooncake.enums.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.*;
import java.util.*;

import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

class OrderManagementServiceTest {
    OrderAdminRepository orders = mock(OrderAdminRepository.class);
    OrderItemRepository items = mock(OrderItemRepository.class);
    OrderItemComponentRepository components = mock(OrderItemComponentRepository.class);
    PaymentRepository payments = mock(PaymentRepository.class);
    InventoryService inventory = mock(InventoryService.class);
    AdminAccessService access = mock(AdminAccessService.class);
    OrderManagementServiceImpl service = new OrderManagementServiceImpl(orders, items, components, payments, inventory, access, Clock.systemUTC());

    @Test
    void permitsOnlyFiveApprovedTransitions() {
        int allowed = 0;
        for (var from : OrderStatus.values())
            for (var to : OrderStatus.values()) if (OrderManagementServiceImpl.allowed(from, to)) allowed++;
        assertThat(allowed).isEqualTo(5);
        assertThat(OrderManagementServiceImpl.allowed(OrderStatus.SHIPPING, OrderStatus.CANCELLED)).isFalse();
        assertThat(OrderManagementServiceImpl.allowed(OrderStatus.COMPLETED, OrderStatus.PROCESSING)).isFalse();
    }

    @Test
    void cancellationRestoresStockedSkuAndCustomSnapshotsOnly() {
        order(OrderStatus.PLACED);
        when(orders.transition(eq(1L), eq(OrderStatus.PLACED), eq(OrderStatus.CANCELLED), any())).thenReturn(1);
        OrderItem standard = new OrderItem();
        ReflectionTestUtils.setField(standard, "id", 1L);
        standard.setInventorySourceSnapshot(InventorySource.STOCKED_SKU);
        standard.setVariant(variant(10));
        standard.setQuantity(2);
        OrderItem custom = new OrderItem();
        ReflectionTestUtils.setField(custom, "id", 2L);
        custom.setInventorySourceSnapshot(InventorySource.COMPONENTS);
        custom.setQuantity(3);
        OrderItemComponent cake = new OrderItemComponent();
        cake.setOrderItem(custom);
        cake.setComponentVariant(variant(10));
        cake.setQuantityPerSet(4);
        OrderItemComponent box = new OrderItemComponent();
        box.setOrderItem(custom);
        box.setComponentVariant(variant(20));
        box.setQuantityPerSet(1);
        when(items.findByOrderId(1)).thenReturn(List.of(standard, custom));
        when(components.findByOrderItemIds(List.of(1L, 2L))).thenReturn(List.of(cake, box));
        service.transition(1, OrderStatus.CANCELLED);
        var capture = ArgumentCaptor.forClass(CancellationInventoryCommand.class);
        verify(inventory).restoreForCancellation(capture.capture());
        assertThat(capture.getValue().operationKey()).isEqualTo("CANCEL_RETURN:1");
        assertThat(capture.getValue().demands()).extracting("quantity").containsExactly(14, 3);
    }

    @Test
    void lostTransitionNeverRestoresInventory() {
        order(OrderStatus.PROCESSING);
        assertThatThrownBy(() -> service.transition(1, OrderStatus.CANCELLED)).isInstanceOf(BusinessRuleViolationException.class);
        verifyNoInteractions(inventory, items);
    }

    @Test
    void deliveryCollectsCodWithoutChangingStock() {
        order(OrderStatus.SHIPPING);
        when(orders.transition(eq(1L), eq(OrderStatus.SHIPPING), eq(OrderStatus.COMPLETED), any())).thenReturn(1);
        when(orders.collectCod(eq(1L), any())).thenReturn(1);
        service.transition(1, OrderStatus.COMPLETED);
        verify(orders).collectCod(eq(1L), any());
        verifyNoInteractions(inventory);
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
        verifyNoInteractions(inventory);
    }

    private void order(OrderStatus status) {
        SalesOrder o = new SalesOrder();
        o.setStatus(status);
        when(orders.require(1)).thenReturn(o);
    }

    private ProductVariant variant(long id) {
        var v = new ProductVariant();
        ReflectionTestUtils.setField(v, "id", id);
        return v;
    }
}
