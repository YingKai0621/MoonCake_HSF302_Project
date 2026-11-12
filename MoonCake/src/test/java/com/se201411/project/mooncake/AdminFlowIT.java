package com.se201411.project.mooncake;

import java.time.LocalDate;
import com.se201411.project.mooncake.order.dto.ShipmentForm;
import com.se201411.project.mooncake.enums.ShippingCarrier;
import com.se201411.project.mooncake.inventory.service.InventoryAdminService;
import com.se201411.project.mooncake.inventory.service.InventoryService;
import com.se201411.project.mooncake.inventory.service.PackingService;
import com.se201411.project.mooncake.inventory.dto.AdminStockForm;
import com.se201411.project.mooncake.order.service.OrderManagementService;
import com.se201411.project.mooncake.product.service.GiftBoxAdminService;
import com.se201411.project.mooncake.product.service.ProductAdminService;
import com.se201411.project.mooncake.product.service.PresetConfigurationAdminService;
import com.se201411.project.mooncake.product.dto.AdminPresetForm;
import com.se201411.project.mooncake.product.dto.AdminCatalogForms.*;
import com.se201411.project.mooncake.security.service.AdminAccessService;
import com.se201411.project.mooncake.enums.*;
import jakarta.persistence.EntityManager;
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
    InventoryAdminService inventoryAdmin;
    @Autowired
    InventoryService inventoryService;
    @Autowired
    EntityManager entityManager;
    @Autowired
    OrderManagementService orders;
    @Autowired
    ProductAdminService catalog;
    @Autowired
    PresetConfigurationAdminService presets;
    @Autowired
    GiftBoxAdminService giftBoxes;
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
            giveCakesDatedStock(100);
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
    void packingWithUndatedCakeStockIsRefusedAndChangesNothing() {
        long gift = sku("GIFT-KIM-PHUC-4");
        int before = quantity(gift);
        String key = UUID.randomUUID().toString();
        assertThatThrownBy(() -> tx.executeWithoutResult(status -> {
            jdbc.update("UPDATE dbo.STOCK_BATCH SET produced_on=NULL, expires_on=NULL WHERE variant_id IN (SELECT mooncake_variant_id FROM dbo.BOX_CONFIGURATION_ITEM)");
            jdbc.update("UPDATE dbo.INVENTORY_BALANCE SET available_quantity=available_quantity+100 WHERE variant_id IN (SELECT mooncake_variant_id FROM dbo.BOX_CONFIGURATION_ITEM)");
            jdbc.update("UPDATE dbo.INVENTORY_BALANCE SET available_quantity=10 WHERE variant_id=?", sku("BOX-KIM-PHUC-4"));
            packing.pack(new AdminStockForm(gift, 1, "IT undated", key));
        })).hasMessageContaining("còn lô chưa có hạn dùng");
        assertThat(quantity(gift)).isEqualTo(before);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM dbo.STOCK_MOVEMENT WHERE operation_group_key=?", Integer.class, "PACK:" + key)).isZero();
    }

    private void giveCakesDatedStock(int quantity) {
        LocalDate produced = LocalDate.now().minusDays(1);
        LocalDate expires = produced.plusDays(30);
        String cakes = "SELECT DISTINCT mooncake_variant_id FROM dbo.BOX_CONFIGURATION_ITEM";
        jdbc.update("UPDATE dbo.STOCK_BATCH SET produced_on=?, expires_on=? WHERE expires_on IS NULL AND variant_id IN (" + cakes + ")",
            produced, expires);
        jdbc.update("""
                INSERT INTO dbo.STOCK_BATCH(variant_id, batch_code, produced_on, expires_on, remaining_quantity)
                SELECT cake.mooncake_variant_id, CONCAT('IT-PACK-', cake.mooncake_variant_id, '-', ?), ?, ?,
                       ? - COALESCE((SELECT SUM(b.remaining_quantity) FROM dbo.STOCK_BATCH b WHERE b.variant_id = cake.mooncake_variant_id), 0)
                FROM (SELECT DISTINCT mooncake_variant_id FROM dbo.BOX_CONFIGURATION_ITEM) cake
                WHERE ? > COALESCE((SELECT SUM(b.remaining_quantity) FROM dbo.STOCK_BATCH b WHERE b.variant_id = cake.mooncake_variant_id), 0)
                """, UUID.randomUUID().toString().substring(0, 8), produced, expires, quantity, quantity);
        jdbc.update("UPDATE dbo.INVENTORY_BALANCE SET available_quantity=? WHERE variant_id IN (" + cakes + ")", quantity);
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
                    orders.transition(id, OrderStatus.DELIVERED);
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
        assertThat(jdbc.queryForObject("SELECT order_status FROM dbo.SALES_ORDER WHERE order_id=?", String.class, id)).isEqualTo("DELIVERED");
        assertThat(jdbc.queryForObject("SELECT payment_status FROM dbo.PAYMENT WHERE order_id=?", String.class, id)).isEqualTo("PAID");
        assertThat(jdbc.queryForObject("SELECT paid_at FROM dbo.PAYMENT WHERE order_id=?", java.sql.Timestamp.class, id)).isNotNull();
    }

    @Test
    void shipmentStoresCarrierTrackingCodeDateAndTime() {
        long id = createOrder(true);
        jdbc.update("UPDATE dbo.SALES_ORDER SET order_status='PROCESSING' WHERE order_id=?", id);
        LocalDate expected = LocalDate.now().plusDays(3);

        orders.ship(id, new ShipmentForm(ShippingCarrier.GHN, "ghn-it-1", expected));

        Map<String, Object> row = jdbc.queryForMap("SELECT order_status, shipping_carrier, tracking_code, estimated_delivery_date, shipping_at FROM dbo.SALES_ORDER WHERE order_id=?", id);
        assertThat(row.get("order_status")).isEqualTo("SHIPPING");
        assertThat(row.get("shipping_carrier")).isEqualTo("GHN");
        assertThat(row.get("tracking_code")).isEqualTo("GHN-IT-1");
        assertThat(row.get("estimated_delivery_date").toString()).isEqualTo(expected.toString());
        assertThat(row.get("shipping_at")).isNotNull();
    }

    @Test
    void missingPaymentRollsBackOrderCompletion() {
        long id = createOrder(false);
        assertThatThrownBy(() -> orders.transition(id, OrderStatus.DELIVERED)).isInstanceOf(RuntimeException.class);
        assertThat(jdbc.queryForObject("SELECT order_status FROM dbo.SALES_ORDER WHERE order_id=?", String.class, id)).isEqualTo("SHIPPING");
    }

    @Test
    void createsCatalogSkuAndBalanceAtomically() {
        tx.executeWithoutResult(status -> {
            String key = "it-" + UUID.randomUUID();
            catalog.saveCategory(new CategoryForm(null, key, key, true));
            long category = jdbc.queryForObject("SELECT category_id FROM dbo.CATEGORY WHERE slug=?", Long.class, key);
            catalog.saveProduct(new ProductForm(null, category, ProductType.MOONCAKE, key, key, "Test product", null, null, 30, null, true));
            long product = jdbc.queryForObject("SELECT product_id FROM dbo.PRODUCT WHERE slug=?", Long.class, key);
            catalog.saveVariant(new VariantForm(null, product, key, key, 150, "Thập cẩm", null, null, BigDecimal.valueOf(65_000), true));
            assertThat(quantity(sku(key))).isZero();
            status.setRollbackOnly();
        });
    }

    @Test
    void deletesUnusedProductWithItsSkuImageAndBalance() {
        tx.executeWithoutResult(status -> {
            String key = "it-delete-" + UUID.randomUUID();
            catalog.saveCategory(new CategoryForm(null, key, key, true));
            long category = jdbc.queryForObject("SELECT category_id FROM dbo.CATEGORY WHERE slug=?", Long.class, key);
            long product = catalog.createProduct(new NewProductForm(
                category, ProductType.MOONCAKE, key, key, "Test product", null, null, 30, null, false,
                key.toUpperCase(), key, BigDecimal.valueOf(65_000), 150, "Thập cẩm", null, null, "/images/products/test.png", null));
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM dbo.PRODUCT_IMAGE i JOIN dbo.PRODUCT_VARIANT v ON v.variant_id=i.variant_id WHERE v.product_id=?", Integer.class, product)).isEqualTo(1);

            catalog.deleteProduct(product);

            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM dbo.PRODUCT WHERE product_id=?", Integer.class, product)).isZero();
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM dbo.PRODUCT_VARIANT WHERE sku=?", Integer.class, key.toUpperCase())).isZero();
            status.setRollbackOnly();
        });
    }

    @Test
    void newGiftBoxIsCreatedHiddenWithADraftRecipeOnTheChosenBox() {
        tx.executeWithoutResult(status -> {
            String key = "it-gift-" + UUID.randomUUID();
            long category = jdbc.queryForObject("SELECT TOP 1 category_id FROM dbo.CATEGORY ORDER BY category_id", Long.class);
            long box = jdbc.queryForObject("SELECT TOP 1 box_id FROM dbo.BOX WHERE is_active = 1 ORDER BY box_id", Long.class);

            long product = giftBoxes.create(new NewProductForm(
                category, ProductType.GIFT_SET, key, key, "Hộp quà test", null, null, 30, null, true,
                key.toUpperCase(), "Hộp quà", BigDecimal.valueOf(450_000), null, null, null, null, null, null), box);
            entityManager.flush();

            Long recipeBox = jdbc.queryForObject("""
                    SELECT c.box_id FROM dbo.BOX_CONFIGURATION c
                    JOIN dbo.PRODUCT_VARIANT v ON v.variant_id = c.preset_variant_id
                    WHERE v.product_id = ? AND c.configuration_status = 'DRAFT'
                    """, Long.class, product);
            assertThat(recipeBox).isEqualTo(box);
            Boolean visible = jdbc.queryForObject("SELECT is_active FROM dbo.PRODUCT WHERE product_id=?", Boolean.class, product);
            assertThat(visible).isFalse();
            status.setRollbackOnly();
        });
    }

    @Test
    void emptyBoxVariantIsSavedWithItsCapacity() {
        tx.executeWithoutResult(status -> {
            String key = "it-box-" + UUID.randomUUID();
            long category = jdbc.queryForObject("SELECT TOP 1 category_id FROM dbo.CATEGORY ORDER BY category_id", Long.class);

            long product = catalog.createProduct(new NewProductForm(
                category, ProductType.EMPTY_BOX, key, key, "Vỏ hộp test", null, null, null, null, false,
                key.toUpperCase(), "Hộp 6 bánh", BigDecimal.valueOf(40_000), null, null, 6, "Giấy kraft", null, null));
            entityManager.flush();

            Integer capacity = jdbc.queryForObject("""
                    SELECT b.capacity_cakes FROM dbo.BOX b
                    JOIN dbo.PRODUCT_VARIANT v ON v.variant_id = b.empty_box_variant_id
                    WHERE v.product_id = ?
                    """, Integer.class, product);
            assertThat(capacity).isEqualTo(6);
            status.setRollbackOnly();
        });
    }

    @Test
    void receivedCakeBatchGetsExpiryFromProductShelfLife() {
        tx.executeWithoutResult(status -> {
            long variant = sku("MC-KSPM-080");
            LocalDate produced = LocalDate.now().minusDays(1);
            String lot = "IT-LOT-" + UUID.randomUUID().toString().substring(0, 8);

            inventoryAdmin.receive(new AdminStockForm(variant, 3, "IT receive", UUID.randomUUID().toString()), lot, produced);
            entityManager.flush();

            LocalDate expires = jdbc.queryForObject("SELECT expires_on FROM dbo.STOCK_BATCH WHERE batch_code=?", LocalDate.class, lot);
            assertThat(expires).isEqualTo(produced.plusDays(30));
            status.setRollbackOnly();
        });
    }

    @Test
    void expiredStockIsHiddenFromCustomersAndCanBeWrittenOff() {
        tx.executeWithoutResult(status -> {
            long variant = sku("MC-HSMC-120");
            jdbc.update("UPDATE dbo.STOCK_BATCH SET remaining_quantity=0 WHERE variant_id=?", variant);
            jdbc.update("UPDATE dbo.INVENTORY_BALANCE SET available_quantity=5 WHERE variant_id=?", variant);
            LocalDate today = LocalDate.now();
            jdbc.update("INSERT INTO dbo.STOCK_BATCH(variant_id,batch_code,produced_on,expires_on,remaining_quantity) VALUES (?,?,?,?,5)",
                variant, "IT-EXP-" + UUID.randomUUID().toString().substring(0, 8), today.minusDays(40), today.minusDays(10));

            assertThat(inventoryService.availableQuantities(List.of(variant))).containsEntry(variant, 0);

            inventoryAdmin.writeOffExpired(variant, UUID.randomUUID().toString());

            assertThat(quantity(variant)).isZero();
            assertThat(jdbc.queryForObject("SELECT COALESCE(SUM(remaining_quantity),0) FROM dbo.STOCK_BATCH WHERE variant_id=?", Integer.class, variant)).isZero();
            status.setRollbackOnly();
        });
    }

    @Test
    void refusesToDeleteProductWithStockHistory() {
        tx.executeWithoutResult(status -> {
            long variant = sku("MC-KSPM-080");
            long product = jdbc.queryForObject("SELECT product_id FROM dbo.PRODUCT_VARIANT WHERE variant_id=?", Long.class, variant);
            String key = "it-history-" + UUID.randomUUID();
            jdbc.update("""
                    INSERT INTO dbo.STOCK_MOVEMENT(variant_id, performed_by_account_id, movement_type, quantity_delta,
                        reason, operation_key, operation_group_key, created_at)
                    VALUES (?, ?, 'ADJUST', 1, 'IT history', ?, ?, SYSUTCDATETIME())
                    """, variant, adminId, key + ":" + variant, key);
            assertThatThrownBy(() -> catalog.deleteProduct(product)).isInstanceOf(RuntimeException.class);
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM dbo.PRODUCT WHERE product_id=?", Integer.class, product)).isEqualTo(1);
            status.setRollbackOnly();
        });
    }

    @Test
    void draftRecipeCanBecomeReadyAndThenRejectsCompositionChanges() {
        tx.executeWithoutResult(status -> {
            String key = "it-preset-" + UUID.randomUUID();
            catalog.saveCategory(new CategoryForm(null, key, key, true));
            long category = jdbc.queryForObject("SELECT category_id FROM dbo.CATEGORY WHERE slug=?", Long.class, key);
            catalog.saveProduct(new ProductForm(null, category, ProductType.GIFT_SET, key, key, "Test gift", null, null, 30, null, true));
            long product = jdbc.queryForObject("SELECT product_id FROM dbo.PRODUCT WHERE slug=?", Long.class, key);
            catalog.saveVariant(new VariantForm(null, product, key, key, null, null, null, null, BigDecimal.valueOf(450_000), false));
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
