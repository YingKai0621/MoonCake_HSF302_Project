package com.se201411.project.mooncake.inventory.service.impl;

import com.se201411.project.mooncake.inventory.service.BatchStockService;
import org.springframework.beans.factory.annotation.Autowired;
import com.se201411.project.mooncake.inventory.service.*;
import com.se201411.project.mooncake.inventory.dto.*;
import com.se201411.project.mooncake.inventory.dto.AdminInventoryView.*;
import com.se201411.project.mooncake.inventory.repository.InventoryAdminRepository;
import com.se201411.project.mooncake.product.service.PresetConfigurationQueryService;
import com.se201411.project.mooncake.security.service.AdminAccessService;
import com.se201411.project.mooncake.common.validation.AdminFormValidation;
import com.se201411.project.mooncake.common.exception.BusinessRuleViolationException;
import com.se201411.project.mooncake.enums.StockMovementType;
import com.se201411.project.mooncake.entity.InventoryBalance;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@Transactional
public class InventoryAdminServiceImpl implements InventoryAdminService, PackingService {
    private BatchStockService batchStock;

    @Autowired
    public void setBatchStock(BatchStockService batchStock) {
        this.batchStock = batchStock;
    }

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

    private static final ZoneId VIETNAM = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override
    @Transactional(readOnly = true)
    public List<Balance> balances() {
        access.requireAdmin();
        List<Balance> result = new ArrayList<>();
        for (InventoryBalance balance : repo.balances()) {
            result.add(new Balance(
                balance.getVariantId(),
                balance.getVariant().getSku(),
                balance.getVariant().getProduct().getName(),
                balance.getVariant().getName(),
                balance.getAvailableQuantity(),
                balance.getLowStockThreshold()
            ));
        }
        return List.copyOf(result);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Balance> balances(StockFilter filter) {
        List<Balance> result = new ArrayList<>();
        for (Balance balance : balances()) {
            if (matches(balance, filter)) {
                result.add(balance);
            }
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockTab> stockTabs(StockFilter selected) {
        List<Balance> all = balances();
        List<StockTab> tabs = new ArrayList<>();
        tabs.add(new StockTab(StockFilter.ALL, "Tất cả", count(all, StockFilter.ALL), selected == StockFilter.ALL));
        tabs.add(new StockTab(StockFilter.LOW, "Sắp hết", count(all, StockFilter.LOW), selected == StockFilter.LOW));
        tabs.add(new StockTab(StockFilter.OUT, "Hết hàng", count(all, StockFilter.OUT), selected == StockFilter.OUT));
        return tabs;
    }

    private long count(List<Balance> balances, StockFilter filter) {
        long total = 0;
        for (Balance balance : balances) {
            if (matches(balance, filter)) {
                total++;
            }
        }
        return total;
    }

    private boolean matches(Balance balance, StockFilter filter) {
        if (filter == StockFilter.OUT) {
            return balance.out();
        }
        if (filter == StockFilter.LOW) {
            return balance.low() && !balance.out();
        }
        return true;
    }

    private String movementLabel(StockMovementType type) {
        if (type == StockMovementType.RECEIVE) {
            return "Nhập kho";
        }
        if (type == StockMovementType.SALE) {
            return "Bán hàng";
        }
        if (type == StockMovementType.CANCEL_RETURN) {
            return "Hoàn kho do hủy đơn";
        }
        if (type == StockMovementType.ADJUST) {
            return "Điều chỉnh";
        }
        return "Đóng hộp";
    }

    @Transactional(readOnly = true)
    public List<Movement> movements(int page) {
        access.requireAdmin();
        var result = new ArrayList<Movement>();
        for (var m : repo.movements(Math.max(0, Math.min(page, 100000))))
            result.add(new Movement(
                m.getVariant().getSku(),
                m.getMovementType(),
                m.getQuantityDelta(),
                m.getReason(),
                m.getOperationGroupKey(),
                m.getOrder() == null ? null : m.getOrder().getId(),
                m.getCreatedAt(),
                m.getVariant().getProduct().getName() + " · " + m.getVariant().getName(),
                movementLabel(m.getMovementType()),
                m.getCreatedAt().atOffset(ZoneOffset.UTC).atZoneSameInstant(VIETNAM).format(DATE_TIME)
            ));
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

    @Override
    public void receive(AdminStockForm f, String batchCode, LocalDate producedOn, LocalDate expiresOn) {
        change(f, StockMovementType.RECEIVE);
        boolean hasInfo = (batchCode != null && !batchCode.isBlank()) || producedOn != null || expiresOn != null;
        if (hasInfo && batchStock != null) {
            String key = StockMovementType.RECEIVE + ":" + f.operationKey() + ":" + f.variantId();
            batchStock.labelReceivedBatch(key, batchCode, producedOn, expiresOn);
        }
    }

    public void adjust(AdminStockForm f) {
        change(f, StockMovementType.ADJUST);
    }

    private void change(AdminStockForm f, StockMovementType type) {
        long admin = access.requireAdmin();
        validation.validate(f);
        if (type == StockMovementType.RECEIVE && f.quantity() <= 0) {
            throw new BusinessRuleViolationException("Số lượng nhập phải lớn hơn 0.");
        }
        if (f.quantity() == 0) {
            throw new BusinessRuleViolationException("Số điều chỉnh phải khác 0, ví dụ -2 hoặc 3.");
        }
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
        if (batchStock != null) batchStock.finishPack(group, f.variantId());
    }

    private void requireNew(String group) {
        repo.lockOperation(group);
        if (repo.operationExists(group))
            throw new BusinessRuleViolationException("Thao tác đã được ghi nhận. Không thay đổi tồn lần nữa.");
    }

    private void apply(long variant, long admin, int delta, StockMovementType type, String reason, String group, String key) {
        if (repo.change(variant, delta) != 1)
            throw new BusinessRuleViolationException("Thiếu tồn, thiếu số dư hoặc số lượng vượt giới hạn cho SKU #" + variant);
        if (batchStock != null) batchStock.apply(variant, delta, key, null, type);
        repo.movement(variant, admin, delta, type, reason, group, key, LocalDateTime.now(clock));
    }
}
