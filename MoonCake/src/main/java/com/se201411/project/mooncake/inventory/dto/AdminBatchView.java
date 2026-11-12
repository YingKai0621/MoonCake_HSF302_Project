package com.se201411.project.mooncake.inventory.dto;

import com.se201411.project.mooncake.entity.Product;
import com.se201411.project.mooncake.entity.ProductVariant;
import com.se201411.project.mooncake.entity.StockBatch;
import com.se201411.project.mooncake.enums.ProductType;
import java.time.LocalDate;

public record AdminBatchView(
    long id,
    String sku,
    String productName,
    String batchCode,
    LocalDate producedOn,
    LocalDate expiresOn,
    int remainingQuantity,
    String statusLabel,
    String statusTone,
    boolean producedOnEditable
) {

    public static AdminBatchView entityToDto(StockBatch batch, String batchCodeLabel, String statusLabel, String statusTone) {
        ProductVariant variant = batch.getVariant();
        Product product = variant.getProduct();
        boolean giftSet = product.getProductType() == ProductType.GIFT_SET;
        boolean producedOnEditable = !giftSet && product.getShelfLifeDays() != null;
        return new AdminBatchView(
            batch.getId(),
            variant.getSku(),
            variant.getProduct().getName() + " · " + variant.getName(),
            batchCodeLabel,
            batch.getProducedOn(),
            batch.getExpiresOn(),
            batch.getRemainingQuantity(),
            statusLabel,
            statusTone,
            producedOnEditable
        );
    }
}
