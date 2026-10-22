package com.se201411.project.mooncake.product.dto;

import java.util.List;
import java.util.ArrayList;

import com.se201411.project.mooncake.enums.ProductType;
import com.se201411.project.mooncake.product.dto.AdminCatalogForms.*;

public record AdminCatalogView(List<CategoryForm> categories, List<ProductForm> products,
                               List<VariantForm> variants, List<BoxForm> boxes, List<ImageForm> images) {
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
        return imageFrom(images.stream().filter(image -> image.variantId() == variantId));
    }

    public String productImage(long productId) {
        var ids = variants.stream().filter(v -> v.productId() == productId)
                .map(VariantForm::id).collect(java.util.stream.Collectors.toSet());
        return imageFrom(images.stream().filter(image -> ids.contains(image.variantId())));
    }

    private String imageFrom(java.util.stream.Stream<ImageForm> candidates) {
        return candidates.sorted(java.util.Comparator.comparing(ImageForm::primary).reversed()
                        .thenComparingInt(ImageForm::sortOrder).thenComparing(ImageForm::id))
                .map(ImageForm::imageUrl).findFirst().orElse(null);
    }

    private List<VariantForm> variantsOfType(ProductType type) {
        var result = new ArrayList<VariantForm>();
        for (var variant : variants)
            for (var product : products)
                if (variant.productId() == product.id() && product.productType() == type) result.add(variant);
        return List.copyOf(result);
    }
}
