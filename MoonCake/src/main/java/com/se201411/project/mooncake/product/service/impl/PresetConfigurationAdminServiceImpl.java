package com.se201411.project.mooncake.product.service.impl;

import com.se201411.project.mooncake.order.service.OrderManagementService;
import com.se201411.project.mooncake.cart.service.CartService;
import com.se201411.project.mooncake.common.exception.BusinessRuleViolationException;
import com.se201411.project.mooncake.common.validation.AdminFormValidation;
import com.se201411.project.mooncake.entity.Box;
import com.se201411.project.mooncake.entity.BoxConfiguration;
import com.se201411.project.mooncake.entity.BoxConfigurationItem;
import com.se201411.project.mooncake.entity.ProductImage;
import com.se201411.project.mooncake.entity.ProductVariant;
import com.se201411.project.mooncake.enums.ConfigurationStatus;
import com.se201411.project.mooncake.enums.ConfigurationType;
import com.se201411.project.mooncake.enums.ProductType;
import com.se201411.project.mooncake.inventory.dto.AdminInventoryView.Balance;
import com.se201411.project.mooncake.inventory.service.InventoryAdminService;
import com.se201411.project.mooncake.product.dto.AdminPresetForm;
import com.se201411.project.mooncake.product.dto.AdminPresetOptions;
import com.se201411.project.mooncake.product.dto.AdminPresetView;
import com.se201411.project.mooncake.product.repository.PresetAdminRepository;
import com.se201411.project.mooncake.product.repository.ProductAdminRepository;
import com.se201411.project.mooncake.product.service.PresetConfigurationAdminService;
import com.se201411.project.mooncake.security.service.AdminAccessService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Transactional
public class PresetConfigurationAdminServiceImpl implements PresetConfigurationAdminService {

    private final PresetAdminRepository repo;
    private final ProductAdminRepository products;
    private final InventoryAdminService inventory;
    private final AdminAccessService access;
    private final AdminFormValidation validation;
    private final OrderManagementService orders;
    private final CartService carts;
    private final Clock clock;

    public PresetConfigurationAdminServiceImpl(
        PresetAdminRepository repo,
        ProductAdminRepository products,
        InventoryAdminService inventory,
        AdminAccessService access,
        AdminFormValidation validation,
        OrderManagementService orders,
        CartService carts,
        Clock clock
    ) {
        this.repo = repo;
        this.products = products;
        this.inventory = inventory;
        this.access = access;
        this.validation = validation;
        this.orders = orders;
        this.carts = carts;
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
                String cakeName = variantLabel(item.getMooncakeVariant());
                components.add(AdminPresetView.Component.entityToDto(item, cakeName));
            }
            String giftName = variantLabel(configuration.getPresetVariant());
            String boxName = boxLabel(configuration.getBox());
            result.add(AdminPresetView.entityToDto(configuration, components, giftName, boxName));
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
        Map<Long, String> pictures = coverPictures();
        Map<Long, Integer> stock = new HashMap<>();
        for (Balance balance : inventory.balances()) {
            stock.put(balance.variantId(), balance.quantity());
        }
        List<AdminPresetOptions.Option> giftSets = new ArrayList<>();
        List<AdminPresetOptions.Option> cakes = new ArrayList<>();
        for (ProductVariant variant : products.variants()) {
            ProductType type = variant.getProduct().getProductType();
            if (type == ProductType.MOONCAKE) {
                int cakeStock = stock.getOrDefault(variant.getId(), 0);
                cakes.add(AdminPresetOptions.Option.cakeEntityToDto(variant, pictures.get(variant.getId()), cakeStock));
            }
            boolean freeGiftSet = type == ProductType.GIFT_SET
                && !variant.isActive()
                && !giftSetsWithRecipe.contains(variant.getId())
                && !inventory.hasStockOrHistory(variant.getId());
            if (freeGiftSet) {
                giftSets.add(AdminPresetOptions.Option.giftSetEntityToDto(variant, pictures.get(variant.getId())));
            }
        }
        List<AdminPresetOptions.Option> boxes = new ArrayList<>();
        for (Box box : products.boxes()) {
            if (box.isActive()) {
                long boxVariantId = box.getEmptyBoxVariant().getId();
                int boxStock = stock.getOrDefault(boxVariantId, 0);
                boxes.add(AdminPresetOptions.Option.boxEntityToDto(box, pictures.get(boxVariantId), boxStock));
            }
        }
        return new AdminPresetOptions(giftSets, boxes, cakes);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminPresetView> presetsForProduct(long productId) {
        Set<Long> variantIds = new HashSet<>();
        for (ProductVariant variant : products.variantsOf(productId)) {
            variantIds.add(variant.getId());
        }
        List<AdminPresetView> result = new ArrayList<>();
        for (AdminPresetView preset : presets()) {
            if (variantIds.contains(preset.giftSetVariantId())) {
                result.add(preset);
            }
        }
        return result;
    }

    @Override
    public void setComponents(long id, Map<Long, Integer> quantities) {
        access.requireAdmin();
        BoxConfiguration configuration = repo.lock(id);
        requireMutable(configuration);
        int total = 0;
        for (Integer quantity : quantities.values()) {
            if (quantity == null || quantity < 0) {
                throw new BusinessRuleViolationException("Số bánh không được âm.");
            }
            total = Math.addExact(total, quantity);
        }
        int capacity = configuration.getBox().getCapacityCakes();
        if (total > capacity) {
            throw new BusinessRuleViolationException("Hộp chỉ chứa " + capacity + " bánh, đang chọn " + total + " bánh.");
        }
        for (Map.Entry<Long, Integer> entry : quantities.entrySet()) {
            setComponent(id, entry.getKey(), entry.getValue());
        }
    }

    private Map<Long, String> coverPictures() {
        Map<Long, String> pictures = new HashMap<>();
        for (ProductImage image : products.images()) {
            long variantId = image.getVariant().getId();
            if (image.isPrimary() || !pictures.containsKey(variantId)) {
                pictures.put(variantId, image.getImageUrl());
            }
        }
        return pictures;
    }

    private String variantLabel(ProductVariant variant) {
        return variant.getProduct().getName() + " · " + variant.getName();
    }

    private String boxLabel(Box box) {
        ProductVariant variant = box.getEmptyBoxVariant();
        return variant.getProduct().getName() + " · " + variant.getName();
    }

    @Override
    public void saveDraft(AdminPresetForm form) {
        access.requireAdmin();
        validation.validate(form);
        boolean isNew = form.id() == null;
        BoxConfiguration configuration;
        if (isNew) {
            configuration = new BoxConfiguration();
        } else {
            configuration = repo.lock(form.id());
            requireMutable(configuration);
        }
        ProductVariant gift = products.variant(form.giftSetVariantId());
        boolean giftUsable = gift.getProduct().getProductType() == ProductType.GIFT_SET
            && !gift.isActive()
            && !inventory.hasStockOrHistory(gift.getId());
        if (!giftUsable) {
            throw new BusinessRuleViolationException("Chỉ chọn được hộp quà chưa mở bán và chưa có hàng trong kho.");
        }
        if (!isNew && !configuration.getPresetVariant().getId().equals(form.giftSetVariantId())) {
            throw new BusinessRuleViolationException("Không đổi được hộp quà của công thức đã tạo.");
        }
        Box box = products.box(form.boxId());
        if (!box.isActive()) {
            throw new BusinessRuleViolationException("Mẫu hộp đã ngừng dùng.");
        }
        configuration.setBox(box);
        configuration.setPresetVariant(gift);
        configuration.setType(ConfigurationType.PRESET);
        configuration.setStatus(ConfigurationStatus.DRAFT);
        if (isNew) {
            configuration.setCreatedAt(LocalDateTime.now(clock));
        }
        configuration.setUpdatedAt(LocalDateTime.now(clock));
        repo.save(configuration);
    }

    @Override
    public void setComponent(long id, long variantId, int quantity) {
        access.requireAdmin();
        BoxConfiguration configuration = repo.lock(id);
        requireMutable(configuration);
        if (quantity < 0) {
            throw new BusinessRuleViolationException("Số bánh không được âm. Dùng 0 để bỏ thành phần.");
        }
        ProductVariant cake = products.variant(variantId);
        if (cake.getProduct().getProductType() != ProductType.MOONCAKE) {
            throw new BusinessRuleViolationException("Thành phần phải là bánh lẻ.");
        }
        BoxConfigurationItem target = null;
        for (BoxConfigurationItem item : repo.items(id)) {
            if (item.getMooncakeVariant().getId().equals(variantId)) {
                target = item;
            }
        }
        if (quantity == 0) {
            if (target != null) {
                repo.removeItem(target);
            }
        } else {
            if (target == null) {
                target = new BoxConfigurationItem();
                target.setConfiguration(configuration);
                target.setMooncakeVariant(cake);
            }
            target.setQuantityPerBox(quantity);
            repo.saveItem(target);
        }
        configuration.setUpdatedAt(LocalDateTime.now(clock));
        repo.save(configuration);
    }

    @Override
    public void activate(long id) {
        access.requireAdmin();
        BoxConfiguration configuration = repo.lock(id);
        requireMutable(configuration);
        products.box(configuration.getBox().getId());
        Box box = configuration.getBox();
        ProductType boxType = box.getEmptyBoxVariant().getProduct().getProductType();
        if (!box.isActive() || boxType != ProductType.EMPTY_BOX) {
            throw new BusinessRuleViolationException("Mẫu hộp không hợp lệ.");
        }
        int total = 0;
        for (BoxConfigurationItem item : repo.items(id)) {
            if (item.getMooncakeVariant().getProduct().getProductType() != ProductType.MOONCAKE) {
                throw new BusinessRuleViolationException("Thành phần phải là bánh.");
            }
            total = Math.addExact(total, item.getQuantityPerBox());
        }
        if (total != box.getCapacityCakes()) {
            throw new BusinessRuleViolationException("Tổng số bánh phải bằng sức chứa của hộp.");
        }
        configuration.setStatus(ConfigurationStatus.READY);
        configuration.setUpdatedAt(LocalDateTime.now(clock));
        repo.save(configuration);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canUnlock(long id) {
        access.requireAdmin();
        BoxConfiguration configuration = repo.lock(id);
        return unlockBlocker(configuration) == null;
    }

    @Override
    public void unlock(long id) {
        access.requireAdmin();
        BoxConfiguration configuration = repo.lock(id);
        String blocker = unlockBlocker(configuration);
        if (blocker != null) {
            throw new BusinessRuleViolationException(blocker);
        }
        configuration.setStatus(ConfigurationStatus.DRAFT);
        configuration.setUpdatedAt(LocalDateTime.now(clock));
        repo.save(configuration);
    }

    @Override
    public void copyRecipe(long fromId, long toGiftSetVariantId) {
        access.requireAdmin();
        BoxConfiguration source = repo.lock(fromId);
        saveDraft(new AdminPresetForm(null, toGiftSetVariantId, source.getBox().getId()));
        BoxConfiguration copy = null;
        for (BoxConfiguration configuration : repo.all()) {
            if (configuration.getPresetVariant().getId() == toGiftSetVariantId) {
                copy = configuration;
            }
        }
        if (copy == null) {
            throw new BusinessRuleViolationException("Không tạo được bản sao danh sách bánh.");
        }
        for (BoxConfigurationItem item : repo.items(fromId)) {
            setComponent(copy.getId(), item.getMooncakeVariant().getId(), item.getQuantityPerBox());
        }
    }

    private String unlockBlocker(BoxConfiguration configuration) {
        long giftId = configuration.getPresetVariant().getId();
        if (configuration.getStatus() != ConfigurationStatus.READY) {
            return "Danh sách bánh đang ở dạng nháp, không cần mở khóa.";
        }
        if (configuration.getPresetVariant().isActive()) {
            return "Hộp quà đang mở bán. Hãy ngừng bán trước khi mở khóa.";
        }
        if (inventory.hasStockOrHistory(giftId)) {
            return "Hộp quà đã đóng hộp nên không mở khóa được. Hãy tạo bản sao để đổi bánh.";
        }
        if (inCarts(configuration.getId(), giftId)) {
            return "Hộp quà đang nằm trong giỏ hàng của khách nên không mở khóa được.";
        }
        if (hasOrders(configuration.getId(), giftId)) {
            return "Hộp quà đã có đơn hàng nên không mở khóa được. Hãy tạo bản sao để đổi bánh.";
        }
        return null;
    }

    private boolean inCarts(long configurationId, long giftId) {
        long asRecipe = carts.countItemsWithConfigurations(List.of(configurationId));
        long asGift = carts.countItemsWithVariants(List.of(giftId));
        return asRecipe + asGift > 0;
    }

    private boolean hasOrders(long configurationId, long giftId) {
        long asRecipe = orders.countItemsWithConfigurations(List.of(configurationId));
        long asGift = orders.countItemsWithVariants(List.of(giftId));
        return asRecipe + asGift > 0;
    }

    private void requireMutable(BoxConfiguration configuration) {
        long giftId = configuration.getPresetVariant().getId();
        boolean locked = configuration.getStatus() != ConfigurationStatus.DRAFT
            || configuration.getPresetVariant().isActive()
            || inventory.hasStockOrHistory(giftId)
            || hasOrders(configuration.getId(), giftId);
        if (locked) {
            throw new BusinessRuleViolationException(
                "Công thức đã khóa nên không sửa được. Muốn đổi thành phần, hãy tạo hộp quà mới."
            );
        }
    }
}
