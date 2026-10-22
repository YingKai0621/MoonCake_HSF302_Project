package com.se201411.project.mooncake.inventory.service.impl;

import com.se201411.project.mooncake.inventory.service.*;
import com.se201411.project.mooncake.inventory.dto.*;
import com.se201411.project.mooncake.inventory.dto.AdminInventoryView.*;
import com.se201411.project.mooncake.inventory.repository.InventoryAdminRepository;
import com.se201411.project.mooncake.product.service.PresetConfigurationQueryService;
import com.se201411.project.mooncake.security.service.AdminAccessService;
import com.se201411.project.mooncake.common.validation.AdminFormValidation;
import com.se201411.project.mooncake.common.exception.BusinessRuleViolationException;
import com.se201411.project.mooncake.enums.StockMovementType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;

@Service
@Transactional
public class InventoryAdminServiceImpl implements InventoryAdminService, PackingService {
    private final InventoryAdminRepository repo;
    private final PresetConfigurationQueryService presets;
    private final AdminAccessService access;
    private final AdminFormValidation validation;
    private final Clock clock;

    public InventoryAdminServiceImpl(InventoryAdminRepository repo, PresetConfigurationQueryService presets,
                                     AdminAccessService access, AdminFormValidation validation, Clock clock) {
        this.repo = repo;
        this.presets = presets;
        this.access = access;
        this.validation = validation;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<Balance> balances() {
        access.requireAdmin();
        var result = new ArrayList<Balance>();
        for (var b : repo.balances())
            result.add(new Balance(b.getVariantId(), b.getVariant().getSku(), b.getVariant().getName(), b.getAvailableQuantity(), b.getLowStockThreshold()));
        return List.copyOf(result);
    }

    @Transactional(readOnly = true)
    public List<Movement> movements(int page) {
        access.requireAdmin();
        var result = new ArrayList<Movement>();
        for (var m : repo.movements(Math.max(0, Math.min(page, 100000))))
            result.add(new Movement(m.getVariant().getSku(), m.getMovementType(), m.getQuantityDelta(), m.getReason(), m.getOperationGroupKey(), m.getOrder() == null ? null : m.getOrder().getId(), m.getCreatedAt()));
        return List.copyOf(result);
    }

    @Transactional(readOnly = true)
    public boolean hasStockOrHistory(long id) {
        access.requireAdmin();
        return repo.hasStockOrHistory(id);
    }

    public void receive(AdminStockForm f) {
        change(f, StockMovementType.RECEIVE);
    }

    public void adjust(AdminStockForm f) {
        change(f, StockMovementType.ADJUST);
    }

    private void change(AdminStockForm f, StockMovementType type) {
        long admin = access.requireAdmin();
        validation.validate(f);
        if (f.quantity() == 0 || (type == StockMovementType.RECEIVE && f.quantity() < 0))
            throw new BusinessRuleViolationException("Số lượng nhập phải dương; điều chỉnh phải khác 0.");
        if (repo.isGiftSet(f.variantId())) presets.requireReadyPreset(f.variantId());
        String group = type + ":" + f.operationKey();
        requireNew(group);
        apply(f.variantId(), admin, f.quantity(), type, f.reason(), group, group + ":" + f.variantId());
    }

    public void pack(AdminStockForm f) {
        long admin = access.requireAdmin();
        validation.validate(f);
        if (f.quantity() <= 0) throw new BusinessRuleViolationException("Số hộp phải lớn hơn 0.");
        String group = "PACK:" + f.operationKey();
        requireNew(group);
        var recipe = presets.requireReadyPreset(f.variantId());
        var deltas = new TreeMap<Long, Integer>();
        deltas.put(recipe.boxVariantId(), -f.quantity());
        int cakes = 0;
        for (var component : recipe.components()) {
            cakes = Math.addExact(cakes, component.quantityPerBox());
            int quantity = Math.multiplyExact(component.quantityPerBox(), f.quantity());
            deltas.merge(component.variantId(), -quantity, Math::addExact);
        }
        if (cakes != recipe.capacityCakes() || deltas.containsKey(f.variantId()))
            throw new BusinessRuleViolationException("Công thức hộp không hợp lệ.");
        deltas.put(f.variantId(), f.quantity());
        for (var entry : deltas.entrySet()) {
            String direction = entry.getValue() > 0 ? "IN" : "OUT";
            apply(entry.getKey(), admin, entry.getValue(), StockMovementType.PACK, f.reason(), group, group + ":" + entry.getKey() + ":" + direction);
        }
    }

    private void requireNew(String group) {
        repo.lockOperation(group);
        if (repo.operationExists(group))
            throw new BusinessRuleViolationException("Thao tác đã được ghi nhận; không thay đổi tồn lần nữa.");
    }

    private void apply(long variant, long admin, int delta, StockMovementType type, String reason, String group, String key) {
        if (repo.change(variant, delta) != 1)
            throw new BusinessRuleViolationException("Thiếu tồn, thiếu số dư hoặc số lượng vượt giới hạn cho SKU #" + variant);
        repo.movement(variant, admin, delta, type, reason, group, key, LocalDateTime.now(clock));
    }
}
