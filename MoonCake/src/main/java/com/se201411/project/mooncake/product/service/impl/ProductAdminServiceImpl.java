package com.se201411.project.mooncake.product.service.impl;

import com.se201411.project.mooncake.product.service.*;
import com.se201411.project.mooncake.product.repository.ProductAdminRepository;
import com.se201411.project.mooncake.product.dto.*;
import com.se201411.project.mooncake.product.dto.AdminCatalogForms.*;
import com.se201411.project.mooncake.entity.*;
import com.se201411.project.mooncake.enums.ProductType;
import com.se201411.project.mooncake.inventory.service.InventoryService;
import com.se201411.project.mooncake.security.service.AdminAccessService;
import com.se201411.project.mooncake.common.validation.AdminFormValidation;
import com.se201411.project.mooncake.common.exception.BusinessRuleViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;

@Service
@Transactional
public class ProductAdminServiceImpl implements ProductAdminService {
    private final ProductAdminRepository repo;
    private final InventoryService inventory;
    private final PresetConfigurationQueryService presets;
    private final AdminAccessService access;
    private final AdminFormValidation validation;

    public ProductAdminServiceImpl(ProductAdminRepository repo, InventoryService inventory,
                                   PresetConfigurationQueryService presets, AdminAccessService access, AdminFormValidation validation) {
        this.repo = repo;
        this.inventory = inventory;
        this.presets = presets;
        this.access = access;
        this.validation = validation;
    }

    @Transactional(readOnly = true)
    public AdminCatalogView catalog() {
        access.requireAdmin();
        var categories = new ArrayList<CategoryForm>();
        for (var v : repo.categories())
            categories.add(new CategoryForm(v.getId(), v.getName(), v.getSlug(), v.isActive()));
        var products = new ArrayList<ProductForm>();
        for (var v : repo.products())
            products.add(new ProductForm(v.getId(), v.getCategory().getId(), v.getProductType(), v.getName(), v.getSlug(), v.getShortDescription(), v.getDetailedDescription(), v.getMainIngredients(), v.getShelfLifeInfo(), v.getStorageGuidance(), v.isActive()));
        var variants = new ArrayList<VariantForm>();
        for (var v : repo.variants())
            variants.add(new VariantForm(v.getId(), v.getProduct().getId(), v.getSku(), v.getName(), v.getWeightGrams(), v.getFillingLabel(), v.getUnitPrice(), v.isActive()));
        var boxes = new ArrayList<BoxForm>();
        for (var v : repo.boxes())
            boxes.add(new BoxForm(v.getId(), v.getEmptyBoxVariant().getId(), v.getCapacityCakes(), v.getMaterial(), v.getNotes(), v.isActive()));
        var images = new ArrayList<ImageForm>();
        for (var v : repo.images())
            images.add(new ImageForm(v.getId(), v.getVariant().getId(), v.getImageUrl(), v.getAltText(), v.getSortOrder(), v.isPrimary()));
        return new AdminCatalogView(categories, products, variants, boxes, images);
    }

    public void saveCategory(CategoryForm f) {
        access.requireAdmin();
        validation.validate(f);
        Category v = f.id() == null ? new Category() : repo.category(f.id());
        v.setName(f.name().trim());
        v.setSlug(f.slug());
        v.setActive(f.active());
        repo.save(v);
        repo.flush();
    }

    public void saveProduct(ProductForm f) {
        access.requireAdmin();
        validation.validate(f);
        Product v = f.id() == null ? new Product() : repo.product(f.id());
        if (f.id() != null && v.getProductType() != f.productType() && repo.hasVariants(f.id()))
            throw new BusinessRuleViolationException("Sản phẩm đã có SKU không được đổi loại.");
        v.setCategory(repo.category(f.categoryId()));
        v.setProductType(f.productType());
        v.setName(f.name().trim());
        v.setSlug(f.slug());
        v.setShortDescription(f.shortDescription());
        v.setDetailedDescription(f.detailedDescription());
        v.setMainIngredients(f.mainIngredients());
        v.setShelfLifeInfo(f.shelfLifeInfo());
        v.setStorageGuidance(f.storageGuidance());
        v.setActive(f.active());
        repo.save(v);
        repo.flush();
    }

    public void saveVariant(VariantForm f) {
        access.requireAdmin();
        validation.validate(f);
        ProductVariant v = f.id() == null ? new ProductVariant() : repo.variant(f.id());
        if (f.id() != null && (!v.getProduct().getId().equals(f.productId()) || !v.getSku().equals(f.sku())))
            throw new BusinessRuleViolationException("Không đổi mã SKU hoặc sản phẩm của SKU đã tạo; hãy tạo SKU mới.");
        v.setProduct(repo.product(f.productId()));
        v.setSku(f.sku());
        v.setName(f.name().trim());
        v.setWeightGrams(f.weightGrams());
        v.setFillingLabel(f.fillingLabel());
        v.setUnitPrice(f.unitPrice());
        v.setActive(false);
        repo.save(v);
        inventory.initializeBalance(v.getId());
        if (f.active()) {
            inventory.requireBalance(v.getId());
            if (v.getProduct().getProductType() == ProductType.GIFT_SET) presets.requireReadyPreset(v.getId());
        }
        v.setActive(f.active());
        repo.flush();
    }

    public void saveBox(BoxForm f) {
        access.requireAdmin();
        validation.validate(f);
        Box v = f.id() == null ? new Box() : repo.box(f.id());
        if (f.id() != null && (!v.getEmptyBoxVariant().getId().equals(f.variantId())
                || (v.getCapacityCakes() != f.capacityCakes() && repo.boxReferenced(f.id()))))
            throw new BusinessRuleViolationException("Mẫu hộp đã được dùng không được đổi SKU/sức chứa; hãy tạo mẫu mới.");
        ProductVariant variant = repo.variant(f.variantId());
        if (variant.getProduct().getProductType() != ProductType.EMPTY_BOX)
            throw new BusinessRuleViolationException("Mẫu hộp phải dùng SKU EMPTY_BOX.");
        v.setEmptyBoxVariant(variant);
        v.setCapacityCakes(f.capacityCakes());
        v.setMaterial(f.material());
        v.setNotes(f.notes());
        v.setActive(f.active());
        repo.save(v);
        repo.flush();
    }

    public void removeImage(long imageId) {
        access.requireAdmin();
        repo.removeImage(repo.image(imageId));
    }

    public void saveImage(ImageForm f) {
        access.requireAdmin();
        validation.validate(f);
        ProductVariant variant = repo.variant(f.variantId());
        ProductImage v = f.id() == null ? new ProductImage() : repo.image(f.id());
        if (f.id() != null && !v.getVariant().getId().equals(f.variantId()))
            throw new BusinessRuleViolationException("Ảnh phải giữ nguyên SKU.");
        if (f.primary()) {
            repo.clearPrimary(f.variantId());
            if (f.id() != null) v = repo.image(f.id());
        }
        v.setVariant(variant);
        v.setImageUrl(f.imageUrl());
        v.setAltText(f.altText());
        v.setSortOrder(f.sortOrder());
        v.setPrimary(f.primary());
        repo.save(v);
        repo.flush();
    }
}
