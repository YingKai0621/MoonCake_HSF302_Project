package com.se201411.project.mooncake.product.service.impl;

import com.se201411.project.mooncake.common.exception.BusinessRuleViolationException;
import com.se201411.project.mooncake.common.exception.ResourceNotFoundException;
import com.se201411.project.mooncake.enums.ConfigurationStatus;
import com.se201411.project.mooncake.enums.ProductType;
import com.se201411.project.mooncake.product.dto.AdminCatalogForms.ProductForm;
import com.se201411.project.mooncake.product.dto.AdminCatalogForms.VariantForm;
import com.se201411.project.mooncake.product.dto.AdminPresetView;
import com.se201411.project.mooncake.product.dto.AdminProductDetail;
import com.se201411.project.mooncake.product.dto.AdminProductRow;
import com.se201411.project.mooncake.product.dto.GiftBoxPage;
import com.se201411.project.mooncake.product.dto.GiftBoxRow;
import com.se201411.project.mooncake.product.service.GiftBoxAdminService;
import com.se201411.project.mooncake.product.service.PresetConfigurationAdminService;
import com.se201411.project.mooncake.product.service.ProductAdminService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class GiftBoxAdminServiceImpl implements GiftBoxAdminService {
    private final ProductAdminService products;
    private final PresetConfigurationAdminService recipes;

    public GiftBoxAdminServiceImpl(ProductAdminService products, PresetConfigurationAdminService recipes) {
        this.products = products;
        this.recipes = recipes;
    }

    @Override
    @Transactional(readOnly = true)
    public List<GiftBoxRow> giftBoxes() {
        List<GiftBoxRow> rows = new ArrayList<>();
        for (AdminProductRow row : products.productRows(ProductType.GIFT_SET)) {
            GiftBoxPage page = page(row.id());
            int done = 0;
            for (GiftBoxPage.Step step : page.steps()) {
                if (step.done()) {
                    done++;
                }
            }
            rows.add(new GiftBoxRow(
                row.id(),
                row.name(),
                row.imageUrl(),
                row.minPrice(),
                row.webLabel(),
                row.webTone(),
                done
            ));
        }
        return rows;
    }

    @Override
    @Transactional(readOnly = true)
    public GiftBoxPage page(long productId) {
        AdminProductDetail detail = products.productDetail(productId);
        if (detail.product().productType() != ProductType.GIFT_SET) {
            throw new ResourceNotFoundException("Sản phẩm này không phải hộp quà.");
        }
        AdminProductDetail.Sku sku = null;
        if (!detail.skus().isEmpty()) {
            sku = detail.skus().get(0);
        }
        AdminPresetView recipe = null;
        if (sku != null) {
            for (AdminPresetView preset : recipes.presetsForProduct(productId)) {
                if (preset.giftSetVariantId() == sku.form().id()) {
                    recipe = preset;
                }
            }
        }

        boolean hasPrice = sku != null;
        boolean locked = recipe != null && recipe.status() == ConfigurationStatus.READY;
        boolean packed = sku != null && sku.stock() > 0;
        boolean onSale = sku != null && sku.form().active() && detail.product().active();

        List<GiftBoxPage.Step> steps = new ArrayList<>();
        steps.add(new GiftBoxPage.Step(1, "Thông tin & giá", "info", hasPrice));
        steps.add(new GiftBoxPage.Step(2, "Chọn bánh cho hộp", "recipe", locked));
        steps.add(new GiftBoxPage.Step(3, "Đóng hộp", "pack", packed));
        steps.add(new GiftBoxPage.Step(4, "Mở bán", "sale", onSale));

        String current = "sale";
        for (GiftBoxPage.Step step : steps) {
            if (!step.done()) {
                current = step.panel();
                break;
            }
        }
        boolean canUnlock = locked && recipes.canUnlock(recipe.id());
        return new GiftBoxPage(detail, sku, recipe, steps, current, onSale, canUnlock);
    }

    @Override
    public long copy(long productId) {
        GiftBoxPage source = page(productId);
        long copyId = products.duplicateProduct(productId);
        if (source.recipe() != null) {
            GiftBoxPage target = page(copyId);
            recipes.copyRecipe(source.recipe().id(), target.sku().form().id());
        }
        return copyId;
    }

    @Override
    public void setOnSale(long productId, boolean onSale) {
        GiftBoxPage page = page(productId);
        if (page.sku() == null) {
            throw new BusinessRuleViolationException("Hộp quà chưa có giá bán.");
        }
        if (onSale && (page.recipe() == null || page.recipe().draft())) {
            throw new BusinessRuleViolationException("Hãy chọn đủ bánh và khóa danh sách bánh trước khi mở bán.");
        }
        ProductForm product = page.detail().product();
        products.saveProduct(new ProductForm(
            product.id(),
            product.categoryId(),
            product.productType(),
            product.name(),
            product.slug(),
            product.shortDescription(),
            product.detailedDescription(),
            product.mainIngredients(),
            product.shelfLifeInfo(),
            product.storageGuidance(),
            onSale
        ));
        VariantForm variant = page.sku().form();
        products.saveVariant(new VariantForm(
            variant.id(),
            variant.productId(),
            variant.sku(),
            variant.name(),
            variant.weightGrams(),
            variant.fillingLabel(),
            variant.unitPrice(),
            onSale
        ));
    }
}
