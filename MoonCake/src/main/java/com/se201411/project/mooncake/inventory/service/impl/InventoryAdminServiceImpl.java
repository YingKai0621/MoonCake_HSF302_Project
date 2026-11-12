package com.se201411.project.mooncake.inventory.service.impl;

import com.se201411.project.mooncake.common.format.VietnamTime;
import com.se201411.project.mooncake.product.service.ProductQueryService;
import com.se201411.project.mooncake.inventory.dto.MovementsPageView;
import com.se201411.project.mooncake.common.exception.BusinessRuleViolationException;
import com.se201411.project.mooncake.common.validation.AdminFormValidation;
import com.se201411.project.mooncake.entity.InventoryBalance;
import com.se201411.project.mooncake.entity.StockMovement;
import com.se201411.project.mooncake.enums.StockMovementType;
import com.se201411.project.mooncake.inventory.dto.AdminInventoryView.Balance;
import com.se201411.project.mooncake.inventory.dto.AdminInventoryView.Movement;
import com.se201411.project.mooncake.inventory.dto.AdminInventoryView.StockFilter;
import com.se201411.project.mooncake.inventory.dto.AdminInventoryView.StockTab;
import com.se201411.project.mooncake.inventory.dto.AdminStockForm;
import com.se201411.project.mooncake.inventory.dto.InventoryPageView;
import com.se201411.project.mooncake.inventory.repository.InventoryAdminRepository;
import com.se201411.project.mooncake.inventory.service.BatchStockService;
import com.se201411.project.mooncake.inventory.service.InventoryAdminService;
import com.se201411.project.mooncake.inventory.service.PackingService;
import com.se201411.project.mooncake.product.dto.PresetComponentSnapshot;
import com.se201411.project.mooncake.product.dto.PresetRecipeSnapshot;
import com.se201411.project.mooncake.product.service.PresetConfigurationQueryService;
import com.se201411.project.mooncake.security.service.AdminAccessService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

@Service
@Transactional
public class InventoryAdminServiceImpl implements InventoryAdminService, PackingService {

    private static final ZoneId VIETNAM = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final int MAX_PAGE = 100000;

    private final InventoryAdminRepository repo;
    private final PresetConfigurationQueryService presets;
    private final AdminAccessService access;
    private final AdminFormValidation validation;
    private final BatchStockService batchStock;
    private final ProductQueryService productQueryService;
    private final Clock clock;

    public InventoryAdminServiceImpl(
        InventoryAdminRepository repo,
        PresetConfigurationQueryService presets,
        AdminAccessService access,
        AdminFormValidation validation,
        BatchStockService batchStock,
        ProductQueryService productQueryService,
        Clock clock
    ) {
        this.repo = repo;
        this.presets = presets;
        this.access = access;
        this.validation = validation;
        this.batchStock = batchStock;
        this.productQueryService = productQueryService;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryPageView inventoryPage(
        StockFilter filter,
        Long receiveVariant,
        Long packVariant,
        Long returnProduct,
        Long returnGiftBox
    ) {
        List<Balance> all = balances();
        Set<Long> readyGiftSets = presets.readyGiftSetVariantIds();
        List<Balance> packable = new ArrayList<>();
        for (Balance balance : all) {
            if (readyGiftSets.contains(balance.variantId())) {
                packable.add(balance);
            }
        }
        return new InventoryPageView(
            filter,
            stockTabs(filter),
            balances(filter),
            packable,
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString(),
            receiveVariant,
            packVariant,
            returnProduct,
            returnGiftBox
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<Balance> balances() {
        access.requireAdmin();
        List<InventoryBalance> rows = repo.balances();
        List<Long> variantIds = new ArrayList<>();
        for (InventoryBalance balance : rows) {
            variantIds.add(balance.getVariantId());
        }
        Map<Long, Integer> expired = batchStock.expiredQuantities(variantIds);
        List<Balance> result = new ArrayList<>();
        for (InventoryBalance balance : rows) {
            result.add(Balance.entityToDto(balance, expired.getOrDefault(balance.getVariantId(), 0)));
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

    @Override
    @Transactional(readOnly = true)
    public List<Movement> movements(int page) {
        access.requireAdmin();
        int safePage = Math.max(0, Math.min(page, MAX_PAGE));
        List<Movement> result = new ArrayList<>();
        for (StockMovement movement : repo.movements(safePage)) {
            result.add(toMovement(movement));
        }
        return List.copyOf(result);
    }

    private Movement toMovement(StockMovement movement) {
        String formattedCreatedAt = movement.getCreatedAt()
            .atOffset(ZoneOffset.UTC)
            .atZoneSameInstant(VIETNAM)
            .format(DATE_TIME);
        String typeLabel = movementLabel(movement.getMovementType());
        return Movement.entityToDto(movement, typeLabel, formattedCreatedAt);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasStockOrHistory(long id) {
        access.requireAdmin();
        return repo.hasStockOrHistory(id);
    }

    @Override
    public void receive(AdminStockForm form, String batchCode, LocalDate producedOn) {
        if (repo.isGiftSet(form.variantId())) {
            throw new BusinessRuleViolationException("Hộp quà chỉ vào kho bằng thao tác Đóng hộp quà.");
        }
        LocalDate batchProducedOn = producedOn;
        if (productQueryService.shelfLifeDays(form.variantId()) == null) {
            batchProducedOn = null;
        }
        LocalDate expiresOn = expiryFor(form.variantId(), batchProducedOn);
        if (expiresOn != null && expiresOn.isBefore(VietnamTime.today(clock))) {
            throw new BusinessRuleViolationException(
                "Lô này đã hết hạn (HSD " + VietnamTime.date(expiresOn) + "), không nhập được."
            );
        }
        change(form, StockMovementType.RECEIVE);
        boolean hasCode = batchCode != null && !batchCode.isBlank();
        boolean hasInfo = hasCode || batchProducedOn != null;
        if (hasInfo) {
            String key = StockMovementType.RECEIVE + ":" + form.operationKey() + ":" + form.variantId();
            batchStock.labelReceivedBatch(key, batchCode, batchProducedOn, expiresOn);
        }
    }

    @Override
    public void adjust(AdminStockForm form) {
        if (form.quantity() <= 0) {
            throw new BusinessRuleViolationException(
                "Số lượng bớt đi phải lớn hơn 0. Muốn thêm hàng hãy dùng Nhập kho."
            );
        }
        AdminStockForm decrease = new AdminStockForm(
            form.variantId(),
            -form.quantity(),
            form.reason(),
            form.operationKey()
        );
        change(decrease, StockMovementType.ADJUST);
    }

    private void change(AdminStockForm form, StockMovementType type) {
        long admin = access.requireAdmin();
        validation.validate(form);
        if (type == StockMovementType.RECEIVE && form.quantity() <= 0) {
            throw new BusinessRuleViolationException("Số lượng nhập phải lớn hơn 0.");
        }
        if (repo.isGiftSet(form.variantId())) {
            presets.requireReadyPreset(form.variantId());
        }
        String group = type + ":" + form.operationKey();
        requireNew(group);
        String key = group + ":" + form.variantId();
        apply(form.variantId(), admin, form.quantity(), type, form.reason(), group, key);
    }

    @Override
    public void pack(AdminStockForm form) {
        long admin = access.requireAdmin();
        validation.validate(form);
        if (form.quantity() <= 0) {
            throw new BusinessRuleViolationException("Số hộp phải lớn hơn 0.");
        }
        String group = "PACK:" + form.operationKey();
        requireNew(group);
        PresetRecipeSnapshot recipe = presets.requireReadyPreset(form.variantId());
        Map<Long, Integer> deltas = new TreeMap<>();
        deltas.put(recipe.boxVariantId(), -form.quantity());
        int cakes = 0;
        for (PresetComponentSnapshot component : recipe.components()) {
            cakes = Math.addExact(cakes, component.quantityPerBox());
            int quantity = Math.multiplyExact(component.quantityPerBox(), form.quantity());
            deltas.merge(component.variantId(), -quantity, Math::addExact);
        }
        if (cakes != recipe.capacityCakes() || deltas.containsKey(form.variantId())) {
            throw new BusinessRuleViolationException("Công thức hộp không hợp lệ.");
        }
        deltas.put(form.variantId(), form.quantity());
        for (Map.Entry<Long, Integer> entry : deltas.entrySet()) {
            String direction = "OUT";
            if (entry.getValue() > 0) {
                direction = "IN";
            }
            String key = group + ":" + entry.getKey() + ":" + direction;
            apply(entry.getKey(), admin, entry.getValue(), StockMovementType.PACK, form.reason(), group, key);
        }
        LocalDate packedOn = VietnamTime.today(clock);
        batchStock.finishPack(group, form.variantId(), packedOn);
    }

    @Override
    public void updateBatchInfo(long batchId, String batchCode, LocalDate producedOn) {
        long variantId = batchStock.variantIdOf(batchId);
        if (!producedOnEditable(variantId)) {
            batchStock.updateCode(batchId, batchCode);
            return;
        }
        LocalDate expiresOn = expiryFor(variantId, producedOn);
        batchStock.updateTraceability(batchId, batchCode, producedOn, expiresOn);
    }

    private boolean producedOnEditable(long variantId) {
        if (repo.isGiftSet(variantId)) {
            return false;
        }
        return productQueryService.shelfLifeDays(variantId) != null;
    }

    @Override
    public void writeOffExpired(long variantId, String operationKey) {
        int expired = batchStock.expiredQuantities(List.of(variantId)).getOrDefault(variantId, 0);
        if (expired <= 0) {
            throw new BusinessRuleViolationException("Phiên bản này không có hàng hết hạn.");
        }
        AdminStockForm form = new AdminStockForm(variantId, -expired, "Hủy hàng hết hạn", operationKey);
        change(form, StockMovementType.ADJUST);
    }

    private LocalDate expiryFor(long variantId, LocalDate producedOn) {
        Integer shelfLifeDays = productQueryService.shelfLifeDays(variantId);
        if (shelfLifeDays == null) {
            return null;
        }
        if (producedOn == null) {
            throw new BusinessRuleViolationException("Vui lòng nhập ngày sản xuất.");
        }
        return producedOn.plusDays(shelfLifeDays);
    }

    private void requireNew(String group) {
        repo.lockOperation(group);
        if (repo.operationExists(group)) {
            throw new BusinessRuleViolationException("Thao tác đã được ghi nhận. Không thay đổi tồn lần nữa.");
        }
    }

    private void apply(
        long variant,
        long admin,
        int delta,
        StockMovementType type,
        String reason,
        String group,
        String key
    ) {
        if (repo.change(variant, delta) != 1) {
            throw new BusinessRuleViolationException(
                "Thiếu tồn, thiếu số dư hoặc số lượng vượt giới hạn cho SKU #" + variant
            );
        }
        batchStock.apply(variant, delta, key, null, type);
        repo.movement(variant, admin, delta, type, reason, group, key, LocalDateTime.now(clock));
    }

    @Override
    @Transactional(readOnly = true)
    public long countLowStockVariants() {
        access.requireAdmin();
        return repo.countLowStockVariants();
    }

    @Override
    @Transactional(readOnly = true)
    public long countBatchesExpiringBy(LocalDate until) {
        access.requireAdmin();
        return repo.countBatchesExpiringBy(until);
    }

    @Override
    @Transactional(readOnly = true)
    public MovementsPageView movementsPage(int page) {
        return new MovementsPageView(movements(page), Math.max(0, page));
    }
}
