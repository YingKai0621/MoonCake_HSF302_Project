package com.se201411.project.mooncake.controller;

import com.se201411.project.mooncake.product.dto.CatalogProductView;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller for rendering the official MoonCake UI pages.
 * Maps customer-facing Thymeleaf templates for desktop navigation.
 */
@Controller
public class PageController {

    private static final List<CatalogProductView> GIFT_BOXES = Arrays.asList(
        new CatalogProductView(
            "gift-1",
            "Hộp Quà Dạ Nguyệt Đoàn Viên",
            880000L,
            "880.000đ",
            "https://lh3.googleusercontent.com/aida/AEtjO1XEoJS_KKTzlPcn7I1MA0HHvV1pqYipmrI6QnHR5YfE1-cnE0BZRYcVsOrs7RGHJSBVUb9BzlHemPA68nobEArj8huKh2CCsWpRDkhCO9Tu5kJyFjSFNnw6yS6gNQt-JmX8ZV1xtiK_LkKHBUanymceZerbxGr0mc0CaMfnd-CdCrKYwxYG4ahoY7k68Xn6gM_3SqXgJOd4WLc5UHw4MZMjs4Ci71Dbg2S27NsKONDHR9kwapqckXywuw",
            "Hộp quà Trung Thu gồm 4 bánh cao cấp kèm Trà Sen Tây Hồ trong hộp sơn mài.",
            "gift-boxes",
            4,
            null,
            null,
            "/products/gift-1",
            "Hộp quà Trung Thu",
            "Hộp 4 bánh cao cấp kèm Trà Sen Tây Hồ trong hộp sơn mài",
            "Hộp quà Dạ Nguyệt Đoàn Viên được hoàn thiện trang nhã theo phong cách sơn mài truyền thống, bên trong gồm 4 bánh Trung Thu thượng hạng kết hợp cùng Trà Sen Tây Hồ thanh tao. Bộ sản phẩm phù hợp làm quà biếu trang trọng cho đối tác, người thân hoặc sum vầy cùng gia đình.",
            null,
            null,
            null
        ),
        new CatalogProductView(
            "gift-2",
            "Hộp Quà Tứ Quý Hoàng Kim",
            1250000L,
            "1.250.000đ",
            "https://lh3.googleusercontent.com/aida-public/AB6AXuAJ-Jp6tvfdbppSqd6QYCySbQS0l8lcysmPWbUCIzVN1vS-IRhfn1oB1HNPqWJ5L3Xq0nPGskEkG2OUKUM2zn01TRQVVzczevnzduFJYLR7g4ZSx6kKZKNknkTA_dTnFGkooy5r4MAvykihLJicKFe64_pOxrzx5HvSckPWUuDRxk-3zuHjGXCXEwQhStdaM1ao_QMpxfW6k6zdYTLbWWkJ378CZ4h2TegCFlcque5YT6JYPaBhka5nd4k2FAbHctsvOQOyDlKRuA",
            "Hộp quà Trung Thu gồm 4 bánh trọng lượng 200g kết hợp cùng Trà Shan Tuyết Cổ Thụ.",
            "gift-boxes",
            4,
            null,
            null,
            "/products/gift-2",
            "Hộp quà Trung Thu",
            "Hộp 4 bánh 200g kết hợp cùng Trà Shan Tuyết Cổ Thụ",
            "Hộp quà Tứ Quý Hoàng Kim gồm 4 bánh Trung Thu trọng lượng 200g, kết hợp cùng một hũ Trà Shan Tuyết Cổ Thụ. Thiết kế hộp gỗ vuông vức, mang sắc thái trang nhã, thích hợp để thưởng trà và ngắm trăng trong dịp lễ hội mùa thu.",
            null,
            null,
            null
        ),
        new CatalogProductView(
            "gift-3",
            "Hộp Quà Trăng Vàng Vọng Nguyệt",
            1850000L,
            "1.850.000đ",
            "https://lh3.googleusercontent.com/aida-public/AB6AXuCTF7I6iJlE8t2kPjkXxEdwKf-mej5mroCA8GpxXDW1BX4Bd_Ax6pMuhjf8LmCD35NCI5UJIhGNkFY9cG3o6daddrtYEzksmCklkdQwt-u15plyUGUQmHnmIIjc__nk4sZ6sGaJ9vGTQzkb1BFve_-nLTUMTN4bbAr7hYzbAwliF3fYkzWMh3BUYJYu4ZF-mLVj7ksaUp4IAVZeUvwAMf3LXK5Ft3nNwEK2RHBHtWoFnw7s7-tEW8bUo2jq_uGqRiMWC0ifg_pxSQ",
            "Hộp quà Trung Thu gồm 6 bánh kết hợp cùng Trà Ô Long Kim Tuyên.",
            "gift-boxes",
            6,
            null,
            null,
            "/products/gift-3",
            "Hộp quà Trung Thu",
            "Hộp 6 bánh kết hợp cùng Trà Ô Long Kim Tuyên",
            "Hộp quà Trăng Vàng Vọng Nguyệt gồm 6 bánh Trung Thu được tuyển chọn kỹ lưỡng, đi kèm hộp Trà Ô Long Kim Tuyên hương thơm dịu mát, tạo nên mâm cỗ Trung Thu ấm cúng và đủ đầy.",
            null,
            null,
            null
        ),
        new CatalogProductView(
            "gift-4",
            "Hộp Quà Phú Quý Cát Tường",
            2400000L,
            "2.400.000đ",
            "https://lh3.googleusercontent.com/aida-public/AB6AXuA4J4gci56o3UJwZ9hIUoa81Xr_h1aR45MacwbeC0NmizPqcp2koUgyQulFpl8p_z_KVC_VQKYDM9rj9-OUyV6Jz0ybTNCHlSYvPU5FPH5laq478uWTW8_9L7KyB0g7TTjd3hswZSwIrGx-Q-JpADu3OOJaPj4CDFLGmPQauAzR-N0f2QweQ83qVrnsNsoe9QB9jO4cs8awGSn-a65htbAwg1Ri0A8lMbD7jiclKuXz_ssgtfEaqt0gitNMb5y_vkywYCO3lwDtTQ",
            "Hộp quà Trung Thu gồm 6 bánh cao cấp kết hợp cùng bộ chén trà gốm sứ tinh tế.",
            "gift-boxes",
            6,
            null,
            null,
            "/products/gift-4",
            "Hộp quà Trung Thu",
            "Hộp 6 bánh cao cấp kết hợp cùng bộ chén trà gốm sứ tinh tế",
            "Hộp quà Phú Quý Cát Tường gồm 6 bánh Trung Thu cao cấp kết hợp cùng bộ chén trà gốm sứ tinh tế, vừa là món quà tặng ý nghĩa vừa là bộ trà cụ trang nhã cho những buổi thưởng trà đàm đạo.",
            null,
            null,
            null
        )
    );

    private static final List<CatalogProductView> MOONCAKES = Arrays.asList(
        new CatalogProductView(
            "cake-1",
            "Bánh Thập Cẩm Sò Điệp XO 200g",
            165000L,
            "165.000đ",
            "https://lh3.googleusercontent.com/aida-public/AB6AXuAD6tcZ3jmQuwW9l6RaSfeHcE0eT8DLZMBJmLhi414eOpi6EBtuFBWGEqhjtidiSzAj58CPpbG0lMd9-GukQeTWt6et_CSfOzcDzIyfT1D0jGj9fCUWWeDZ1Mo8NscGGhhHiHH7Y7kcmUZwEthCXFMOTa2p1ocrSIB7k1o3nnzpCCqVzNqpGw3IQCvu9nHxk95rc6gFairj4EQI0GX7VmrqMlbdiwQiIMTdlmNC7euoQH3LxdeTx_sB4rZ62FND74pcwpRsujUGQQ",
            "Bánh Trung Thu nhân thập cẩm sò điệp xốt XO và 2 trứng muối, trọng lượng 200g.",
            "mooncakes",
            null,
            200,
            "thap-cam",
            "/products/cake-1",
            "Bánh nướng",
            "Đóng gói túi hút chân không từng chiếc",
            "Bánh nướng truyền thống với lớp vỏ mỏng vàng nâu đều màu, ôm trọn phần nhân thập cẩm phối trộn cùng thịt sò điệp xé sợi và xốt XO đậm đà. Hai lòng đỏ trứng muối bùi béo tạo điểm nhấn cân bằng vị giác khi thưởng thức.",
            null,
            null,
            null
        ),
        new CatalogProductView(
            "cake-2",
            "Bánh Lava Kim Sa Trứng Muối 150g",
            135000L,
            "135.000đ",
            "https://lh3.googleusercontent.com/aida-public/AB6AXuCdJla6ISC_cGwyXF2RlKbQed2jXBgugeYP3fzi5YpBMtdvo84ugb8oAR-05nAjjzCDve75o-noJ4WBXMBybOzbTkNSqucJHfrpiQ-8nzk0-Hro8G4ewZ1DJ7ncf2p2199-s9BTLkwungg7CJPXU8ZYYZ2Tt0YzB1IeHDKp9AxhzYA4seZwG6WSIrrivxuyYY0GUxTgszesXuVafeikf9jSCd02wTivczYf6wKpbGG73BgVwW-A6orLQkBRqdhc5V_wprKecCno9Q",
            "Bánh Trung Thu nhân lava kim sa trứng muối tan chảy, trọng lượng 150g.",
            "mooncakes",
            null,
            150,
            "lava",
            "/products/cake-2",
            "Bánh nướng",
            "Đóng gói túi hút chân không từng chiếc",
            "Bánh Trung Thu lava hiện đại với trọng lượng 150g, nổi bật bởi lớp nhân kim sa trứng muối sánh mịn tan chảy mềm mại, mang lại hương vị béo ngậy hài hòa và ngọt dịu khó quên.",
            null,
            null,
            null
        ),
        new CatalogProductView(
            "cake-3",
            "Bánh Hạt Sen Táo Đỏ 2 Trứng 200g",
            140000L,
            "140.000đ",
            "https://lh3.googleusercontent.com/aida-public/AB6AXuCtAg4Z467vsyf72vjZ6-2Kvg7QBEPGQHSRiaGQI_K38ViufuzlE0SfiweeJ1o94uGcAVRSSrnQ8XcOjUHsPiNhk3q2MEFx6vLJL45N1GNRNdPMZmGW9EC7sxqdP7KKyM2uzPhIwFM6RqeZ40L3oo6ddlQgpGolV-2hBo6QNAu5meeuY5n2uM-B-MQZR_DUboMqVE8gW-C1p4UWMbbPrJmgPCajes66WunULNir--BLLfOnw07j-P5Uq2On0EG---wsINiIxL5WMw",
            "Bánh Trung Thu nhân hạt sen và táo đỏ 2 trứng muối, trọng lượng 200g.",
            "mooncakes",
            null,
            200,
            "hat-sen",
            "/products/cake-3",
            "Bánh nướng",
            "Đóng gói túi hút chân không từng chiếc",
            "Bánh nướng nhân hạt sen xay mịn hòa quyện cùng vị ngọt thanh của táo đỏ và 2 lòng đỏ trứng muối. Bánh có trọng lượng 200g, mang phong vị thanh nhã nhẹ nhàng theo công thức truyền thống.",
            null,
            null,
            null
        ),
        new CatalogProductView(
            "cake-4",
            "Bánh Than Tre Trà Xanh Macca 200g",
            145000L,
            "145.000đ",
            "https://lh3.googleusercontent.com/aida-public/AB6AXuA-jS6NdWtMdOxaiTvZtHDkvscmFbvdVDswgrk1v5FoJ4WQC0Ja7gTfQrVhCCQX1cUFI59MZkdJ3PRRD1ilwjXCLPfDyEn4sM1ybJFPODCnOWtWnHFHWGJj1IUMAiil7G8GK42-6BR2x5BoeIiNeQMGGuTE1c588SUcINyJ3kBPoBcRMNq20_KzmMJ_mImhKBxDclwnxX5FmYVEl8MKXgwYULT93_u0lxVp8dqFSoTR-MFxwPdLmgaGX0qCRGoijqZkYBKdRAuFzQ",
            "Bánh Trung Thu vỏ than tre nhân trà xanh và hạt macca, trọng lượng 200g.",
            "mooncakes",
            null,
            200,
            "tra-xanh",
            "/products/cake-4",
            "Bánh nướng",
            "Đóng gói túi hút chân không từng chiếc",
            "Bánh Trung Thu với lớp vỏ than tre đen mịn tự nhiên, bao bọc phần nhân trà xanh thanh mát kết hợp cùng hạt macca giòn bùi, mang lại trải nghiệm ẩm thực mới lạ và tròn vị.",
            null,
            null,
            null
        ),
        new CatalogProductView(
            "cake-5",
            "Bánh Đậu Xanh Hoàng Kim 150g",
            115000L,
            "115.000đ",
            "https://lh3.googleusercontent.com/aida-public/AB6AXuA1hC2qxet9QS5RpbMVnFSpYHsOMK6QZl-7RovjIc0BF6bl2ShIC0yqmox0yqrMXhTETdrH9dTN4_VTmyO6jHVu1Y_jFPPC3He0Z3u3PHY7KaPnbuTsb4R9jxj5X9tjcyM8mGbMal5u1SOUJjsVTyj6TUp93YWNLEKtghy5O-LhIl4SS3Q3dKMQyOK22N6mfYL72JDGDwJoy8K4YMZ_uBP-jIEICZyWaHEpBRZH6Ez91OqzxFa7UTNX-Ip0dEfb1Wsc1sCst_nF5Q",
            "Bánh Trung Thu nhân đậu xanh truyền thống kết hợp trứng muối, trọng lượng 150g.",
            "mooncakes",
            null,
            150,
            "dau-xanh",
            "/products/cake-5",
            "Bánh nướng",
            "Đóng gói túi hút chân không từng chiếc",
            "Bánh nướng nhân đậu xanh truyền thống được tán nhuyễn mịn màng, kết hợp cùng trứng muối thơm bùi. Trọng lượng bánh 150g vừa vặn cho một phần thưởng thức kèm tách trà nóng.",
            null,
            null,
            null
        )
    );

    private CatalogProductView findProductById(String id) {
        if (id == null) return null;
        for (CatalogProductView p : GIFT_BOXES) {
            if (p.getId().equalsIgnoreCase(id)) return p;
        }
        for (CatalogProductView p : MOONCAKES) {
            if (p.getId().equalsIgnoreCase(id)) return p;
        }
        return null;
    }

    @GetMapping("/")
    public String homepage() {
        return "homepage";
    }

    /**
     * Backward-compatible route: redirects to the primary gift-boxes catalog.
     */
    @GetMapping("/products")
    public String shopCatalogRedirect() {
        return "redirect:/products/gift-boxes";
    }

    /**
     * Page A: Gift boxes category catalog
     */
    @GetMapping("/products/gift-boxes")
    public String giftBoxesCatalog(Model model) {
        model.addAttribute("category", "gift-boxes");
        model.addAttribute("pageTitle", "Hộp quà Trung Thu");
        model.addAttribute("breadcrumbTitle", "Hộp quà Trung Thu");
        model.addAttribute("introHeading", "Hộp quà Trung Thu");
        model.addAttribute("introDescription", "Khám phá các mẫu hộp quà và lựa chọn sản phẩm phù hợp.");
        model.addAttribute("products", GIFT_BOXES);
        model.addAttribute("totalCount", GIFT_BOXES.size());
        return "shop-catalog";
    }

    /**
     * Page B: Individual mooncakes category catalog
     */
    @GetMapping("/products/mooncakes")
    public String mooncakesCatalog(Model model) {
        model.addAttribute("category", "mooncakes");
        model.addAttribute("pageTitle", "Bánh Trung Thu");
        model.addAttribute("breadcrumbTitle", "Bánh Trung Thu");
        model.addAttribute("introHeading", "Bánh Trung Thu");
        model.addAttribute("introDescription", "Lựa chọn bánh theo nhân, trọng lượng và mức giá.");
        model.addAttribute("products", MOONCAKES);
        model.addAttribute("totalCount", MOONCAKES.size());
        return "shop-catalog";
    }

    /**
     * Canonical Product Detail route: dynamically renders the selected product by ID.
     */
    @GetMapping("/products/{id}")
    public String productDetail(@PathVariable("id") String id, Model model) {
        CatalogProductView product = findProductById(id);
        if (product == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy sản phẩm");
        }

        boolean isGiftBox = "gift-boxes".equalsIgnoreCase(product.getCategory());
        String breadcrumbCategoryUrl = isGiftBox ? "/products/gift-boxes" : "/products/mooncakes";
        String breadcrumbCategoryName = isGiftBox ? "Hộp quà Trung Thu" : "Bánh Trung Thu";

        List<CatalogProductView> sameCategoryList = isGiftBox ? GIFT_BOXES : MOONCAKES;
        List<CatalogProductView> relatedProducts = sameCategoryList.stream()
            .filter(p -> !p.getId().equalsIgnoreCase(product.getId()))
            .limit(4)
            .collect(Collectors.toList());

        model.addAttribute("product", product);
        model.addAttribute("breadcrumbCategoryUrl", breadcrumbCategoryUrl);
        model.addAttribute("breadcrumbCategoryName", breadcrumbCategoryName);
        model.addAttribute("relatedProducts", relatedProducts);
        return "product-detail";
    }

    /**
     * Backward-compatible demo route: displays the gift-1 product detail.
     */
    @GetMapping("/products/demo")
    public String productDetailDemo(Model model) {
        return productDetail("gift-1", model);
    }

    @GetMapping("/cart")
    public String cart() {
        return "cart";
    }

    @GetMapping("/checkout")
    public String checkout() {
        return "checkout";
    }

    @GetMapping("/order-success")
    public String orderSuccess() {
        return "order-success";
    }
}
