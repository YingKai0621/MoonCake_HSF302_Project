package com.se201411.project.mooncake.product.service;

import com.se201411.project.mooncake.product.dto.AdminCatalogForms.NewProductForm;
import com.se201411.project.mooncake.product.dto.GiftBoxPage;
import com.se201411.project.mooncake.product.dto.GiftBoxRow;
import com.se201411.project.mooncake.product.dto.NewProductPageView;

import java.util.List;

public interface GiftBoxAdminService {
    List<GiftBoxRow> giftBoxes();

    GiftBoxPage page(long productId);

    NewProductPageView newPage();

    long create(NewProductForm form, Long boxId);

    void setOnSale(long productId, boolean onSale);

    long copy(long productId);
}
