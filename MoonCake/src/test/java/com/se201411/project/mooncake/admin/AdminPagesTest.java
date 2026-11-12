package com.se201411.project.mooncake.admin;

import com.se201411.project.mooncake.enums.ShippingCarrier;
import com.se201411.project.mooncake.order.dto.OrderFulfilmentView;
import com.se201411.project.mooncake.enums.OrderStatus;
import com.se201411.project.mooncake.product.dto.ProductFormOptions;
import com.se201411.project.mooncake.product.dto.ProductEditPageView;
import com.se201411.project.mooncake.product.dto.NewProductPageView;
import com.se201411.project.mooncake.product.dto.MediaPageView;
import com.se201411.project.mooncake.product.dto.AdminCatalogPageView;
import com.se201411.project.mooncake.inventory.dto.MovementsPageView;
import com.se201411.project.mooncake.order.dto.AdminOrderListPageView;
import com.se201411.project.mooncake.dashboard.controller.DashboardController;
import com.se201411.project.mooncake.dashboard.dto.DashboardPeriod;
import com.se201411.project.mooncake.dashboard.dto.DashboardTaskView;
import com.se201411.project.mooncake.dashboard.dto.DashboardView;
import com.se201411.project.mooncake.dashboard.dto.DiscountSummaryView;
import com.se201411.project.mooncake.dashboard.dto.KpiView;
import com.se201411.project.mooncake.dashboard.dto.RecentOrderView;
import com.se201411.project.mooncake.dashboard.dto.TopProductView;
import com.se201411.project.mooncake.dashboard.service.DashboardService;
import com.se201411.project.mooncake.product.dto.AdminVideoView;
import com.se201411.project.mooncake.inventory.dto.AdminBatchView;
import com.se201411.project.mooncake.inventory.dto.InventoryPageView;
import com.se201411.project.mooncake.feedback.controller.AdminFeedbackController;
import com.se201411.project.mooncake.feedback.dto.AdminFeedbackView;
import com.se201411.project.mooncake.feedback.service.FeedbackService;
import com.se201411.project.mooncake.product.controller.AdminCatalogController;
import com.se201411.project.mooncake.inventory.controller.AdminInventoryController;
import com.se201411.project.mooncake.order.controller.AdminOrderController;
import com.se201411.project.mooncake.product.service.*;
import com.se201411.project.mooncake.inventory.service.*;
import com.se201411.project.mooncake.order.service.OrderManagementService;
import com.se201411.project.mooncake.order.service.OrderCancellationService;
import com.se201411.project.mooncake.product.dto.*;
import com.se201411.project.mooncake.product.dto.AdminCatalogForms.*;
import com.se201411.project.mooncake.inventory.dto.AdminInventoryView.*;
import com.se201411.project.mooncake.order.dto.AdminOrderTab;
import com.se201411.project.mooncake.order.dto.AdminOrderView;
import com.se201411.project.mooncake.enums.*;
import com.se201411.project.mooncake.common.exception.AdminExceptionAdvice;
import com.se201411.project.mooncake.common.exception.BusinessRuleViolationException;
import com.se201411.project.mooncake.order.dto.CustomerOpenOrderView;
import com.se201411.project.mooncake.security.config.SecurityConfig;
import com.se201411.project.mooncake.security.controller.AdminCustomerController;
import com.se201411.project.mooncake.security.dto.AdminCustomerDetailPageView;
import com.se201411.project.mooncake.security.dto.AdminCustomerListPageView;
import com.se201411.project.mooncake.security.dto.AdminCustomerRow;
import com.se201411.project.mooncake.security.service.CustomerAdminService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import org.springframework.mock.web.MockMultipartFile;
import static org.mockito.Mockito.*;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = {AdminPagesTest.Config.class, SecurityConfig.class})
class AdminPagesTest {
    @Autowired
    WebApplicationContext context;
    @Autowired
    ProductAdminService catalog;
    @Autowired
    PresetConfigurationAdminService presets;
    @Autowired
    InventoryAdminService inventory;
    @Autowired
    OrderManagementService orders;
    @Autowired
    OrderCancellationService cancellations;
    @Autowired DashboardService dashboards;
    @Autowired GiftBoxAdminService giftBoxes;
    @Autowired ProductMediaService media;
    @Autowired BatchStockService batches;
    @Autowired
    CustomerAdminService customers;
    MockMvc mvc;

    @BeforeEach
    void setup() {
        reset(catalog, presets, inventory, orders, cancellations, media, customers);
        when(dashboards.dashboard(any())).thenReturn(sampleDashboard());
        when(batches.list()).thenReturn(List.of(new AdminBatchView(1L, "SKU", "Bánh Kim Sa · Bánh 80g", "LOT", null, null, 3, "Chưa có hạn dùng", "muted", true)));
        when(media.allVideos()).thenReturn(List.of(new AdminVideoView(1L, "Cake", "https://example.test/cake.mp4", "Video")));
        mvc = webAppContextSetup(context).apply(springSecurity()).build();
        when(catalog.catalog()).thenReturn(new AdminCatalogView(
                List.of(new CategoryForm(1L, "Category", "category", true)),
                List.of(new ProductForm(1L, 1, ProductType.MOONCAKE, "Cake", "cake", "Description", null, null, null, null, true)),
                List.of(new VariantForm(1L, 1, "SKU", "Cake", 100, null, null, null, BigDecimal.TEN, true)),
                List.of(new BoxForm(1L, 1, 4, null, null, true)),
                List.of(new ImageForm(1L, 1, "/images/cake.png", "Cake", 0, true))));
        when(catalog.productRows(null)).thenReturn(List.of(new AdminProductRow(
                1L, "Cake", "Category", "Bánh lẻ", 1, BigDecimal.TEN, BigDecimal.TEN, "/images/cake.png", "Đang bán trên web", "success")));
        when(catalog.productTabs(null)).thenReturn(List.of(new AdminProductTab(null, "Tất cả", 1, true)));
        when(catalog.categories()).thenReturn(List.of(new CategoryForm(1L, "Category", "category", true)));
        when(catalog.productTypes()).thenReturn(List.of(new ProductTypeOption(ProductType.MOONCAKE, "Bánh lẻ")));
        when(catalog.productDetail(1)).thenReturn(new AdminProductDetail(
                new ProductForm(1L, 1, ProductType.MOONCAKE, "Cake", "cake", "Description", null, null, null, null, true),
                "Bánh lẻ",
                true,
                false,
                "Đang bán trên web",
                "success",
                null,
                List.of(new AdminProductDetail.Sku(
                        new VariantForm(1L, 1, "SKU", "Cake", 100, null, null, null, BigDecimal.TEN, true),
                        "/images/cake.png",
                        List.of(new ImageForm(1L, 1, "/images/cake.png", "Cake", 0, true)),
                        12,
                        "Đang bán trên web",
                        "success",
                        null,
                        null))));
        when(presets.presetOptions()).thenReturn(new AdminPresetOptions(
                List.of(new AdminPresetOptions.Option(5, "Hộp quà mới · Hộp 4 bánh", null, null, 0, false, BigDecimal.valueOf(590_000))),
                List.of(new AdminPresetOptions.Option(1, "Vỏ hộp Kim Phúc 4 bánh", "4 bánh", "/images/box.png", 10, true, BigDecimal.valueOf(40_000))),
                List.of(new AdminPresetOptions.Option(1, "Bánh Kim Sa", "Bánh 80g", "/images/cake.png", 120, true, BigDecimal.valueOf(65_000)))));
        when(presets.presets()).thenReturn(List.of(
                new AdminPresetView(1, 1, "GIFT", 1, 4, ConfigurationStatus.DRAFT, List.of(new AdminPresetView.Component(1, "CAKE", 4, "Bánh Kim Sa · 80g")), "Hộp quà Kim Phúc · Hộp 4 bánh", "Vỏ hộp Kim Phúc · 4 bánh"),
                new AdminPresetView(2, 2, "GIFT-READY", 1, 4, ConfigurationStatus.READY, List.of(), "Hộp quà Lam Phúc · Hộp 6 bánh", "Vỏ hộp Lam Phúc · 6 bánh")));
        when(inventory.balances()).thenReturn(List.of(new Balance(1, "SKU", "Cake", "Cake 80g", 3, 5)));
        when(inventory.balances(any())).thenReturn(List.of(new Balance(1, "SKU", "Cake", "Cake 80g", 3, 5)));
        when(inventory.stockTabs(any())).thenReturn(List.of());
        when(inventory.inventoryPage(any(), any(), any(), any(), any())).thenReturn(new InventoryPageView(
                StockFilter.ALL,
                List.of(),
                List.of(new Balance(1, "SKU", "Cake", "Cake 80g", 3, 5)),
                List.of(),
                "receive-key",
                "adjust-key",
                "pack-key",
                null,
                null,
                null,
                null));
        when(inventory.movements(0)).thenReturn(List.of(new Movement("SKU", StockMovementType.RECEIVE, 3, "Receive", "GROUP", 1L, LocalDateTime.now(), "Cake · Cake 80g", "Nhập kho", "09/11/2026 10:00")));
        when(orders.orders(null, 0)).thenReturn(List.of(order(OrderStatus.PLACED)));
        when(orders.detail(1)).thenReturn(order(OrderStatus.PLACED));
        when(orders.statusTabs(null)).thenReturn(List.of(new AdminOrderTab(null, "Tất cả", 1, true)));
        stubPageViews();
    }

    private void stubPageViews() {
        when(orders.ordersPage(any(), anyInt())).thenAnswer(call -> {
            OrderStatus status = call.getArgument(0);
            int page = call.getArgument(1);
            return new AdminOrderListPageView(orders.orders(status, page), orders.statusTabs(status), status, page);
        });
        when(inventory.movementsPage(anyInt())).thenAnswer(call -> {
            int page = call.getArgument(0);
            return new MovementsPageView(inventory.movements(page), page);
        });
        when(catalog.catalogPage(any())).thenAnswer(call -> new AdminCatalogPageView(
            catalog.productRows(call.getArgument(0)),
            catalog.productTabs(call.getArgument(0)),
            catalog.categories()));
        when(catalog.formOptions(anyBoolean())).thenAnswer(call -> formOptions(call.getArgument(0)));
        when(catalog.newProductPage(anyBoolean())).thenAnswer(call -> new NewProductPageView(formOptions(call.getArgument(0)), List.of()));
        doAnswer(call -> new NewProductPageView(formOptions(true), presets.presetOptions().boxes())).when(giftBoxes).newPage();
        when(catalog.productEditPage(anyLong())).thenAnswer(call -> {
            long id = call.getArgument(0);
            return new ProductEditPageView(catalog.productDetail(id), formOptions(false));
        });
        when(media.mediaPage()).thenAnswer(call -> new MediaPageView(media.allVideos(), catalog.catalog()));
    }

    private ProductFormOptions formOptions(boolean giftBox) {
        return new ProductFormOptions(catalog.categories(), catalog.productTypes(), giftBox);
    }

    @Test
    void rendersAllPagesWithRealThymeleafAndCsrf() throws Exception {
        for (String path : List.of("/admin/catalog", "/admin/catalog/products/new", "/admin/catalog/products/1", "/admin/inventory", "/admin/orders", "/admin/orders/1"))
            mvc.perform(get(path).with(user("admin").roles("ADMIN")))
                    .andExpect(status().isOk()).andExpect(content().string(containsString("_csrf")));
    }

    @Test void newAdminPagesRenderAndDenyCustomerAccess() throws Exception {
        for(String path:List.of("/admin/overview","/admin/media","/admin/batches")){
            mvc.perform(get(path).with(user("admin").roles("ADMIN"))).andExpect(status().isOk());
            mvc.perform(get(path).with(user("customer").roles("CUSTOMER"))).andExpect(status().isForbidden());
        }
    }

    @Test
    void rendersCustomerListAndDetailPages() throws Exception {
        AdminCustomerRow active = new AdminCustomerRow(
            7L,
            "customer@example.com",
            "Nguyễn Văn A",
            "0901234567",
            "01/10/2026",
            true,
            12,
            3
        );
        when(customers.customersPage(any(), anyInt())).thenReturn(new AdminCustomerListPageView(
            List.of(active),
            "customer",
            0,
            true
        ));
        when(customers.customerPage(7L)).thenReturn(new AdminCustomerDetailPageView(
            active,
            List.of(
                new CustomerOpenOrderView(1L, "MC-1", "Chờ xử lý", "COD · Chưa thanh toán", "250.000đ", true),
                new CustomerOpenOrderView(2L, "MC-2", "Đang giao", "COD · Chưa thanh toán", "300.000đ", false)
            ),
            1
        ));

        mvc.perform(get("/admin/customers").param("q", "customer").with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("customer@example.com")))
            .andExpect(content().string(containsString("12 đơn · 3 đã hủy")))
            .andExpect(content().string(containsString("Đang hoạt động")))
            .andExpect(content().string(containsString("Trang sau →")))
            .andExpect(content().string(containsString("href=\"/admin/customers\"")));

        mvc.perform(get("/admin/customers/7").with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Hủy luôn các đơn chưa thanh toán đang chờ xử lý (1 đơn)")))
            .andExpect(content().string(containsString("Khóa tài khoản")))
            .andExpect(content().string(containsString("Xử lý thủ công")))
            .andExpect(content().string(containsString("data-confirm-message")))
            .andExpect(content().string(containsString("_csrf")));
    }

    @Test
    void lockedCustomerDetailOffersOnlyUnlock() throws Exception {
        AdminCustomerRow locked = new AdminCustomerRow(
            7L,
            "customer@example.com",
            "Nguyễn Văn A",
            "0901234567",
            "01/10/2026",
            false,
            0,
            0
        );
        when(customers.customerPage(7L)).thenReturn(new AdminCustomerDetailPageView(locked, List.of(), 0));

        mvc.perform(get("/admin/customers/7").with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Mở khóa tài khoản")))
            .andExpect(content().string(not(containsString("Hủy luôn các đơn"))));
    }

    @Test
    void orderListShowsLockedCustomerLabel() throws Exception {
        when(orders.orders(any(), anyInt())).thenReturn(List.of(order(OrderStatus.PLACED, true)));

        mvc.perform(get("/admin/orders").with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Khách đã bị khóa")));
    }

    @Test
    void adminSuccessFlashUsesSharedDismissibleMessage() throws Exception {
        mvc.perform(get("/admin/orders")
                .with(user("admin").roles("ADMIN"))
                .flashAttr("success", "Đã cập nhật đơn hàng."))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("data-flash-message")))
            .andExpect(content().string(containsString("data-flash-timeout=\"5000\"")))
            .andExpect(content().string(containsString("/js/flash-message.js")));
    }

    @Test
    void catalogRendersMarkersForUncheckedCheckboxes() throws Exception {
        mvc.perform(get("/admin/catalog").with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("name=\"_active\"")));
        mvc.perform(get("/admin/catalog/products/1").with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("name=\"_primary\"")))
            .andExpect(content().string(containsString("/admin/catalog/products/1/visibility")))
            .andExpect(content().string(containsString("/admin/variants/1/sale")))
            .andExpect(content().string(containsString("receive=1")));
    }

    @Test
    void productListLinksToSeparateEditPage() throws Exception {
        mvc.perform(get("/admin/catalog").with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("/admin/catalog/products/1")))
            .andExpect(content().string(containsString("Đang bán trên web")))
            .andExpect(content().string(containsString("tabs__count")));
    }

    @Test
    void creatingGiftBoxWithBoxPriceAndImageGoesToCakeStep() throws Exception {
        when(giftBoxes.create(any(), any())).thenReturn(9L);
        mvc.perform(post("/admin/catalog/products")
                .with(user("admin").roles("ADMIN"))
                .with(csrf())
                .param("categoryId", "1")
                .param("productType", "GIFT_SET")
                .param("name", "Hộp quà mới")
                .param("slug", "hop-qua-moi")
                .param("shortDescription", "Mô tả")
                .param("_active", "on")
                .param("variantName", "Hộp 4 bánh")
                .param("unitPrice", "450000")
                .param("sku", "GIFT-NEW")
                .param("boxId", "1")
                .param("imageUrl", "/images/products/gift.png"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/admin/gift-boxes/9#recipe"));
        verify(giftBoxes).create(new NewProductForm(
            1, ProductType.GIFT_SET, "Hộp quà mới", "hop-qua-moi", "Mô tả", null, null, null, null, false,
            "GIFT-NEW", "Hộp 4 bánh", BigDecimal.valueOf(450_000), null, null, null, null, "/images/products/gift.png", null), 1L);
        verify(catalog, never()).createProduct(any());
    }

    @Test
    void creatingProductRejectsBadImageUrl() throws Exception {
        mvc.perform(post("/admin/catalog/products")
                .with(user("admin").roles("ADMIN"))
                .with(csrf())
                .param("categoryId", "1")
                .param("productType", "GIFT_SET")
                .param("name", "Hộp quà mới")
                .param("slug", "hop-qua-moi")
                .param("shortDescription", "Mô tả")
                .param("_active", "on")
                .param("variantName", "Hộp 4 bánh")
                .param("unitPrice", "450000")
                .param("sku", "GIFT-NEW")
                .param("imageUrl", "javascript:alert(1)"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/admin/gift-boxes/new#new-product"))
            .andExpect(flash().attribute("formError", "Đường dẫn ảnh phải bắt đầu bằng https://, /images/ hoặc /uploads/."))
            .andExpect(flash().attributeExists("formValues"));
        verify(catalog, never()).createProduct(any());
        verify(giftBoxes, never()).create(any(), any());
    }

    @Test
    void uploadingImageReturnsToTheSkuOnItsProductPage() throws Exception {
        when(catalog.uploadImage(any())).thenReturn(4L);
        MockMultipartFile photo = new MockMultipartFile("imageFile", "cake.png", "image/png", new byte[] {1, 2, 3});
        mvc.perform(multipart("/admin/images/upload")
                .file(photo)
                .with(user("admin").roles("ADMIN"))
                .with(csrf())
                .param("productId", "4")
                .param("variantId", "2")
                .param("sortOrder", "1")
                .param("_primary", "on"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/admin/catalog/products/4#sku-2"));
        verify(catalog).uploadImage(any());
    }

    @Test
    void deletingProductReturnsToListWithItsName() throws Exception {
        when(catalog.deleteProduct(5)).thenReturn("Bánh thử");
        mvc.perform(post("/admin/catalog/products/5/delete")
                .with(user("admin").roles("ADMIN"))
                .with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/admin/catalog"))
            .andExpect(flash().attribute("success", "Đã xóa Bánh thử."));
    }

    @Test
    void deletingVariantReturnsToItsProduct() throws Exception {
        when(catalog.deleteVariant(8)).thenReturn(3L);
        mvc.perform(post("/admin/variants/8/delete")
                .with(user("admin").roles("ADMIN"))
                .with(csrf())
                .param("productId", "3"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/admin/catalog/products/3#skus"));
    }

    @Test
    void editPageOffersDeleteOnlyWithoutBlocker() throws Exception {
        mvc.perform(get("/admin/catalog/products/1").with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("/admin/catalog/products/1/delete")))
            .andExpect(content().string(containsString("/admin/variants/1/delete")));
    }

    @Test
    void variantRuleErrorReturnsToTheSameCardWithTypedValues() throws Exception {
        doThrow(new BusinessRuleViolationException("Bánh lẻ cần nhập khối lượng."))
            .when(catalog).saveVariant(any());
        mvc.perform(post("/admin/variants")
                .with(user("admin").roles("ADMIN"))
                .with(csrf())
                .param("productId", "1")
                .param("sku", "MC-NEW")
                .param("name", "Bánh 150g")
                .param("unitPrice", "65000")
                .param("_active", "on"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/admin/catalog/products/1#sku-new"))
            .andExpect(flash().attribute("formError", "Bánh lẻ cần nhập khối lượng."))
            .andExpect(flash().attribute("formValues", new VariantForm(null, 1, "MC-NEW", "Bánh 150g", null, null, null, null, BigDecimal.valueOf(65_000), false)));
    }

    @Test
    void editPageShowsErrorInsideTheFailedSection() throws Exception {
        mvc.perform(get("/admin/catalog/products/1")
                .with(user("admin").roles("ADMIN"))
                .flashAttr("formTarget", "sku-new")
                .flashAttr("formError", "Bánh lẻ cần nhập khối lượng.")
                .flashAttr("formValues", new VariantForm(null, 1, "MC-NEW", "Bánh 150g", null, null, null, null, BigDecimal.valueOf(65_000), false)))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("class=\"form-alert\"")))
            .andExpect(content().string(containsString("Bánh lẻ cần nhập khối lượng.")))
            .andExpect(content().string(containsString("value=\"MC-NEW\"")));
    }

    @Test
    void savingProductRedirectsToItsEditPage() throws Exception {
        when(catalog.saveProduct(any())).thenReturn(7L);
        mvc.perform(post("/admin/products")
                .with(user("admin").roles("ADMIN"))
                .with(csrf())
                .param("categoryId", "1")
                .param("productType", "MOONCAKE")
                .param("name", "New Product")
                .param("slug", "new-product")
                .param("shortDescription", "Description")
                .param("_active", "on"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/admin/catalog/products/7"));
    }

    @Test
    void anonymousAndCustomerCannotReadAdminPages() throws Exception {
        mvc.perform(get("/admin/catalog")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/admin/catalog").with(user("customer").roles("CUSTOMER"))).andExpect(status().isForbidden());
        verifyNoInteractions(catalog);
    }

    @Test
    void postsRequireAdminAndCsrf() throws Exception {
        mvc.perform(post("/admin/orders/1/status").param("target", "PROCESSING").with(user("admin").roles("ADMIN"))).andExpect(status().isForbidden());
        mvc.perform(post("/admin/orders/1/status").param("target", "PROCESSING").with(csrf()).with(user("customer").roles("CUSTOMER"))).andExpect(status().isForbidden());
        verifyNoInteractions(orders);
    }

    @Test
    void cancellationPostUsesSharedCancellationService() throws Exception {
        mvc.perform(post("/admin/orders/1/status")
                .param("target", "CANCELLED")
                .with(user("admin").roles("ADMIN"))
                .with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/admin/orders/1"));

        verify(cancellations).cancelForAdmin(1L);
        verify(orders, never()).transition(1L, OrderStatus.CANCELLED);
    }

    @Test
    void bindsRecordFormAndRejectsInvalidInput() throws Exception {
        mvc.perform(post("/admin/categories").with(user("admin").roles("ADMIN")).with(csrf())
                        .param("name", "New Category").param("slug", "new-category").param("active", "true"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/admin/catalog#categories"));
        verify(catalog).saveCategory(new CategoryForm(null, "New Category", "new-category", true));
        mvc.perform(post("/admin/categories").with(user("admin").roles("ADMIN")).with(csrf())
                .param("name", "").param("slug", "bad slug"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/catalog#category-new"))
                .andExpect(flash().attribute("formTarget", "category-new"))
                .andExpect(flash().attributeExists("formError"));
        verifyNoMoreInteractions(catalog);
    }

    @Test
    void uncheckedCatalogCheckboxesBindAsFalse() throws Exception {
        mvc.perform(post("/admin/categories")
                .with(user("admin").roles("ADMIN"))
                .with(csrf())
                .param("name", "Inactive Category")
                .param("slug", "inactive-category")
                .param("_active", "on"))
            .andExpect(status().is3xxRedirection());
        verify(catalog).saveCategory(new CategoryForm(
            null,
            "Inactive Category",
            "inactive-category",
            false
        ));

        mvc.perform(post("/admin/products")
                .with(user("admin").roles("ADMIN"))
                .with(csrf())
                .param("categoryId", "1")
                .param("productType", "MOONCAKE")
                .param("name", "Inactive Product")
                .param("slug", "inactive-product")
                .param("shortDescription", "Description")
                .param("_active", "on"))
            .andExpect(status().is3xxRedirection());
        verify(catalog).saveProduct(new ProductForm(
            null,
            1,
            ProductType.MOONCAKE,
            "Inactive Product",
            "inactive-product",
            "Description",
            null,
            null,
            null,
            null,
            false
        ));

        mvc.perform(post("/admin/variants")
                .with(user("admin").roles("ADMIN"))
                .with(csrf())
                .param("productId", "1")
                .param("sku", "INACTIVE-SKU")
                .param("name", "Inactive Variant")
                .param("unitPrice", "100000")
                .param("_active", "on"))
            .andExpect(status().is3xxRedirection());
        verify(catalog).saveVariant(new VariantForm(
            null,
            1,
            "INACTIVE-SKU",
            "Inactive Variant",
            null,
            null,
            null,
            null,
            BigDecimal.valueOf(100_000),
            false
        ));

        mvc.perform(post("/admin/images")
                .with(user("admin").roles("ADMIN"))
                .with(csrf())
                .param("productId", "1")
                .param("variantId", "1")
                .param("imageUrl", "/images/cake.png")
                .param("sortOrder", "0")
                .param("_primary", "on"))
            .andExpect(status().is3xxRedirection());
        verify(catalog).saveImage(new ImageForm(
            null,
            1,
            "/images/cake.png",
            null,
            0,
            false
        ));
    }

    @Test
    void ordersPageShowsStatusTabsAndPaymentColumn() throws Exception {
        mvc.perform(get("/admin/orders").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("tabs__count")))
                .andExpect(content().string(containsString("Chưa thanh toán")));
    }

    @Test
    void inventoryShowsStockRowsWithActionsAndHistoryPage() throws Exception {
        mvc.perform(get("/admin/inventory").with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Sắp hết")))
            .andExpect(content().string(containsString("data-open-modal=\"receive-dialog\"")));
        mvc.perform(get("/admin/inventory/movements").with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Nhập kho")))
            .andExpect(content().string(containsString("+3")));
    }

    @Test
    void inventoryRowWithExpiredStockOffersWriteOff() throws Exception {
        when(inventory.inventoryPage(any(), any(), any(), any(), any())).thenReturn(new InventoryPageView(
                StockFilter.ALL,
                List.of(),
                List.of(new Balance(1, "SKU", "Cake", "Cake 80g", 9, 5, 4, false, true)),
                List.of(),
                "receive-key",
                "adjust-key",
                "pack-key",
                null,
                null,
                null,
                null));
        mvc.perform(get("/admin/inventory").with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("trong đó 4 hết hạn")))
            .andExpect(content().string(containsString("/admin/inventory/expired")));
    }

    @Test
    void stockErrorReopensTheDialogInsteadOfErrorPage() throws Exception {
        doThrow(new BusinessRuleViolationException("Số lượng bớt đi phải lớn hơn 0. Muốn thêm hàng hãy dùng Nhập kho."))
            .when(inventory).adjust(any());
        mvc.perform(post("/admin/inventory/adjust")
                .with(user("admin").roles("ADMIN"))
                .with(csrf())
                .param("variantId", "1")
                .param("quantity", "0")
                .param("reason", "Kiểm kê")
                .param("operationKey", "123e4567-e89b-12d3-a456-426614174000"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/admin/inventory"))
            .andExpect(flash().attribute("dialogId", "adjust-dialog"))
            .andExpect(flash().attribute("dialogError", "Số lượng bớt đi phải lớn hơn 0. Muốn thêm hàng hãy dùng Nhập kho."));
    }

    @Test
    void newProductAndNewGiftBoxPagesRenderFromOnePageView() throws Exception {
        mvc.perform(get("/admin/catalog/products/new").with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Thêm sản phẩm")))
            .andExpect(content().string(containsString("name=\"categoryId\"")));

        mvc.perform(get("/admin/gift-boxes/new").with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Thêm hộp quà")))
            .andExpect(content().string(containsString("value=\"GIFT_SET\"")))
            .andExpect(content().string(containsString("Chọn vỏ hộp")))
            .andExpect(content().string(containsString("name=\"boxId\"")))
            .andExpect(content().string(containsString("Tạo hộp quà")))
            .andExpect(content().string(not(containsString("Hiển thị trên cửa hàng ngay"))));
    }

    @Test
    void giftBoxPageShowsStepTabsCakeCardsAndInlineErrors() throws Exception {
        AdminProductDetail detail = new AdminProductDetail(
                new ProductForm(2L, 1, ProductType.GIFT_SET, "Hộp quà Kim Phúc", "hop-qua-kim-phuc", "Mô tả", null, null, null, null, false),
                "Hộp quà",
                true,
                false,
                "Chưa có công thức",
                "warning",
                null,
                List.of(new AdminProductDetail.Sku(
                        new VariantForm(1L, 2, "GIFT-KP-4", "Hộp 4 bánh", null, null, null, null, BigDecimal.valueOf(590_000), false),
                        null,
                        List.of(),
                        0,
                        "Ngừng bán",
                        "muted",
                        null,
                        null)));
        AdminPresetView recipe = new AdminPresetView(
                1, 1, "GIFT-KP-4", 1, 4, ConfigurationStatus.DRAFT,
                List.of(new AdminPresetView.Component(1, "CAKE", 3, "Bánh Kim Sa · Bánh 80g")),
                "Hộp quà Kim Phúc · Hộp 4 bánh", "Vỏ hộp Kim Phúc 4 bánh");
        ProductFormOptions giftBoxOptions = formOptions(true);
        AdminPresetOptions presetOptions = presets.presetOptions();
        when(giftBoxes.page(2)).thenReturn(new GiftBoxPage(
                detail,
                detail.skus().get(0),
                recipe,
                List.of(
                        new GiftBoxPage.Step(1, "Thông tin & giá", "info", true),
                        new GiftBoxPage.Step(2, "Chọn bánh cho hộp", "recipe", false),
                        new GiftBoxPage.Step(3, "Đóng hộp & mở bán", "sale", false)),
                "recipe",
                false,
                false,
                giftBoxOptions,
                presetOptions));
        mvc.perform(get("/admin/gift-boxes/2").with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("data-default=\"recipe\"")))
            .andExpect(content().string(containsString("name=\"quantities[1]\"")))
            .andExpect(content().string(containsString("/images/cake.png")))
            .andExpect(content().string(containsString("Xong, chốt danh sách")))
            .andExpect(content().string(containsString("Kho: 120")));

        when(catalog.productDetail(2)).thenReturn(detail);
        mvc.perform(get("/admin/catalog/products/2").with(user("admin").roles("ADMIN")))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/admin/gift-boxes/2"));

        doThrow(new BusinessRuleViolationException("Hộp chỉ chứa 4 bánh, đang chọn 5 bánh."))
            .when(presets).setComponents(eq(1L), any());
        mvc.perform(post("/admin/gift-boxes/2/cakes")
                .with(user("admin").roles("ADMIN"))
                .with(csrf())
                .param("recipeId", "1")
                .param("quantities[1]", "5"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/admin/gift-boxes/2#recipe"))
            .andExpect(flash().attribute("formError", "Hộp chỉ chứa 4 bánh, đang chọn 5 bánh."));
    }

    @Test
    void oldRecipePageRedirectsToGiftBoxes() throws Exception {
        mvc.perform(get("/admin/presets").with(user("admin").roles("ADMIN")))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/admin/gift-boxes"));
    }

    @Test
    void shippingShowsOnlyDeliveryAction() throws Exception {
        when(orders.detail(1)).thenReturn(order(OrderStatus.SHIPPING));
        mvc.perform(get("/admin/orders/1").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("value=\"DELIVERED\"")))
                .andExpect(content().string(containsString("Đã giao hàng")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("value=\"CANCELLED\""))));
    }

    @Test
    void deliveredOrderShowsCustomerConfirmationNotice() throws Exception {
        when(orders.detail(1)).thenReturn(order(OrderStatus.DELIVERED));

        mvc.perform(get("/admin/orders/1").with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Chờ khách xác nhận")))
            .andExpect(content().string(containsString("Tự hoàn tất ngày")))
            .andExpect(content().string(org.hamcrest.Matchers.not(
                containsString("value=\"CANCELLED\"")
            )));
    }

    @Test
    void processingOrderShowsShipmentFormWithCarriers() throws Exception {
        when(orders.detail(1)).thenReturn(order(OrderStatus.PROCESSING));
        mvc.perform(get("/admin/orders/1").with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("/admin/orders/1/ship")))
            .andExpect(content().string(containsString("name=\"carrier\"")))
            .andExpect(content().string(containsString("Giao Hàng Nhanh")))
            .andExpect(content().string(containsString("name=\"estimatedDeliveryDate\"")));
    }

    @Test
    void shipmentWithoutCarrierReturnsToOrderWithError() throws Exception {
        mvc.perform(post("/admin/orders/1/ship")
                .with(user("admin").roles("ADMIN"))
                .with(csrf())
                .param("estimatedDeliveryDate", "2099-01-01"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/admin/orders/1"))
            .andExpect(flash().attribute("shipError", "Vui lòng chọn đơn vị vận chuyển."));
        verify(orders, never()).ship(anyLong(), any());
    }

    @Test
    void overviewRendersDashboardAndDiscountedOrders() throws Exception {
        mvc.perform(get("/admin/overview").param("period", "MONTH").with(user("admin").roles("ADMIN")))
          .andExpect(status().isOk())
          .andExpect(content().string(containsString("Đơn được chiết khấu số lượng")))
          .andExpect(content().string(containsString("MC-DISC")))
          .andExpect(content().string(org.hamcrest.Matchers.not(containsString("Khách hàng doanh nghiệp"))))
          .andExpect(content().string(containsString("Đơn chờ xử lý")))
          .andExpect(content().string(containsString("Bánh Tôm Hùm")))
          .andExpect(content().string(containsString("&lt;b&gt;Khách&lt;/b&gt;")))
          .andExpect(content().string(containsString("09\\/11")));
    }

    @Test
    void feedbackPageEscapesContentAndOffersPublish() throws Exception {
        FeedbackService feedbackService = context.getBean(FeedbackService.class);
        AdminFeedbackView view = new AdminFeedbackView(
            5L,
            "Customer",
            5,
            "<script>alert(1)</script>",
            "02/11/2026 10:00",
            false
        );
        when(feedbackService.findForAdmin()).thenReturn(List.of(view));

        mvc.perform(get("/admin/feedback").with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("&lt;script&gt;")))
            .andExpect(content().string(containsString("/admin/feedback/5/publish")));
    }

    static DashboardView sampleDashboard() {
        return new DashboardView(
            DashboardPeriod.DAY,
            List.of(new KpiView("Doanh thu", 620000L, true, 12.5, "so với hôm qua")),
            List.of("08/11", "09/11"),
            List.of(0L, 620000L),
            List.of(0L, 1L),
            List.of(new DashboardTaskView("Đơn chờ xử lý", 3L, "/admin/orders?status=PLACED", "warning")),
            List.of(new RecentOrderView(1L, "MC-1", "<b>Khách</b>", "620.000đ", "Chờ xử lý", "warning", "09/11 10:00")),
            List.of(new TopProductView("Bánh Tôm Hùm", 4L, new BigDecimal("440000"))),
            new DiscountSummaryView("Tháng này", 1L, 50000L, List.of(new DiscountSummaryView.Order(
                3L, "MC-DISC", "Khách sỉ", "50.000đ", "950.000đ", "09/11 11:00")))
        );
    }

    static AdminOrderView order(OrderStatus status) {
        return order(status, false);
    }

    static AdminOrderView order(OrderStatus status, boolean customerLocked) {
        boolean placed = status == OrderStatus.PLACED;
        boolean processing = status == OrderStatus.PROCESSING;
        boolean shipping = status == OrderStatus.SHIPPING;
        AdminOrderView.Actions actions = new AdminOrderView.Actions(
                placed, processing, shipping, placed || processing, false, false, false);
        return new AdminOrderView(1, "ORDER", status, "Chờ xử lý", "warning",
                "Recipient", "0901234567", "Address", null,
                BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.TEN, "09/11/2026 10:00", "09/11/2026 10:30",
                List.of(new AdminOrderView.Item("Cake", "SKU", 1, BigDecimal.TEN, BigDecimal.TEN, List.of(new AdminOrderView.Component("Cake", 1, ComponentRole.CAKE)))),
                List.of(new AdminOrderView.PaymentLine("COD", "Chưa thanh toán", false, BigDecimal.TEN, null)),
                actions,
                new OrderFulfilmentView(
                    "10:00 09/11/2026",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
                ),
                status == OrderStatus.DELIVERED ? "15/11/2026" : null,
                List.of(ShippingCarrier.values()),
                5L,
                customerLocked);
    }

    @Configuration
    @EnableWebMvc
    @Import({AdminCatalogController.class, com.se201411.project.mooncake.product.controller.AdminGiftBoxController.class, AdminInventoryController.class, AdminOrderController.class, DashboardController.class, com.se201411.project.mooncake.product.controller.ProductMediaController.class, com.se201411.project.mooncake.inventory.controller.BatchController.class, AdminFeedbackController.class, AdminCustomerController.class, AdminExceptionAdvice.class})
    static class Config {
        @Bean FeedbackService feedbackService(){return mock(FeedbackService.class);}
        @Bean GiftBoxAdminService giftBoxAdminService(){return mock(GiftBoxAdminService.class);}
        @Bean DashboardService dashboardService(){return mock(DashboardService.class);}
        @Bean ProductMediaService media(){return mock(ProductMediaService.class);}
        @Bean BatchStockService batches(){return mock(BatchStockService.class);}

        @Bean
        ProductAdminService catalog() {
            return mock(ProductAdminService.class);
        }

        @Bean
        PresetConfigurationAdminService presets() {
            return mock(PresetConfigurationAdminService.class);
        }

        @Bean
        InventoryAdminService inventory() {
            return mock(InventoryAdminService.class);
        }

        @Bean
        PackingService packing() {
            return mock(PackingService.class);
        }

        @Bean
        OrderManagementService orders() {
            return mock(OrderManagementService.class);
        }

        @Bean
        OrderCancellationService cancellations() {
            return mock(OrderCancellationService.class);
        }

        @Bean
        CustomerAdminService customers() {
            return mock(CustomerAdminService.class);
        }

        @Bean
        SpringTemplateEngine engine() {
            var resolver = new ClassLoaderTemplateResolver();
            resolver.setPrefix("templates/");
            resolver.setSuffix(".html");
            resolver.setCharacterEncoding("UTF-8");
            var engine = new SpringTemplateEngine();
            engine.setTemplateResolver(resolver);
            return engine;
        }

        @Bean
        ThymeleafViewResolver viewResolver(SpringTemplateEngine engine) {
            var resolver = new ThymeleafViewResolver();
            resolver.setTemplateEngine(engine);
            resolver.setCharacterEncoding("UTF-8");
            return resolver;
        }
    }
}
