package com.se201411.project.mooncake.product.controller;

import com.se201411.project.mooncake.enums.ProductType;
import com.se201411.project.mooncake.product.dto.CatalogProductView;
import com.se201411.project.mooncake.product.service.ProductQueryService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

@Controller
@RequestMapping("/products")
public class ProductCatalogController {

    private final ProductQueryService productQueryService;

    public ProductCatalogController(ProductQueryService productQueryService) {
        this.productQueryService = productQueryService;
    }

    @GetMapping
    public String catalogRedirect() {
        return "redirect:/products/gift-boxes";
    }

    @GetMapping("/demo")
    public String legacyDemoRedirect() {
        return "redirect:/products/gift-boxes";
    }

    @GetMapping("/gift-boxes")
    public String giftBoxesCatalog(Model model) {
        List<CatalogProductView> products = productQueryService
            .findActiveCatalog(ProductType.GIFT_SET);

        addCatalogModel(
            model,
            products,
            "gift-boxes",
            "Hộp quà Trung Thu",
            "Khám phá các hộp quà PRESET đang sẵn sàng để bán."
        );
        model.addAttribute("boxCountOptions", distinctBoxCounts(products));
        return "shop-catalog";
    }

    @GetMapping("/mooncakes")
    public String mooncakesCatalog(Model model) {
        List<CatalogProductView> products = productQueryService
            .findActiveCatalog(ProductType.MOONCAKE);

        addCatalogModel(
            model,
            products,
            "mooncakes",
            "Bánh Trung Thu",
            "Lựa chọn bánh theo nhân, trọng lượng và mức giá."
        );
        model.addAttribute("fillingOptions", distinctFillings(products));
        model.addAttribute("weightOptions", distinctWeights(products));
        return "shop-catalog";
    }

    @GetMapping("/{slug}")
    public String productDetail(@PathVariable String slug, Model model) {
        CatalogProductView product = productQueryService.requireActiveProduct(slug);
        List<CatalogProductView> catalog = productQueryService.findActiveCatalog(product.getType());
        List<CatalogProductView> relatedProducts = findRelatedProducts(catalog, product.getId());

        model.addAttribute("product", product);
        model.addAttribute("breadcrumbCategoryUrl", categoryUrl(product.getType()));
        model.addAttribute("breadcrumbCategoryName", categoryName(product.getType()));
        model.addAttribute("relatedProducts", relatedProducts);
        return "product-detail";
    }

    private List<CatalogProductView> findRelatedProducts(
        List<CatalogProductView> catalog,
        long currentProductId
    ) {
        List<CatalogProductView> relatedProducts = new ArrayList<>();

        for (CatalogProductView candidate : catalog) {
            if (candidate.getId() == currentProductId) {
                continue;
            }

            relatedProducts.add(candidate);
            if (relatedProducts.size() == 4) {
                break;
            }
        }

        return List.copyOf(relatedProducts);
    }

    private void addCatalogModel(
        Model model,
        List<CatalogProductView> products,
        String category,
        String title,
        String description
    ) {
        model.addAttribute("category", category);
        model.addAttribute("pageTitle", title);
        model.addAttribute("breadcrumbTitle", title);
        model.addAttribute("introHeading", title);
        model.addAttribute("introDescription", description);
        model.addAttribute("products", products);
        model.addAttribute("totalCount", products.size());
    }

    private List<Integer> distinctBoxCounts(List<CatalogProductView> products) {
        Set<Integer> boxCounts = new TreeSet<>();
        for (CatalogProductView product : products) {
            boxCounts.addAll(product.getBoxCounts());
        }
        return List.copyOf(boxCounts);
    }

    private List<String> distinctFillings(List<CatalogProductView> products) {
        Set<String> fillings = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (CatalogProductView product : products) {
            fillings.addAll(product.getFillings());
        }
        return List.copyOf(fillings);
    }

    private List<Integer> distinctWeights(List<CatalogProductView> products) {
        Set<Integer> weights = new TreeSet<>();
        for (CatalogProductView product : products) {
            weights.addAll(product.getWeights());
        }
        return List.copyOf(weights);
    }

    private String categoryUrl(ProductType type) {
        if (type == ProductType.MOONCAKE) {
            return "/products/mooncakes";
        }
        return "/products/gift-boxes";
    }

    private String categoryName(ProductType type) {
        if (type == ProductType.MOONCAKE) {
            return "Bánh Trung Thu";
        }
        return "Hộp quà Trung Thu";
    }
}
