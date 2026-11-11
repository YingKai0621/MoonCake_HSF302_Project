package com.se201411.project.mooncake.product.dto;

import com.se201411.project.mooncake.entity.Box;
import com.se201411.project.mooncake.entity.ProductVariant;

import java.util.List;

public record AdminPresetOptions(List<Option> giftSets, List<Option> boxes, List<Option> cakes) {

    public record Option(long id, String label, String detail, String imageUrl, int stock, boolean selling) {

        public static Option cakeEntityToDto(ProductVariant variant, String imageUrl, int stock) {
            boolean selling = variant.isActive() && variant.getProduct().isActive();
            return new Option(
                variant.getId(),
                variant.getProduct().getName(),
                variant.getName(),
                imageUrl,
                stock,
                selling
            );
        }

        public static Option giftSetEntityToDto(ProductVariant variant, String imageUrl) {
            String label = variant.getProduct().getName() + " · " + variant.getName();
            return new Option(variant.getId(), label, null, imageUrl, 0, false);
        }

        public static Option boxEntityToDto(Box box, String imageUrl, int stock) {
            String label = box.getEmptyBoxVariant().getProduct().getName();
            String detail = box.getCapacityCakes() + " bánh";
            return new Option(box.getId(), label, detail, imageUrl, stock, true);
        }
    }
}
