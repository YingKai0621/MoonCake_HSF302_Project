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

    @Transactional(readOnly = true)
    public List<AdminPresetView> presets() {
        access.requireAdmin();
        var result = new ArrayList<AdminPresetView>();
        var componentsByConfiguration = new HashMap<Long, List<BoxConfigurationItem>>();
        for (var item : repo.allPresetItems()) {
            componentsByConfiguration.computeIfAbsent(item.getConfiguration().getId(), ignored -> new ArrayList<>()).add(item);
        }
        for (var c : repo.all()) {
            var components = new ArrayList<AdminPresetView.Component>();
            for (var i : componentsByConfiguration.getOrDefault(c.getId(), List.of()))
                components.add(new AdminPresetView.Component(i.getMooncakeVariant().getId(), i.getMooncakeVariant().getSku(), i.getQuantityPerBox()));
            result.add(new AdminPresetView(c.getId(), c.getPresetVariant().getId(), c.getPresetVariant().getSku(), c.getBox().getId(), c.getBox().getCapacityCakes(), c.getStatus(), List.copyOf(components)));
        }
        return List.copyOf(result);
    }

    public void saveDraft(AdminPresetForm f) {
        access.requireAdmin();
        validation.validate(f);
        BoxConfiguration c = f.id() == null ? new BoxConfiguration() : repo.lock(f.id());
        if (f.id() != null) requireMutable(c);
        ProductVariant gift = products.variant(f.giftSetVariantId());
        if (gift.getProduct().getProductType() != ProductType.GIFT_SET || gift.isActive() || inventory.hasStockOrHistory(gift.getId()))
            throw new BusinessRuleViolationException("Chọn SKU GIFT_SET mới, chưa mở bán và chưa có tồn/lịch sử.");
        if (f.id() != null && !c.getPresetVariant().getId().equals(f.giftSetVariantId()))
            throw new BusinessRuleViolationException("Không đổi SKU của công thức đã tạo.");
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
        if (quantity < 0) throw new BusinessRuleViolationException("Số bánh không được âm; dùng 0 để bỏ thành phần.");
        ProductVariant cake = products.variant(variantId);
        if (cake.getProduct().getProductType() != ProductType.MOONCAKE)
            throw new BusinessRuleViolationException("Thành phần phải là SKU MOONCAKE.");
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
            throw new BusinessRuleViolationException("Tổng số bánh phải bằng sức chứa hộp.");
        c.setStatus(ConfigurationStatus.READY);
        c.setUpdatedAt(LocalDateTime.now(clock));
        repo.save(c);
    }

    private void requireMutable(BoxConfiguration c) {
        if (c.getStatus() != ConfigurationStatus.DRAFT || c.getPresetVariant().isActive()
                || inventory.hasStockOrHistory(c.getPresetVariant().getId())
                || repo.historicallyReferenced(c.getId(), c.getPresetVariant().getId()))
            throw new BusinessRuleViolationException("Công thức đã khóa. Thay đổi thành phần cần SKU và PRESET mới.");
    }
}
