package com.se201411.project.mooncake.cart.service.impl;

import com.se201411.project.mooncake.cart.dto.CartItemView;
import com.se201411.project.mooncake.cart.dto.CustomCartComponentView;
import com.se201411.project.mooncake.cart.dto.CustomerCartView;
import com.se201411.project.mooncake.cart.repository.CartItemRepository;
import com.se201411.project.mooncake.cart.repository.CartRepository;
import com.se201411.project.mooncake.cart.repository.CustomBoxConfigurationItemRepository;
import com.se201411.project.mooncake.cart.repository.CustomBoxConfigurationRepository;
import com.se201411.project.mooncake.cart.service.CustomerCartService;
import com.se201411.project.mooncake.common.exception.BusinessRuleViolationException;
import com.se201411.project.mooncake.common.exception.ConcurrentOperationException;
import com.se201411.project.mooncake.common.exception.ResourceNotFoundException;
import com.se201411.project.mooncake.entity.Cart;
import com.se201411.project.mooncake.entity.CartItem;
import com.se201411.project.mooncake.entity.BoxConfiguration;
import com.se201411.project.mooncake.entity.BoxConfigurationItem;
import com.se201411.project.mooncake.entity.ProductVariant;
import com.se201411.project.mooncake.enums.CartStatus;
import com.se201411.project.mooncake.enums.ConfigurationStatus;
import com.se201411.project.mooncake.enums.ConfigurationType;
import com.se201411.project.mooncake.enums.ProductType;
import com.se201411.project.mooncake.product.dto.CartVariantSnapshot;
import com.se201411.project.mooncake.product.service.ProductQueryService;
import com.se201411.project.mooncake.security.dto.ShopperIdentity;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class CustomerCartServiceImpl implements CustomerCartService {

    private static final Locale VIETNAMESE = Locale.forLanguageTag("vi-VN");
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final CustomBoxConfigurationRepository configurationRepository;
    private final CustomBoxConfigurationItemRepository configurationItemRepository;
    private final ProductQueryService productQueryService;
    private final EntityManager entityManager;
    private final Clock clock;

    public CustomerCartServiceImpl(
        CartRepository cartRepository,
        CartItemRepository cartItemRepository,
        CustomBoxConfigurationRepository configurationRepository,
        CustomBoxConfigurationItemRepository configurationItemRepository,
        ProductQueryService productQueryService,
        EntityManager entityManager,
        Clock clock
    ) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.configurationRepository = configurationRepository;
        this.configurationItemRepository = configurationItemRepository;
        this.productQueryService = productQueryService;
        this.entityManager = entityManager;
        this.clock = clock;
    }

    @Override
    public CustomerCartView getCurrentCart(ShopperIdentity customerIdentity) {
        requireIdentity(customerIdentity);

        Optional<Cart> cartResult = cartRepository.findByCustomerIdAndStatus(
            customerIdentity.customerId(),
            CartStatus.ACTIVE
        );
        if (cartResult.isEmpty()) {
            return CustomerCartView.empty();
        }

        Cart cart = cartResult.get();
        List<CartItem> variantItems = cartItemRepository.findVariantItemsByCartId(cart.getId());
        List<CartItem> customItems = cartItemRepository.findCustomItemsByCartId(cart.getId());
        return buildCartView(cart, variantItems, customItems);
    }

    @Override
    public int countCurrentItems(ShopperIdentity customerIdentity) {
        requireIdentity(customerIdentity);

        Optional<Cart> cartResult = cartRepository.findByCustomerIdAndStatus(
            customerIdentity.customerId(),
            CartStatus.ACTIVE
        );
        if (cartResult.isEmpty()) {
            return 0;
        }

        long totalQuantity = cartItemRepository.sumQuantityByCartId(cartResult.get().getId());
        if (totalQuantity > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) totalQuantity;
    }

    @Override
    @Transactional
    public void addVariant(
        ShopperIdentity customerIdentity,
        long variantId,
        int quantity
    ) {
        requireIdentity(customerIdentity);
        requirePositiveId(variantId, "SKU không hợp lệ");
        requirePositiveQuantity(quantity);

        productQueryService.requireSellableVariant(variantId);
        Cart cart = lockOrCreateActiveCart(customerIdentity.customerId());

        Optional<CartItem> existingItem = cartItemRepository.findVariantItem(
            cart.getId(),
            variantId
        );
        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            item.setQuantity(addQuantities(item.getQuantity(), quantity));
        } else {
            CartItem item = new CartItem();
            item.setCart(cart);
            ProductVariant variantReference = entityManager.getReference(
                ProductVariant.class,
                variantId
            );
            item.setVariant(variantReference);
            item.setQuantity(quantity);
            item.setAddedAt(now());
            cartItemRepository.save(item);
        }

        cart.setUpdatedAt(now());
    }

    @Override
    @Transactional
    public void updateItemQuantity(
        ShopperIdentity customerIdentity,
        long cartItemId,
        int quantity
    ) {
        requireIdentity(customerIdentity);
        requirePositiveId(cartItemId, "Dòng giỏ hàng không hợp lệ");
        requirePositiveQuantity(quantity);

        Cart cart = requireLockedActiveCart(customerIdentity.customerId());
        CartItem item = requireOwnedItem(cart.getId(), cartItemId);

        if (item.getVariant() != null && quantity > item.getQuantity()) {
            productQueryService.requireSellableVariant(item.getVariant().getId());
        }
        if (item.getConfigurationId() != null && quantity > item.getQuantity()) {
            validateCustomConfigurationSellable(item.getConfigurationId(), cart.getId());
        }

        item.setQuantity(quantity);
        cart.setUpdatedAt(now());
    }

    @Override
    @Transactional
    public void removeItem(ShopperIdentity customerIdentity, long cartItemId) {
        requireIdentity(customerIdentity);
        requirePositiveId(cartItemId, "Dòng giỏ hàng không hợp lệ");

        Cart cart = requireLockedActiveCart(customerIdentity.customerId());
        CartItem item = requireOwnedItem(cart.getId(), cartItemId);
        if (item.getConfigurationId() == null) {
            cartItemRepository.delete(item);
        } else {
            BoxConfiguration configuration = requireOwnedCustomConfiguration(
                item.getConfigurationId(),
                cart.getId()
            );
            cartItemRepository.delete(item);
            entityManager.flush();
            List<BoxConfigurationItem> components = configurationItemRepository
                .findByConfigurationIds(List.of(configuration.getId()));
            configurationItemRepository.deleteAll(components);
            entityManager.flush();
            configurationRepository.delete(configuration);
        }
        cart.setUpdatedAt(now());
    }

    private CustomerCartView buildCartView(
        Cart cart,
        List<CartItem> variantItems,
        List<CartItem> customItems
    ) {
        if (variantItems.isEmpty() && customItems.isEmpty()) {
            return new CustomerCartView(
                cart.getId(),
                List.of(),
                0,
                BigDecimal.ZERO,
                formatVnd(BigDecimal.ZERO),
                false
            );
        }

        List<Long> variantIds = new ArrayList<>();
        for (CartItem item : variantItems) {
            variantIds.add(item.getVariant().getId());
        }
        for (CartItem item : customItems) {
            variantIds.add(
                item.getConfiguration().getBox().getEmptyBoxVariant().getId()
            );
        }

        List<CartVariantSnapshot> variantSnapshots = productQueryService
            .findVariantsForCart(variantIds);
        Map<Long, CartVariantSnapshot> variantsById = new LinkedHashMap<>();
        for (CartVariantSnapshot snapshot : variantSnapshots) {
            variantsById.put(snapshot.variantId(), snapshot);
        }

        List<CartItemView> itemViews = new ArrayList<>();
        int totalQuantity = 0;
        BigDecimal subtotal = BigDecimal.ZERO;
        boolean hasUnavailableItems = false;

        for (CartItem item : variantItems) {
            long variantId = item.getVariant().getId();
            CartVariantSnapshot variant = variantsById.get(variantId);
            if (variant == null) {
                throw new ResourceNotFoundException(
                    "Không tìm thấy thông tin SKU trong giỏ: " + variantId
                );
            }

            BigDecimal lineTotal = variant.unitPrice()
                .multiply(BigDecimal.valueOf(item.getQuantity()));
            CartItemView itemView = new CartItemView(
                item.getId(),
                variant.variantId(),
                variant.productSlug(),
                variant.productName(),
                variant.variantName(),
                variant.sku(),
                variant.productType(),
                variant.imageUrl(),
                variant.imageAlt(),
                item.getQuantity(),
                variant.unitPrice(),
                formatVnd(variant.unitPrice()),
                lineTotal,
                formatVnd(lineTotal),
                variant.currentlySellable()
            );
            itemViews.add(itemView);

            totalQuantity = addQuantities(totalQuantity, item.getQuantity());
            subtotal = subtotal.add(lineTotal);
            if (!variant.currentlySellable()) {
                hasUnavailableItems = true;
            }
        }

        List<Long> configurationIds = new ArrayList<>();
        for (CartItem item : customItems) {
            configurationIds.add(item.getConfigurationId());
        }
        Map<Long, List<BoxConfigurationItem>> componentsByConfiguration =
            loadCustomComponents(configurationIds);

        for (CartItem item : customItems) {
            BoxConfiguration configuration = item.getConfiguration();
            List<BoxConfigurationItem> components = componentsByConfiguration.getOrDefault(
                configuration.getId(),
                List.of()
            );
            BigDecimal unitPrice = calculateCustomUnitPrice(configuration, components);
            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));
            boolean currentlySellable = isCustomConfigurationSellable(
                configuration,
                components
            );

            List<CustomCartComponentView> componentViews = new ArrayList<>();
            for (BoxConfigurationItem component : components) {
                ProductVariant cake = component.getMooncakeVariant();
                componentViews.add(new CustomCartComponentView(
                    cake.getProduct().getName(),
                    cake.getName(),
                    component.getQuantityPerBox()
                ));
            }

            ProductVariant boxVariant = configuration.getBox().getEmptyBoxVariant();
            CartVariantSnapshot boxSnapshot = variantsById.get(boxVariant.getId());
            if (boxSnapshot == null) {
                throw new ResourceNotFoundException(
                    "Không tìm thấy thông tin SKU vỏ hộp trong giỏ: "
                        + boxVariant.getId()
                );
            }
            CartItemView customView = CartItemView.custom(
                item.getId(),
                configuration.getId(),
                "Hộp quà tự chọn",
                boxVariant.getProduct().getName(),
                boxVariant.getSku(),
                boxSnapshot.imageUrl(),
                boxSnapshot.imageAlt(),
                item.getQuantity(),
                unitPrice,
                formatVnd(unitPrice),
                lineTotal,
                formatVnd(lineTotal),
                currentlySellable,
                configuration.getBox().getCapacityCakes(),
                componentViews
            );
            itemViews.add(customView);

            totalQuantity = addQuantities(totalQuantity, item.getQuantity());
            subtotal = subtotal.add(lineTotal);
            if (!currentlySellable) {
                hasUnavailableItems = true;
            }
        }

        return new CustomerCartView(
            cart.getId(),
            itemViews,
            totalQuantity,
            subtotal,
            formatVnd(subtotal),
            hasUnavailableItems
        );
    }

    private Map<Long, List<BoxConfigurationItem>> loadCustomComponents(
        List<Long> configurationIds
    ) {
        if (configurationIds.isEmpty()) {
            return Map.of();
        }

        List<BoxConfigurationItem> components = configurationItemRepository
            .findByConfigurationIds(configurationIds);
        Map<Long, List<BoxConfigurationItem>> result = new LinkedHashMap<>();
        for (BoxConfigurationItem component : components) {
            long configurationId = component.getConfiguration().getId();
            List<BoxConfigurationItem> configurationComponents = result.get(configurationId);
            if (configurationComponents == null) {
                configurationComponents = new ArrayList<>();
                result.put(configurationId, configurationComponents);
            }
            configurationComponents.add(component);
        }
        return result;
    }

    private BigDecimal calculateCustomUnitPrice(
        BoxConfiguration configuration,
        List<BoxConfigurationItem> components
    ) {
        BigDecimal unitPrice = configuration.getBox()
            .getEmptyBoxVariant()
            .getUnitPrice();
        for (BoxConfigurationItem component : components) {
            BigDecimal componentTotal = component.getMooncakeVariant()
                .getUnitPrice()
                .multiply(BigDecimal.valueOf(component.getQuantityPerBox()));
            unitPrice = unitPrice.add(componentTotal);
        }
        return unitPrice;
    }

    private boolean isCustomConfigurationSellable(
        BoxConfiguration configuration,
        List<BoxConfigurationItem> components
    ) {
        if (configuration.getType() != ConfigurationType.CUSTOM
            || configuration.getStatus() != ConfigurationStatus.READY) {
            return false;
        }
        if (!configuration.getBox().isActive()) {
            return false;
        }

        ProductVariant boxVariant = configuration.getBox().getEmptyBoxVariant();
        if (!isActiveVariantOfType(boxVariant, ProductType.EMPTY_BOX)) {
            return false;
        }

        int cakeCount = 0;
        for (BoxConfigurationItem component : components) {
            ProductVariant cake = component.getMooncakeVariant();
            if (!isActiveVariantOfType(cake, ProductType.MOONCAKE)) {
                return false;
            }
            cakeCount = addQuantities(cakeCount, component.getQuantityPerBox());
        }
        return cakeCount == configuration.getBox().getCapacityCakes();
    }

    private boolean isActiveVariantOfType(
        ProductVariant variant,
        ProductType expectedType
    ) {
        return variant.isActive()
            && variant.getProduct().isActive()
            && variant.getProduct().getCategory().isActive()
            && variant.getProduct().getProductType() == expectedType;
    }

    private void validateCustomConfigurationSellable(
        long configurationId,
        long cartId
    ) {
        BoxConfiguration configuration = requireOwnedCustomConfiguration(
            configurationId,
            cartId
        );
        List<BoxConfigurationItem> components = configurationItemRepository
            .findByConfigurationIds(List.of(configurationId));
        if (!isCustomConfigurationSellable(configuration, components)) {
            throw new BusinessRuleViolationException(
                "Hộp tự chọn không còn hợp lệ để tăng số lượng"
            );
        }
    }

    private BoxConfiguration requireOwnedCustomConfiguration(
        long configurationId,
        long cartId
    ) {
        Optional<BoxConfiguration> result = configurationRepository.findOwnedConfiguration(
            configurationId,
            cartId,
            ConfigurationType.CUSTOM
        );
        if (result.isEmpty()) {
            throw new ResourceNotFoundException(
                "Không tìm thấy cấu hình hộp tự chọn trong giỏ hàng của bạn"
            );
        }
        return result.get();
    }

    private Cart lockOrCreateActiveCart(long customerId) {
        Optional<Cart> existingCart = cartRepository.findByCustomerIdAndStatusForUpdate(
            customerId,
            CartStatus.ACTIVE
        );
        if (existingCart.isPresent()) {
            return existingCart.get();
        }

        cartRepository.createActiveCartIfMissing(customerId);
        Optional<Cart> createdCart = cartRepository.findByCustomerIdAndStatusForUpdate(
            customerId,
            CartStatus.ACTIVE
        );
        if (createdCart.isEmpty()) {
            throw new ConcurrentOperationException(
                "Không thể tạo giỏ hàng lúc này, vui lòng thử lại"
            );
        }
        return createdCart.get();
    }

    private Cart requireLockedActiveCart(long customerId) {
        Optional<Cart> cartResult = cartRepository.findByCustomerIdAndStatusForUpdate(
            customerId,
            CartStatus.ACTIVE
        );
        if (cartResult.isEmpty()) {
            throw new ResourceNotFoundException("Không tìm thấy giỏ hàng đang hoạt động");
        }
        return cartResult.get();
    }

    private CartItem requireOwnedItem(long cartId, long cartItemId) {
        Optional<CartItem> itemResult = cartItemRepository.findByIdAndCartId(
            cartItemId,
            cartId
        );
        if (itemResult.isEmpty()) {
            throw new ResourceNotFoundException(
                "Không tìm thấy sản phẩm trong giỏ hàng của bạn"
            );
        }
        return itemResult.get();
    }

    private int addQuantities(int first, int second) {
        try {
            return Math.addExact(first, second);
        } catch (ArithmeticException exception) {
            throw new BusinessRuleViolationException("Số lượng sản phẩm quá lớn");
        }
    }

    private void requireIdentity(ShopperIdentity customerIdentity) {
        if (customerIdentity == null) {
            throw new IllegalArgumentException("Customer identity must not be null");
        }
    }

    private void requirePositiveId(long id, String message) {
        if (id <= 0) {
            throw new BusinessRuleViolationException(message);
        }
    }

    private void requirePositiveQuantity(int quantity) {
        if (quantity <= 0) {
            throw new BusinessRuleViolationException("Số lượng phải lớn hơn 0");
        }
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    private String formatVnd(BigDecimal amount) {
        return NumberFormat.getNumberInstance(VIETNAMESE).format(amount) + "đ";
    }
}
