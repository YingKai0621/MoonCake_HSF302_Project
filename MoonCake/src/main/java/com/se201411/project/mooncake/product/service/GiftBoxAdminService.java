package com.se201411.project.mooncake.product.service;

import com.se201411.project.mooncake.product.dto.GiftBoxPage;
import com.se201411.project.mooncake.product.dto.GiftBoxRow;

import java.util.List;

public interface GiftBoxAdminService {
    List<GiftBoxRow> giftBoxes();

    GiftBoxPage page(long productId);

    void setOnSale(long productId, boolean onSale);

    long copy(long productId);
}
