package com.se201411.project.mooncake;

import com.se201411.project.mooncake.inventory.service.PackingService;
import com.se201411.project.mooncake.inventory.dto.AdminStockForm;
import com.se201411.project.mooncake.order.service.OrderManagementService;
import com.se201411.project.mooncake.product.service.ProductAdminService;
import com.se201411.project.mooncake.product.service.PresetConfigurationAdminService;
import com.se201411.project.mooncake.product.dto.AdminPresetForm;
import com.se201411.project.mooncake.product.dto.AdminCatalogForms.*;
import com.se201411.project.mooncake.security.service.AdminAccessService;
import com.se201411.project.mooncake.enums.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.*;
import java.util.concurrent.*;
import java.math.BigDecimal;

import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("integration")
@ContextConfiguration(initializers = MoonCakeApplicationIT.TestDatabaseGuard.class)
class AdminFlowIT {
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    PackingService packing;
    @Autowired
    OrderManagementService orders;
    @Autowired
    ProductAdminService catalog;
    @Autowired
    PresetConfigurationAdminService presets;
    @Autowired
    PlatformTransactionManager transactionManager;
    @MockitoBean
    AdminAccessService access;
    TransactionTemplate tx;
    Long adminId;
    final List<Long> orderIds = new ArrayList<>();

    @BeforeEach
    void setup() {
        assertThat(jdbc.queryForObject("SELECT DB_NAME()", String.class)).isEqualTo("MoonCakeTestDB");
        tx = new TransactionTemplate(transactionManager);
        adminId = jdbc.queryForObject("""
                INSERT INTO dbo.ACCOUNT(email,password_hash,role,status)
                OUTPUT INSERTED.account_id VALUES (?, 'unused-integration-fixture', 'ADMIN','ACTIVE')
                """, Long.class, "it-admin-" + UUID.randomUUID() + "@example.test");
        when(access.requireAdmin()).thenReturn(adminId);
    }

    @AfterEach
    void cleanup() {
        if (tx == null) return;
        tx.executeWithoutResult(status -> {
            for (Long id : orderIds) {
                jdbc.update("DELETE FROM dbo.PAYMENT WHERE order_id=?", id);
                jdbc.update("DELETE FROM dbo.SALES_ORDER WHERE order_id=?", id);
            }
            jdbc.update("DELETE FROM dbo.ACCOUNT WHERE account_id=?", adminId);
        });
    }

    @Test
    void packUsesSqlServerOperationLockAndRollsBackAllMovements() {
        tx.executeWithoutResult(status -> {
            long gift = sku("GIFT-KIM-PHUC-4");
            long box = sku("BOX-KIM-PHUC-4");
            jdbc.update("UPDATE dbo.INVENTORY_BALANCE SET available_quantity=100 WHERE variant_id IN (SELECT mooncake_variant_id FROM dbo.BOX_CONFIGURATION_ITEM)");
            jdbc.update("UPDATE dbo.INVENTORY_BALANCE SET available_quantity=10 WHERE variant_id=?", box);
            int before = quantity(gift);
            String key = UUID.randomUUID().toString();
            packing.pack(new AdminStockForm(gift, 2, "IT pack", key));
            assertThat(quantity(gift)).isEqualTo(before + 2);
            assertThat(quantity(box)).isEqualTo(8);
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM dbo.STOCK_MOVEMENT WHERE operation_group_key=?", Integer.class, "PACK:" + key)).isEqualTo(6);
            status.setRollbackOnly();
        });
    }

    @Test
    void failedPackingTransactionLeavesStockAndLedgerUnchanged() {
        long gift = sku("GIFT-KIM-PHUC-4");
        int before = quantity(gift);
        String key = UUID.randomUUID().toString();
        assertThatThrownBy(() -> tx.executeWithoutResult(status -> {
            jdbc.update("UPDATE dbo.INVENTORY_BALANCE SET available_quantity=0 WHERE variant_id=?", sku("BOX-KIM-PHUC-4"));
            packing.pack(new AdminStockForm(gift, 1, "IT insufficient", key));
        })).isInstanceOf(RuntimeException.class);
        assertThat(quantity(gift)).isEqualTo(before);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM dbo.STOCK_MOVEMENT WHERE operation_group_key=?", Integer.class, "PACK:" + key)).isZero();
    }

    @Test
    void deliveryAndCodCommitTogetherAndConcurrentRepeatCannotWinTwice() throws Exception {
        long id = createOrder(true);
        var start = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            Callable<Boolean> deliver = () -> {
                start.await();
                try {
                    orders.transition(id, OrderStatus.COMPLETED);
                    return true;
                } catch (RuntimeException ex) {
                    return false;
                }
            };
            var first = pool.submit(deliver);
            var second = pool.submit(deliver);
            start.countDown();
            assertThat(List.of(first.get(20, TimeUnit.SECONDS), second.get(20, TimeUnit.SECONDS))).containsExactlyInAnyOrder(true, false);
        }
        assertThat(jdbc.queryForObject("SELECT order_status FROM dbo.SALES_ORDER WHERE order_id=?", String.class, id)).isEqualTo("COMPLETED");
        assertThat(jdbc.queryForObject("SELECT payment_status FROM dbo.PAYMENT WHERE order_id=?", String.class, id)).isEqualTo("PAID");
        assertThat(jdbc.queryForObject("SELECT paid_at FROM dbo.PAYMENT WHERE order_id=?", java.sql.Timestamp.class, id)).isNotNull();
    }

    @Test
    void missingPaymentRollsBackOrderCompletion() {
        long id = createOrder(false);
        assertThatThrownBy(() -> orders.transition(id, OrderStatus.COMPLETED)).isInstanceOf(RuntimeException.class);
        assertThat(jdbc.queryForObject("SELECT order_status FROM dbo.SALES_ORDER WHERE order_id=?", String.class, id)).isEqualTo("SHIPPING");
    }

    @Test
    void createsCatalogSkuAndBalanceAtomically() {
        tx.executeWithoutResult(status -> {
            String key = "it-" + UUID.randomUUID();
            catalog.saveCategory(new CategoryForm(null, key, key, true));
            long category = jdbc.queryForObject("SELECT category_id FROM dbo.CATEGORY WHERE slug=?", Long.class, key);
            catalog.saveProduct(new ProductForm(null, category, ProductType.MOONCAKE, key, key, "Test product", null, null, null, null, true));
            long product = jdbc.queryForObject("SELECT product_id FROM dbo.PRODUCT WHERE slug=?", Long.class, key);
            catalog.saveVariant(new VariantForm(null, product, key, key, null, null, BigDecimal.TEN, true));
            assertThat(quantity(sku(key))).isZero();
            status.setRollbackOnly();
        });
    }

    @Test
    void draftRecipeCanBecomeReadyAndThenRejectsCompositionChanges() {
        tx.executeWithoutResult(status -> {
            String key = "it-preset-" + UUID.randomUUID();
            catalog.saveCategory(new CategoryForm(null, key, key, true));
            long category = jdbc.queryForObject("SELECT category_id FROM dbo.CATEGORY WHERE slug=?", Long.class, key);
            catalog.saveProduct(new ProductForm(null, category, ProductType.GIFT_SET, key, key, "Test gift", null, null, null, null, true));
            long product = jdbc.queryForObject("SELECT product_id FROM dbo.PRODUCT WHERE slug=?", Long.class, key);
            catalog.saveVariant(new VariantForm(null, product, key, key, null, null, BigDecimal.TEN, false));
            long gift = sku(key);
            long box = jdbc.queryForObject("SELECT box_id FROM dbo.BOX WHERE empty_box_variant_id=?", Long.class, sku("BOX-KIM-PHUC-4"));
            presets.saveDraft(new AdminPresetForm(null, gift, box));
            long configuration = jdbc.queryForObject("SELECT configuration_id FROM dbo.BOX_CONFIGURATION WHERE preset_variant_id=?", Long.class, gift);
            long cake = sku("MC-KSPM-080");
            presets.setComponent(configuration, cake, 4);
            presets.activate(configuration);
            assertThatThrownBy(() -> presets.setComponent(configuration, cake, 3)).isInstanceOf(RuntimeException.class);
            assertThat(jdbc.queryForObject("SELECT quantity_per_box FROM dbo.BOX_CONFIGURATION_ITEM WHERE configuration_id=?", Integer.class, configuration)).isEqualTo(4);
            status.setRollbackOnly();
        });
    }

    private long createOrder(boolean payment) {
        long id = jdbc.queryForObject("""
                INSERT INTO dbo.SALES_ORDER(order_code,order_type,buyer_name_snapshot,buyer_phone_snapshot,
                    recipient_name,recipient_phone,shipping_address_snapshot,subtotal_amount,discount_amount,shipping_fee,total_amount,order_status)
                OUTPUT INSERTED.order_id VALUES (?,'RETAIL','IT','0901234567','IT','0901234567','IT address',100,0,0,100,'SHIPPING')
                """, Long.class, "IT-" + UUID.randomUUID());
        orderIds.add(id);
        if (payment)
            jdbc.update("INSERT INTO dbo.PAYMENT(order_id,payment_method,payment_status,amount) VALUES (?,'COD','UNPAID',100)", id);
        return id;
    }

    private long sku(String sku) {
        return jdbc.queryForObject("SELECT variant_id FROM dbo.PRODUCT_VARIANT WHERE sku=?", Long.class, sku);
    }

    private int quantity(long id) {
        return jdbc.queryForObject("SELECT available_quantity FROM dbo.INVENTORY_BALANCE WHERE variant_id=?", Integer.class, id);
    }
}
