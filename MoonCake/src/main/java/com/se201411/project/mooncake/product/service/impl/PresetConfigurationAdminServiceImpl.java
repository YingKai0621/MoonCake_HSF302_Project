package com.se201411.project.mooncake.product.service.impl;

import com.se201411.project.mooncake.product.service.PresetConfigurationAdminService;
import com.se201411.project.mooncake.product.dto.*;
import com.se201411.project.mooncake.product.repository.*;
import com.se201411.project.mooncake.entity.*;
import com.se201411.project.mooncake.enums.*;
import com.se201411.project.mooncake.inventory.service.InventoryAdminService;
import com.se201411.project.mooncake.security.service.AdminAccessService;
import com.se201411.project.mooncake.common.validation.AdminFormValidation;
import com.se201411.project.mooncake.common.exception.BusinessRuleViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;

@Service
@Transactional
public class PresetConfigurationAdminServiceImpl implements PresetConfigurationAdminService {
    private final PresetAdminRepository repo;
    private final ProductAdminRepository products;
    private final InventoryAdminService inventory;
    private final AdminAccessService access;
    private final AdminFormValidation validation;
    private final Clock clock;

    public PresetConfigurationAdminServiceImpl(PresetAdminRepository repo, ProductAdminRepository products,
                                               InventoryAdminService inventory, AdminAccessService access, AdminFormValidation validation, Clock clock) {
        this.repo = repo;
        this.products = products;
        this.inventory = inventory;
        this.access = access;
        this.validation = validation;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminPresetView> presets() {
        access.requireAdmin();
        Map<Long, List<BoxConfigurationItem>> itemsByConfiguration = new HashMap<>();
        for (BoxConfigurationItem item : repo.allPresetItems()) {
            long configurationId = item.getConfiguration().getId();
            itemsByConfiguration.computeIfAbsent(configurationId, key -> new ArrayList<>()).add(item);
        }
        List<AdminPresetView> result = new ArrayList<>();
        for (BoxConfiguration configuration : repo.all()) {
            List<AdminPresetView.Component> components = new ArrayList<>();
            for (BoxConfigurationItem item : itemsByConfiguration.getOrDefault(configuration.getId(), List.of())) {
                ProductVariant cake = item.getMooncakeVariant();
                components.add(new AdminPresetView.Component(
                    cake.getId(),
                    cake.getSku(),
                    item.getQuantityPerBox(),
                    variantLabel(cake)
                ));
            }
            Box box = configuration.getBox();
            result.add(new AdminPresetView(
                configuration.getId(),
                configuration.getPresetVariant().getId(),
                configuration.getPresetVariant().getSku(),
                box.getId(),
                box.getCapacityCakes(),
                configuration.getStatus(),
                components,
                variantLabel(configuration.getPresetVariant()),
                boxLabel(box)
            ));
        }
        return List.copyOf(result);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminPresetOptions presetOptions() {
        access.requireAdmin();
        Set<Long> giftSetsWithRecipe = new HashSet<>();
        for (BoxConfiguration configuration : repo.all()) {
            giftSetsWithRecipe.add(configuration.getPresetVariant().getId());
        }
        List<AdminPresetOptions.Option> giftSets = new ArrayList<>();
        List<AdminPresetOptions.Option> cakes = new ArrayList<>();
        for (ProductVariant variant : products.variants()) {
            ProductType type = variant.getProduct().getProductType();
            if (type == ProductType.MOONCAKE) {
                cakes.add(new AdminPresetOptions.Option(variant.getId(), variantLabel(variant)));
            }
            boolean freeGiftSet = type == ProductType.GIFT_SET
                && !variant.isActive()
                && !giftSetsWithRecipe.contains(variant.getId())
                && !inventory.hasStockOrHistory(variant.getId());
            if (freeGiftSet) {
                giftSets.add(new AdminPresetOptions.Option(variant.getId(), variantLabel(variant)));
            }
        }
        List<AdminPresetOptions.Option> boxes = new ArrayList<>();
        for (Box box : products.boxes()) {
            if (box.isActive()) {
                boxes.add(new AdminPresetOptions.Option(box.getId(), boxLabel(box)));
            }
        }
        return new AdminPresetOptions(giftSets, boxes, cakes);
    }

    private String variantLabel(ProductVariant variant) {
        return variant.getProduct().getName() + " · " + variant.getName();
    }

    private String boxLabel(Box box) {
        return box.getEmptyBoxVariant().getProduct().getName();
    }

    public void saveDraft(AdminPresetForm f) {
        access.requireAdmin();
        validation.validate(f);
        BoxConfiguration c = f.id() == null ? new BoxConfiguration() : repo.lock(f.id());
        if (f.id() != null) requireMutable(c);
        ProductVariant gift = products.variant(f.giftSetVariantId());
        if (gift.getProduct().getProductType() != ProductType.GIFT_SET || gift.isActive() || inventory.hasStockOrHistory(gift.getId()))
            throw new BusinessRuleViolationException("Chỉ chọn được hộp quà chưa mở bán và chưa có hàng trong kho.");
        if (f.id() != null && !c.getPresetVariant().getId().equals(f.giftSetVariantId()))
            throw new BusinessRuleViolationException("Không đổi được hộp quà của công thức đã tạo.");
        Box box = products.box(f.boxId());
        if (!box.isActive()) throw new BusinessRuleViolationException("Mẫu hộp đã ngừng dùng.");
        c.setBox(box);
        c.setPresetVariant(gift);
        c.setType(ConfigurationType.PRESET);
        c.setStatus(ConfigurationStatus.DRAFT);
        if (f.id() == null) c.setCreatedAt(LocalDateTime.now(clock));
        c.setUpdatedAt(LocalDateTime.now(clock));
        repo.save(c);
    }

    public void setComponent(long id, long variantId, int quantity) {
        access.requireAdmin();
        var c = repo.lock(id);
        requireMutable(c);
        if (quantity < 0) throw new BusinessRuleViolationException("Số bánh không được âm. Dùng 0 để bỏ thành phần.");
        ProductVariant cake = products.variant(variantId);
        if (cake.getProduct().getProductType() != ProductType.MOONCAKE)
            throw new BusinessRuleViolationException("Thành phần phải là bánh lẻ.");
        BoxConfigurationItem target = null;
        for (var i : repo.items(id)) if (i.getMooncakeVariant().getId().equals(variantId)) target = i;
        if (quantity == 0) {
            if (target != null) repo.removeItem(target);
        } else {
            if (target == null) {
                target = new BoxConfigurationItem();
                target.setConfiguration(c);
                target.setMooncakeVariant(cake);
            }
            target.setQuantityPerBox(quantity);
            repo.saveItem(target);
        }
        c.setUpdatedAt(LocalDateTime.now(clock));
        repo.save(c);
    }

    public void activate(long id) {
        access.requireAdmin();
        var c = repo.lock(id);
        requireMutable(c);
        products.box(c.getBox().getId());
        int total = 0;
        if (!c.getBox().isActive() || c.getBox().getEmptyBoxVariant().getProduct().getProductType() != ProductType.EMPTY_BOX)
            throw new BusinessRuleViolationException("Mẫu hộp không hợp lệ.");
        for (var i : repo.items(id)) {
            if (i.getMooncakeVariant().getProduct().getProductType() != ProductType.MOONCAKE)
                throw new BusinessRuleViolationException("Thành phần phải là bánh.");
            total = Math.addExact(total, i.getQuantityPerBox());
        }
        if (total != c.getBox().getCapacityCakes())
            throw new BusinessRuleViolationException("Tổng số bánh phải bằng sức chứa của hộp.");
        c.setStatus(ConfigurationStatus.READY);
        c.setUpdatedAt(LocalDateTime.now(clock));
        repo.save(c);
    }

    private void requireMutable(BoxConfiguration c) {
        if (c.getStatus() != ConfigurationStatus.DRAFT || c.getPresetVariant().isActive()
                || inventory.hasStockOrHistory(c.getPresetVariant().getId())
                || repo.historicallyReferenced(c.getId(), c.getPresetVariant().getId()))
            throw new BusinessRuleViolationException("Công thức đã khóa nên không sửa được. Muốn đổi thành phần, hãy tạo hộp quà mới.");
    }
}
