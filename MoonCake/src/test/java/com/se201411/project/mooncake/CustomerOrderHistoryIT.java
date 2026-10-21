package com.se201411.project.mooncake;

import com.se201411.project.mooncake.common.exception.ResourceNotFoundException;
import com.se201411.project.mooncake.enums.AccountRole;
import com.se201411.project.mooncake.enums.AccountStatus;
import com.se201411.project.mooncake.order.dto.CustomerOrderDetailView;
import com.se201411.project.mooncake.order.dto.CustomerOrderSummaryView;
import com.se201411.project.mooncake.order.service.CustomerOrderService;
import com.se201411.project.mooncake.security.auth.MoonCakeUserPrincipal;
import com.se201411.project.mooncake.security.dto.ShopperIdentity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integration")
@ContextConfiguration(initializers = MoonCakeApplicationIT.TestDatabaseGuard.class)
@Transactional
class CustomerOrderHistoryIT {

    @Autowired
    private CustomerOrderService customerOrderService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void customerCanReadOnlyOwnedOrdersWithSnapshotDetails() throws Exception {
        long customerId = createCustomer("owner");
        long anotherCustomerId = createCustomer("other");
        long emptyCustomerId = createCustomer("empty");
        long giftVariantId = requireVariantId("GIFT-KIM-PHUC-4");
        long boxVariantId = requireVariantId("BOX-KIM-PHUC-4");
        long cakeVariantId = requireVariantId("MC-KSPM-080");

        String olderOrderCode = uniqueOrderCode("OLDER");
        String newestOrderCode = uniqueOrderCode("NEWEST");
        String otherOrderCode = uniqueOrderCode("OTHER");

        createOrder(
            customerId,
            olderOrderCode,
            giftVariantId,
            boxVariantId,
            cakeVariantId,
            -10
        );
        createOrder(
            customerId,
            newestOrderCode,
            giftVariantId,
            boxVariantId,
            cakeVariantId,
            0
        );
        createOrder(
            anotherCustomerId,
            otherOrderCode,
            giftVariantId,
            boxVariantId,
            cakeVariantId,
            1
        );

        ShopperIdentity customer = ShopperIdentity.forCustomer(customerId);
        List<CustomerOrderSummaryView> orders = customerOrderService.findOrders(customer);

        assertThat(orders).extracting(CustomerOrderSummaryView::orderCode)
            .containsExactly(newestOrderCode, olderOrderCode);

        CustomerOrderDetailView detail = customerOrderService.requireOrder(
            newestOrderCode,
            customer
        );
        assertThat(detail.orderCode()).isEqualTo(newestOrderCode);
        assertThat(detail.statusLabel()).isEqualTo("Đã tiếp nhận");
        assertThat(detail.paymentStatusLabel()).isEqualTo("Chưa thanh toán");
        assertThat(detail.items()).hasSize(1);
        assertThat(detail.items().get(0).components()).hasSize(2);
        assertThat(detail.items().get(0).components())
            .extracting(component -> component.roleLabel())
            .containsExactly("Vỏ hộp", "Bánh");

        assertThatThrownBy(() -> customerOrderService.requireOrder(otherOrderCode, customer))
            .isInstanceOf(ResourceNotFoundException.class);

        Authentication authentication = customerAuthentication(customerId);
        mockMvc.perform(get("/orders").with(authentication(authentication)))
            .andExpect(status().isOk())
            .andExpect(view().name("orders"))
            .andExpect(content().string(containsString(newestOrderCode)))
            .andExpect(content().string(containsString(olderOrderCode)))
            .andExpect(content().string(not(containsString(otherOrderCode))));

        mockMvc.perform(
            get("/orders/{orderCode}", newestOrderCode)
                .with(authentication(authentication))
        )
            .andExpect(status().isOk())
            .andExpect(view().name("order-detail"))
            .andExpect(content().string(containsString(newestOrderCode)))
            .andExpect(content().string(containsString("Hộp quà Kim Phúc")))
            .andExpect(content().string(containsString("Vỏ hộp Kim Phúc")));

        Authentication emptyCustomer = customerAuthentication(emptyCustomerId);
        mockMvc.perform(get("/orders").with(authentication(emptyCustomer)))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Bạn chưa có đơn hàng nào")));
    }

    private long createCustomer(String label) {
        String email = label + "-orders-" + UUID.randomUUID() + "@mooncake.test";
        Long accountId = jdbcTemplate.queryForObject("""
            INSERT INTO dbo.ACCOUNT (email, password_hash, role, status, created_at)
            OUTPUT INSERTED.account_id
            VALUES (?, ?, 'CUSTOMER', 'ACTIVE', SYSUTCDATETIME())
            """, Long.class, email, "test-hash");

        Long customerId = jdbcTemplate.queryForObject("""
            INSERT INTO dbo.CUSTOMER (account_id, full_name, phone, email, created_at)
            OUTPUT INSERTED.customer_id
            VALUES (?, N'Order History Integration Test', '0901234567', ?, SYSUTCDATETIME())
            """, Long.class, accountId, email);
        if (customerId == null) {
            throw new IllegalStateException("Expected customer ID");
        }
        return customerId;
    }

    private void createOrder(
        long customerId,
        String orderCode,
        long giftVariantId,
        long boxVariantId,
        long cakeVariantId,
        int createdAtOffsetMinutes
    ) {
        Long orderId = jdbcTemplate.queryForObject("""
            INSERT INTO dbo.SALES_ORDER (
                order_code,
                customer_id,
                business_id,
                order_type,
                buyer_name_snapshot,
                buyer_phone_snapshot,
                buyer_email_snapshot,
                recipient_name,
                recipient_phone,
                shipping_address_snapshot,
                order_note,
                subtotal_amount,
                discount_amount,
                shipping_fee,
                total_amount,
                order_status,
                created_at,
                updated_at
            )
            OUTPUT INSERTED.order_id
            VALUES (
                ?, ?, NULL, 'RETAIL', N'Khách kiểm thử', '0901234567',
                N'orders@mooncake.test', N'Người nhận kiểm thử', '0901234567',
                N'123 Đường Trung Thu, Phường Bến Thành, Thành phố Hồ Chí Minh',
                N'Gọi trước khi giao', 590000, 0, 30000, 620000, 'PLACED',
                DATEADD(MINUTE, ?, SYSUTCDATETIME()), SYSUTCDATETIME()
            )
            """, Long.class, orderCode, customerId, createdAtOffsetMinutes);
        if (orderId == null) {
            throw new IllegalStateException("Expected order ID");
        }

        Long orderItemId = jdbcTemplate.queryForObject("""
            INSERT INTO dbo.ORDER_ITEM (
                order_id,
                variant_id,
                configuration_id,
                volume_discount_tier_id,
                item_kind,
                inventory_source_snapshot,
                product_name_snapshot,
                sku_snapshot,
                discount_group_snapshot,
                box_name_snapshot,
                box_sku_snapshot,
                quantity,
                unit_base_price_snapshot,
                discount_pct_snapshot,
                unit_discount_snapshot,
                unit_final_price_snapshot,
                line_total
            )
            OUTPUT INSERTED.order_item_id
            VALUES (
                ?, ?, NULL, NULL, 'STANDARD', 'STOCKED_SKU', N'Hộp quà Kim Phúc',
                N'GIFT-KIM-PHUC-4', 'GIFT_BOX', NULL, NULL, 1,
                590000, 0, 0, 590000, 590000
            )
            """, Long.class, orderId, giftVariantId);
        if (orderItemId == null) {
            throw new IllegalStateException("Expected order item ID");
        }

        jdbcTemplate.update("""
            INSERT INTO dbo.ORDER_ITEM_COMPONENT (
                order_item_id,
                component_variant_id,
                component_role,
                component_name_snapshot,
                quantity_per_set,
                unit_price_snapshot
            )
            VALUES (?, ?, 'BOX', N'Vỏ hộp Kim Phúc', 1, 190000)
            """, orderItemId, boxVariantId);
        jdbcTemplate.update("""
            INSERT INTO dbo.ORDER_ITEM_COMPONENT (
                order_item_id,
                component_variant_id,
                component_role,
                component_name_snapshot,
                quantity_per_set,
                unit_price_snapshot
            )
            VALUES (?, ?, 'CAKE', N'Bánh Kim Sa Phô Mai', 4, 100000)
            """, orderItemId, cakeVariantId);
        jdbcTemplate.update("""
            INSERT INTO dbo.PAYMENT (
                order_id,
                payment_method,
                payment_status,
                amount,
                paid_at,
                created_at,
                updated_at
            )
            VALUES (?, 'COD', 'UNPAID', 620000, NULL, SYSUTCDATETIME(), SYSUTCDATETIME())
            """, orderId);
    }

    private long requireVariantId(String sku) {
        Long variantId = jdbcTemplate.queryForObject("""
            SELECT variant_id
            FROM dbo.PRODUCT_VARIANT
            WHERE sku = ?
            """, Long.class, sku);
        if (variantId == null) {
            throw new IllegalStateException("Expected variant for SKU " + sku);
        }
        return variantId;
    }

    private String uniqueOrderCode(String label) {
        String randomPart = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "IT-" + label + "-" + randomPart;
    }

    private Authentication customerAuthentication(long customerId) {
        MoonCakeUserPrincipal principal = new MoonCakeUserPrincipal(
            customerId,
            customerId,
            "order-history@mooncake.test",
            "test-hash",
            AccountRole.CUSTOMER,
            AccountStatus.ACTIVE
        );
        return UsernamePasswordAuthenticationToken.authenticated(
            principal,
            null,
            principal.getAuthorities()
        );
    }
}
