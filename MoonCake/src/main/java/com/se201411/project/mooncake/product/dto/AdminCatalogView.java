package com.se201411.project.mooncake.product.dto;

import com.se201411.project.mooncake.enums.ProductType;
import com.se201411.project.mooncake.product.dto.AdminCatalogForms.BoxForm;
import com.se201411.project.mooncake.product.dto.AdminCatalogForms.CategoryForm;
import com.se201411.project.mooncake.product.dto.AdminCatalogForms.ImageForm;
import com.se201411.project.mooncake.product.dto.AdminCatalogForms.ProductForm;
import com.se201411.project.mooncake.product.dto.AdminCatalogForms.VariantForm;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public record AdminCatalogView(
    List<CategoryForm> categories,
    List<ProductForm> products,
    List<VariantForm> variants,
    List<BoxForm> boxes,
    List<ImageForm> images
) {
    public AdminCatalogView {
        categories = List.copyOf(categories);
        products = List.copyOf(products);
        variants = List.copyOf(variants);
        boxes = List.copyOf(boxes);
        images = List.copyOf(images);
    }

    public List<VariantForm> giftVariants() {
        return variantsOfType(ProductType.GIFT_SET);
    }

    public List<VariantForm> cakeVariants() {
        return variantsOfType(ProductType.MOONCAKE);
    }

    public List<VariantForm> boxVariants() {
        return variantsOfType(ProductType.EMPTY_BOX);
    }

    public String variantImage(long variantId) {
        Set<Long> ids = new HashSet<>();
        ids.add(variantId);
        return imageOf(ids);
    }

    public String productImage(long productId) {
        Set<Long> ids = new HashSet<>();
        for (VariantForm variant : variants) {
            if (variant.productId() == productId) {
                ids.add(variant.id());
            }
        }
        return imageOf(ids);
    }

    private String imageOf(Set<Long> variantIds) {
        ImageForm best = null;
        for (ImageForm image : images) {
            if (variantIds.contains(image.variantId()) && isBetter(image, best)) {
                best = image;
            }
        }
        if (best == null) {
            return null;
        }
        return best.imageUrl();
    }

    private boolean isBetter(ImageForm image, ImageForm current) {
        if (current == null) {
            return true;
        }
        if (image.primary() != current.primary()) {
            return image.primary();
        }
        if (image.sortOrder() != current.sortOrder()) {
            return image.sortOrder() < current.sortOrder();
        }
        return image.id() < current.id();
    }

    private List<VariantForm> variantsOfType(ProductType type) {
        List<VariantForm> result = new ArrayList<>();
        for (VariantForm variant : variants) {
            for (ProductForm product : products) {
                if (variant.productId() == product.id() && product.productType() == type) {
                    result.add(variant);
                }
            }
        }
        return List.copyOf(result);
    }
}
