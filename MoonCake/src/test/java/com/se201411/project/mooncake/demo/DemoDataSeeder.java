package com.se201411.project.mooncake.demo;

import com.se201411.project.mooncake.cart.service.CustomerCartService;
import com.se201411.project.mooncake.enums.OrderStatus;
import com.se201411.project.mooncake.enums.ShippingCarrier;
import com.se201411.project.mooncake.feedback.dto.FeedbackForm;
import com.se201411.project.mooncake.feedback.service.FeedbackService;
import com.se201411.project.mooncake.inventory.dto.AdminStockForm;
import com.se201411.project.mooncake.inventory.service.InventoryAdminService;
import com.se201411.project.mooncake.inventory.service.PackingService;
import com.se201411.project.mooncake.order.dto.CheckoutForm;
import com.se201411.project.mooncake.order.dto.CheckoutPreparation;
import com.se201411.project.mooncake.order.dto.CheckoutResult;
import com.se201411.project.mooncake.order.dto.ShipmentForm;
import com.se201411.project.mooncake.order.service.CheckoutService;
import com.se201411.project.mooncake.order.service.CustomerOrderService;
import com.se201411.project.mooncake.order.service.OrderCancellationService;
import com.se201411.project.mooncake.order.service.OrderManagementService;
import com.se201411.project.mooncake.security.dto.AdminAccountForm;
import com.se201411.project.mooncake.security.dto.CustomerRegistrationForm;
import com.se201411.project.mooncake.security.dto.ShopperIdentity;
import com.se201411.project.mooncake.security.service.AdminAccountService;
import com.se201411.project.mooncake.security.service.AuthenticationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class DemoDataSeeder {

    private static final String DEMO_DATABASE = "MoonCakeDemoDB";
    private static final String DEMO_PASSWORD = "MoonCake@2026";
    private static final String MAIN_CUSTOMER = "khach.demo@example.test";
    private static final String SECOND_ADMIN = "nhanvien.demo@example.test";

    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private UserDetailsService userDetailsService;
    @Autowired
    private AuthenticationService authenticationService;
    @Autowired
    private AdminAccountService adminAccountService;
    @Autowired
    private InventoryAdminService inventoryAdminService;
    @Autowired
    private PackingService packingService;
    @Autowired
    private CustomerCartService customerCartService;
    @Autowired
    private CheckoutService checkoutService;
    @Autowired
    private OrderManagementService orderManagementService;
    @Autowired
    private CustomerOrderService customerOrderService;
    @Autowired
    private OrderCancellationService orderCancellationService;
    @Autowired
    private FeedbackService feedbackService;

    private final LocalDate today = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));

    @Test
    void seedPresentationData() {
        requireFreshDemoDatabase();
        String superAdmin = jdbc.queryForObject(
            "SELECT TOP 1 email FROM dbo.ACCOUNT WHERE role = 'ADMIN' ORDER BY account_id", String.class);

        signIn(superAdmin);
        createSecondAdmin();
        registerCustomers();

        signIn(superAdmin);
        receiveStock();
        packGiftBoxes();
        markOneCakeLotExpired();

        placeOrders(superAdmin);
        writeReviews(superAdmin);

        SecurityContextHolder.clearContext();
        checkEverything();
    }

    private void requireFreshDemoDatabase() {
        String database = jdbc.queryForObject("SELECT DB_NAME()", String.class);
        assertThat(database).as("Chỉ chạy trên " + DEMO_DATABASE).isEqualTo(DEMO_DATABASE);
        Integer orders = jdbc.queryForObject("SELECT COUNT(*) FROM dbo.SALES_ORDER", Integer.class);
        assertThat(orders).as("Database demo phải còn trống đơn hàng").isZero();
        Integer admins = jdbc.queryForObject("SELECT COUNT(*) FROM dbo.ACCOUNT WHERE role = 'ADMIN'", Integer.class);
        assertThat(admins).as("Hãy tạo super admin bằng bootstrap-admin trước").isEqualTo(1);
    }

    private void createSecondAdmin() {
        AdminAccountForm form = new AdminAccountForm();
        form.setEmail(SECOND_ADMIN);
        form.setPassword(DEMO_PASSWORD);
        adminAccountService.create(form);
    }

    private void registerCustomers() {
        SecurityContextHolder.clearContext();
        register(MAIN_CUSTOMER, "Nguyễn Minh Anh", "0901234567");
        register("tran.binh@example.test", "Trần Văn Bình", "0912345678");
        register("le.chi@example.test", "Lê Thu Chi", "0923456789");
        register("pham.dung@example.test", "Phạm Quốc Dũng", "0934567890");
    }

    private void register(String email, String fullName, String phone) {
        CustomerRegistrationForm form = new CustomerRegistrationForm();
        form.setEmail(email);
        form.setPassword(DEMO_PASSWORD);
        form.setConfirmPassword(DEMO_PASSWORD);
        form.setFullName(fullName);
        form.setPhone(phone);
        authenticationService.registerCustomer(form);
    }

    private void receiveStock() {
        List<String> giftCakes = List.of(
            "MC-LXGQXO-150", "MC-SDHK-150", "MC-HSMC-120", "MC-TXHN-120", "MC-THRV-150", "MC-SCDHDC-120"
        );
        for (String sku : giftCakes) {
            receive(sku, 60, "LO-A", today.minusDays(12));
            receive(sku, 100, "LO-B", today.minusDays(2));
        }
        receive("MC-KSPM-080", 40, "LO-A", today.minusDays(10));
        receive("MC-KSPM-080", 80, "LO-B", today.minusDays(1));
        receive("MC-PBMHA-080", 40, "LO-A", today.minusDays(10));
        receive("MC-PBMHA-080", 80, "LO-B", today.minusDays(1));
        receive("MC-CBTN-080", 15, "LO-SAP-HET", today.minusDays(25));
        receive("MC-CBTN-080", 60, "LO-B", today.minusDays(3));
        receive("MC-TCGQ-080", 6, "LO-CU", today.minusDays(20));
        receive("MC-TCGQ-080", 50, "LO-B", today.minusDays(3));
        receive("MC-LSTT-080", 4, "LO-A", today.minusDays(5));
        receive("BOX-KIM-PHUC-4", 200, "VO-KP", null);
        receive("BOX-LAM-PHUC-6", 150, "VO-LP", null);
    }

    private void receive(String sku, int quantity, String lotPrefix, LocalDate producedOn) {
        String lotCode = lotPrefix + "-" + sku;
        AdminStockForm form = new AdminStockForm(variant(sku), quantity, "Nhập hàng từ xưởng", key());
        inventoryAdminService.receive(form, lotCode, producedOn);
    }

    private void packGiftBoxes() {
        packingService.pack(new AdminStockForm(variant("GIFT-KIM-PHUC-4"), 30, "Đóng hộp quà Kim Phúc", key()));
        packingService.pack(new AdminStockForm(variant("GIFT-LAM-PHUC-6"), 20, "Đóng hộp quà Lam Phúc", key()));
    }

    private void markOneCakeLotExpired() {
        jdbc.update("""
            UPDATE dbo.STOCK_BATCH
            SET produced_on = ?, expires_on = ?
            WHERE batch_code = 'LO-CU-MC-TCGQ-080'
            """, today.minusDays(40), today.minusDays(10));
    }

    private void placeOrders(String superAdmin) {
        String mainOne = placeCod(MAIN_CUSTOMER, items("GIFT-KIM-PHUC-4", 2, "MC-KSPM-080", 2));
        complete(superAdmin, MAIN_CUSTOMER, mainOne);
        shiftDays(mainOne, 6);

        String mainTwo = placeCod(MAIN_CUSTOMER, items("GIFT-LAM-PHUC-6", 1));
        signIn(superAdmin);
        orderManagementService.transition(orderId(mainTwo), OrderStatus.PROCESSING);
        ship(mainTwo);
        shiftDays(mainTwo, 1);

        String binhOne = placeCod("tran.binh@example.test", items("MC-LXGQXO-150", 4));
        complete(superAdmin, "tran.binh@example.test", binhOne);
        shiftDays(binhOne, 5);

        String binhTwo = placeCod("tran.binh@example.test", items("GIFT-KIM-PHUC-4", 1));
        signIn(superAdmin);
        orderCancellationService.cancelForAdmin(orderId(binhTwo));
        shiftDays(binhTwo, 4);

        String binhThree = placeCod("tran.binh@example.test", items("MC-SDHK-150", 2, "GIFT-LAM-PHUC-6", 1));
        signIn(superAdmin);
        orderManagementService.transition(orderId(binhThree), OrderStatus.PROCESSING);

        String chiOne = placeCod("le.chi@example.test", items("GIFT-LAM-PHUC-6", 1, "MC-THRV-150", 2));
        complete(superAdmin, "le.chi@example.test", chiOne);
        shiftDays(chiOne, 3);

        String chiTwo = placeCod("le.chi@example.test", items("MC-SCDHDC-120", 2));
        signIn(superAdmin);
        orderManagementService.transition(orderId(chiTwo), OrderStatus.PROCESSING);
        ship(chiTwo);
        orderManagementService.transition(orderId(chiTwo), OrderStatus.DELIVERED);

        String dungOne = placeCod("pham.dung@example.test", items("MC-HSMC-120", 6));
        complete(superAdmin, "pham.dung@example.test", dungOne);
        shiftDays(dungOne, 2);

        String dungTwo = placeCod("pham.dung@example.test", items("MC-TXHN-120", 2));
        signIn("pham.dung@example.test");
        orderCancellationService.cancelCurrentCustomerOrder(dungTwo);
        shiftDays(dungTwo, 1);

        placeCod("pham.dung@example.test", items("GIFT-KIM-PHUC-4", 1, "MC-PBMHA-080", 2));
    }

    private String placeCod(String email, Map<String, Integer> items) {
        ShopperIdentity customer = ShopperIdentity.forCustomer(customerId(email));
        for (Map.Entry<String, Integer> item : items.entrySet()) {
            customerCartService.addVariant(customer, variant(item.getKey()), item.getValue());
        }
        CheckoutPreparation preparation = checkoutService.prepareCheckout(customer);
        CheckoutForm form = new CheckoutForm();
        form.setCartId(preparation.cartId());
        form.setRecipientName(jdbc.queryForObject(
            "SELECT full_name FROM dbo.CUSTOMER WHERE customer_id = ?", String.class, customer.customerId()));
        form.setRecipientPhone(jdbc.queryForObject(
            "SELECT phone FROM dbo.CUSTOMER WHERE customer_id = ?", String.class, customer.customerId()));
        form.setProvinceCode("79");
        form.setWardCode("26740");
        form.setStreetAddress("12 Nguyễn Huệ");
        CheckoutResult result = checkoutService.placeOrder(customer, form);
        return result.orderCode();
    }

    private void complete(String superAdmin, String customerEmail, String orderCode) {
        signIn(superAdmin);
        long id = orderId(orderCode);
        orderManagementService.transition(id, OrderStatus.PROCESSING);
        ship(orderCode);
        orderManagementService.transition(id, OrderStatus.DELIVERED);
        ShopperIdentity customer = ShopperIdentity.forCustomer(customerId(customerEmail));
        customerOrderService.confirmReceived(orderCode, customer);
    }

    private void ship(String orderCode) {
        String tracking = "GHN" + orderId(orderCode) + "VN";
        orderManagementService.ship(orderId(orderCode), new ShipmentForm(ShippingCarrier.GHN, tracking, today.plusDays(2)));
    }

    private void writeReviews(String superAdmin) {
        review("tran.binh@example.test", 5, "Bánh lạp xưởng XO thơm, nhân nhiều, giao hàng nhanh. Sẽ đặt thêm làm quà Trung thu.");
        review("le.chi@example.test", 5, "Hộp quà Lam Phúc rất sang, đóng gói cẩn thận. Người nhận rất thích.");
        review("pham.dung@example.test", 4, "Bánh hạt sen mắc ca ngon, vị vừa phải. Mong shop có thêm hộp 8 bánh.");
        signIn(superAdmin);
        publishReviewOf("tran.binh@example.test");
        publishReviewOf("le.chi@example.test");
    }

    private void review(String email, int rating, String content) {
        signIn(email);
        FeedbackForm form = new FeedbackForm();
        form.setRating(rating);
        form.setContent(content);
        feedbackService.submit(form);
    }

    private void publishReviewOf(String email) {
        long feedbackId = jdbc.queryForObject("""
            SELECT TOP 1 f.feedback_id FROM dbo.FEEDBACK f
            WHERE f.customer_id = ? ORDER BY f.feedback_id DESC
            """, Long.class, customerId(email));
        feedbackService.publish(feedbackId);
    }

    private void shiftDays(String orderCode, int days) {
        long id = orderId(orderCode);
        jdbc.update("""
            UPDATE dbo.SALES_ORDER SET
                created_at = DATEADD(day, -?, created_at),
                updated_at = DATEADD(day, -?, updated_at),
                processing_at = DATEADD(day, -?, processing_at),
                shipping_at = DATEADD(day, -?, shipping_at),
                delivered_at = DATEADD(day, -?, delivered_at),
                customer_confirmed_at = DATEADD(day, -?, customer_confirmed_at),
                completed_at = DATEADD(day, -?, completed_at),
                cancelled_at = DATEADD(day, -?, cancelled_at),
                estimated_delivery_date = DATEADD(day, -?, estimated_delivery_date)
            WHERE order_id = ?
            """, days, days, days, days, days, days, days, days, days, id);
        jdbc.update("""
            UPDATE dbo.PAYMENT SET
                created_at = DATEADD(day, -?, created_at),
                updated_at = DATEADD(day, -?, updated_at),
                paid_at = DATEADD(day, -?, paid_at)
            WHERE order_id = ?
            """, days, days, days, id);
        jdbc.update("UPDATE dbo.STOCK_MOVEMENT SET created_at = DATEADD(day, -?, created_at) WHERE order_id = ?", days, id);
    }

    private void checkEverything() {
        Integer mismatched = jdbc.queryForObject("""
            SELECT COUNT(*) FROM dbo.INVENTORY_BALANCE b
            WHERE b.available_quantity <> COALESCE(
                (SELECT SUM(s.remaining_quantity) FROM dbo.STOCK_BATCH s WHERE s.variant_id = b.variant_id), 0)
            """, Integer.class);
        assertThat(mismatched).as("Tổng lô phải bằng tồn kho").isZero();

        Integer undatedFood = jdbc.queryForObject("""
            SELECT COUNT(*) FROM dbo.STOCK_BATCH s
            JOIN dbo.PRODUCT_VARIANT v ON v.variant_id = s.variant_id
            JOIN dbo.PRODUCT p ON p.product_id = v.product_id
            WHERE s.remaining_quantity > 0 AND s.expires_on IS NULL AND p.product_type <> 'EMPTY_BOX'
            """, Integer.class);
        assertThat(undatedFood).as("Không được có lô bánh hoặc hộp quà thiếu hạn").isZero();

        Integer orders = jdbc.queryForObject("SELECT COUNT(*) FROM dbo.SALES_ORDER", Integer.class);
        assertThat(orders).isEqualTo(10);
    }

    private Map<String, Integer> items(Object... skuAndQuantity) {
        Map<String, Integer> items = new LinkedHashMap<>();
        for (int i = 0; i < skuAndQuantity.length; i += 2) {
            items.put((String) skuAndQuantity[i], (Integer) skuAndQuantity[i + 1]);
        }
        return items;
    }

    private void signIn(String email) {
        UserDetails user = userDetailsService.loadUserByUsername(email);
        UsernamePasswordAuthenticationToken authentication =
            UsernamePasswordAuthenticationToken.authenticated(user, null, user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private long variant(String sku) {
        return jdbc.queryForObject("SELECT variant_id FROM dbo.PRODUCT_VARIANT WHERE sku = ?", Long.class, sku);
    }

    private long customerId(String email) {
        return jdbc.queryForObject("""
            SELECT c.customer_id FROM dbo.CUSTOMER c
            JOIN dbo.ACCOUNT a ON a.account_id = c.account_id
            WHERE a.email = ?
            """, Long.class, email);
    }

    private long orderId(String orderCode) {
        return jdbc.queryForObject("SELECT order_id FROM dbo.SALES_ORDER WHERE order_code = ?", Long.class, orderCode);
    }

    private String key() {
        return UUID.randomUUID().toString();
    }
}
