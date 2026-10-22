package com.se201411.project.mooncake.cart.service.impl;

import com.se201411.project.mooncake.cart.dto.CustomBoxEditView;
import com.se201411.project.mooncake.cart.dto.CustomBoxOptionView;
import com.se201411.project.mooncake.cart.dto.CustomBoxPageView;
import com.se201411.project.mooncake.cart.dto.CustomCakeOptionView;
import com.se201411.project.mooncake.cart.dto.CustomCakeQuantity;
import com.se201411.project.mooncake.cart.dto.SaveCustomBoxCommand;
import com.se201411.project.mooncake.cart.repository.CartItemRepository;
import com.se201411.project.mooncake.cart.repository.CartRepository;
import com.se201411.project.mooncake.cart.repository.CustomBoxConfigurationItemRepository;
import com.se201411.project.mooncake.cart.repository.CustomBoxConfigurationRepository;
import com.se201411.project.mooncake.cart.repository.CustomBoxRepository;
import com.se201411.project.mooncake.cart.service.CustomBoxService;
import com.se201411.project.mooncake.common.exception.BusinessRuleViolationException;
import com.se201411.project.mooncake.common.exception.ConcurrentOperationException;
import com.se201411.project.mooncake.common.exception.ResourceNotFoundException;
import com.se201411.project.mooncake.entity.Box;
import com.se201411.project.mooncake.entity.BoxConfiguration;
import com.se201411.project.mooncake.entity.BoxConfigurationItem;
import com.se201411.project.mooncake.entity.Cart;
import com.se201411.project.mooncake.entity.CartItem;
import com.se201411.project.mooncake.entity.ProductVariant;
import com.se201411.project.mooncake.enums.CartStatus;
import com.se201411.project.mooncake.enums.ConfigurationStatus;
import com.se201411.project.mooncake.enums.ConfigurationType;
import com.se201411.project.mooncake.enums.ProductType;
import com.se201411.project.mooncake.product.dto.CatalogProductView;
import com.se201411.project.mooncake.product.dto.CatalogVariantView;
import com.se201411.project.mooncake.product.dto.SellableVariantSnapshot;
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
public class CustomBoxServiceImpl implements CustomBoxService {

    private static final Locale VIETNAMESE = Locale.forLanguageTag("vi-VN");

    private final CustomBoxRepository customBoxRepository;
    private final CustomBoxConfigurationRepository configurationRepository;
    private final CustomBoxConfigurationItemRepository configurationItemRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductQueryService productQueryService;
    private final EntityManager entityManager;
    private final Clock clock;

    public CustomBoxServiceImpl(
        CustomBoxRepository customBoxRepository,
        CustomBoxConfigurationRepository configurationRepository,
        CustomBoxConfigurationItemRepository configurationItemRepository,
        CartRepository cartRepository,
        CartItemRepository cartItemRepository,
        ProductQueryService productQueryService,
        EntityManager entityManager,
        Clock clock
    ) {
        this.customBoxRepository = customBoxRepository;
        this.configurationRepository = configurationRepository;
        this.configurationItemRepository = configurationItemRepository;
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productQueryService = productQueryService;
        this.entityManager = entityManager;
        this.clock = clock;
    }

    @Override
    public CustomBoxPageView getBuilderView() {
        List<Box> boxes = customBoxRepository.findSellableBoxes(ProductType.EMPTY_BOX);
        Map<Long, CatalogVariantView> boxCatalogVariants = loadCatalogVariants(
            ProductType.EMPTY_BOX
        );
        List<CustomBoxOptionView> boxViews = new ArrayList<>();
        for (Box box : boxes) {
            ProductVariant variant = box.getEmptyBoxVariant();
            CatalogVariantView catalogVariant = boxCatalogVariants.get(variant.getId());
            if (catalogVariant == null) {
                throw new ResourceNotFoundException("Không tìm thấy ảnh của mẫu hộp đang bán");
            }
            boxViews.add(new CustomBoxOptionView(
                box.getId(),
                variant.getId(),
                variant.getProduct().getName(),
                variant.getSku(),
                box.getCapacityCakes(),
                variant.getUnitPrice(),
                formatVnd(variant.getUnitPrice()),
                catalogVariant.imageUrl(),
                catalogVariant.imageAlt()
            ));
        }

        List<CatalogProductView> products = productQueryService
            .findActiveCatalog(ProductType.MOONCAKE);
        List<CustomCakeOptionView> cakeViews = new ArrayList<>();
        for (CatalogProductView product : products) {
            for (CatalogVariantView variant : product.getVariants()) {
                cakeViews.add(new CustomCakeOptionView(
                    variant.id(),
                    product.getName(),
                    variant.name(),
                    variant.sku(),
                    variant.unitPrice(),
                    variant.formattedPrice(),
                    variant.imageUrl(),
                    variant.imageAlt()
                ));
            }
        }
        return new CustomBoxPageView(boxViews, cakeViews);
    }

    private Map<Long, CatalogVariantView> loadCatalogVariants(ProductType productType) {
        List<CatalogProductView> products = productQueryService.findActiveCatalog(productType);
        Map<Long, CatalogVariantView> variants = new LinkedHashMap<>();
        for (CatalogProductView product : products) {
            for (CatalogVariantView variant : product.getVariants()) {
                variants.put(variant.id(), variant);
            }
        }
        return variants;
    }

    @Override
    public CustomBoxEditView getConfigurationForEdit(
        ShopperIdentity customerIdentity,
        long configurationId
    ) {
        requireIdentity(customerIdentity);
        requirePositiveId(configurationId, "Cấu hình hộp không hợp lệ");

        Cart cart = requireActiveCart(customerIdentity.customerId(), false);
        BoxConfiguration configuration = requireOwnedConfiguration(
            cart.getId(),
            configurationId
        );
        List<BoxConfigurationItem> items = configurationItemRepository
            .findByConfigurationIds(List.of(configurationId));

        Map<Long, Integer> quantities = new LinkedHashMap<>();
        for (BoxConfigurationItem item : items) {
            quantities.put(item.getMooncakeVariant().getId(), item.getQuantityPerBox());
        }

        Optional<CartItem> cartItem = cartItemRepository.findConfigurationItem(
            cart.getId(),
            configurationId
        );
        if (cartItem.isEmpty()) {
            throw new ResourceNotFoundException("Không tìm thấy hộp tự chọn trong giỏ hàng");
        }

        return new CustomBoxEditView(
            configurationId,
            configuration.getBox().getId(),
            cartItem.get().getQuantity(),
            quantities
        );
    }

    @Override
    @Transactional
    public long createAndAddToCart(
        ShopperIdentity customerIdentity,
        SaveCustomBoxCommand command
    ) {
        requireIdentity(customerIdentity);
        validateCommandShape(command);

        Cart cart = requireActiveCart(customerIdentity.customerId(), true);
        ValidatedCustomBox validated = validateCustomBox(command);
        LocalDateTime currentTime = now();

        BoxConfiguration configuration = new BoxConfiguration();
        configuration.setBox(validated.box());
        configuration.setCart(cart);
        configuration.setPresetVariant(null);
        configuration.setType(ConfigurationType.CUSTOM);
        configuration.setStatus(ConfigurationStatus.READY);
        configuration.setCreatedAt(currentTime);
        configuration.setUpdatedAt(currentTime);
        configurationRepository.save(configuration);

        saveConfigurationItems(configuration, validated.cakeQuantities());

        CartItem cartItem = new CartItem();
        cartItem.setCart(cart);
        cartItem.setVariant(null);
        cartItem.setConfiguration(configuration);
        cartItem.setQuantity(command.boxQuantity());
        cartItem.setAddedAt(currentTime);
        cartItemRepository.save(cartItem);

        cart.setUpdatedAt(currentTime);
        return configuration.getId();
    }

    @Override
    @Transactional
    public void updateConfiguration(
        ShopperIdentity customerIdentity,
        long configurationId,
        SaveCustomBoxCommand command
    ) {
        requireIdentity(customerIdentity);
        requirePositiveId(configurationId, "Cấu hình hộp không hợp lệ");
        validateCommandShape(command);

        Cart cart = requireActiveCart(customerIdentity.customerId(), false);
        BoxConfiguration configuration = requireOwnedConfiguration(
            cart.getId(),
            configurationId
        );
        ValidatedCustomBox validated = validateCustomBox(command);

        Optional<CartItem> cartItemResult = cartItemRepository.findConfigurationItem(
            cart.getId(),
            configurationId
        );
        if (cartItemResult.isEmpty()) {
            throw new ResourceNotFoundException("Không tìm thấy hộp tự chọn trong giỏ hàng");
        }

        deleteConfigurationItems(configurationId);
        configuration.setBox(validated.box());
        configuration.setStatus(ConfigurationStatus.READY);
        configuration.setUpdatedAt(now());
        saveConfigurationItems(configuration, validated.cakeQuantities());

        CartItem cartItem = cartItemResult.get();
        cartItem.setQuantity(command.boxQuantity());
        cart.setUpdatedAt(now());
    }

    private ValidatedCustomBox validateCustomBox(SaveCustomBoxCommand command) {
        Optional<Box> boxResult = customBoxRepository.findSellableBoxById(
            command.boxId(),
            ProductType.EMPTY_BOX
        );
        if (boxResult.isEmpty()) {
            throw new ResourceNotFoundException("Mẫu hộp đã ngừng bán hoặc không tồn tại");
        }
        Box box = boxResult.get();

        Map<Long, Integer> quantities = aggregateCakeQuantities(command.cakes());
        int selectedCakeCount = 0;
        for (Integer quantity : quantities.values()) {
            selectedCakeCount = addQuantities(selectedCakeCount, quantity);
        }
        if (selectedCakeCount != box.getCapacityCakes()) {
            throw new BusinessRuleViolationException(
                "Hộp " + box.getCapacityCakes()
                    + " bánh phải chọn đúng " + box.getCapacityCakes() + " bánh"
            );
        }

        for (Long variantId : quantities.keySet()) {
            SellableVariantSnapshot variant = productQueryService
                .requireSellableVariant(variantId);
            if (variant.productType() != ProductType.MOONCAKE) {
                throw new BusinessRuleViolationException(
                    "Hộp tự chọn chỉ được chứa SKU bánh Trung Thu"
                );
            }
        }
        return new ValidatedCustomBox(box, quantities);
    }

    private Map<Long, Integer> aggregateCakeQuantities(
        List<CustomCakeQuantity> selections
    ) {
        Map<Long, Integer> quantities = new LinkedHashMap<>();
        for (CustomCakeQuantity selection : selections) {
            if (selection.variantId() <= 0 || selection.quantity() < 0) {
                throw new BusinessRuleViolationException("Lựa chọn bánh không hợp lệ");
            }
            if (selection.quantity() == 0) {
                continue;
            }

            int currentQuantity = quantities.getOrDefault(selection.variantId(), 0);
            int combinedQuantity = addQuantities(currentQuantity, selection.quantity());
            quantities.put(selection.variantId(), combinedQuantity);
        }
        return quantities;
    }

    private void saveConfigurationItems(
        BoxConfiguration configuration,
        Map<Long, Integer> cakeQuantities
    ) {
        for (Map.Entry<Long, Integer> entry : cakeQuantities.entrySet()) {
            BoxConfigurationItem item = new BoxConfigurationItem();
            item.setConfiguration(configuration);
            ProductVariant cakeReference = entityManager.getReference(
                ProductVariant.class,
                entry.getKey()
            );
            item.setMooncakeVariant(cakeReference);
            item.setQuantityPerBox(entry.getValue());
            configurationItemRepository.save(item);
        }
    }

    private void deleteConfigurationItems(long configurationId) {
        List<BoxConfigurationItem> existingItems = configurationItemRepository
            .findByConfigurationIds(List.of(configurationId));
        configurationItemRepository.deleteAll(existingItems);
        entityManager.flush();
    }

    private BoxConfiguration requireOwnedConfiguration(
        long cartId,
        long configurationId
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

    private Cart requireActiveCart(long customerId, boolean createIfMissing) {
        Optional<Cart> cartResult = cartRepository.findByCustomerIdAndStatusForUpdate(
            customerId,
            CartStatus.ACTIVE
        );
        if (cartResult.isPresent()) {
            return cartResult.get();
        }
        if (!createIfMissing) {
            throw new ResourceNotFoundException("Không tìm thấy giỏ hàng đang hoạt động");
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

    private void validateCommandShape(SaveCustomBoxCommand command) {
        if (command == null) {
            throw new BusinessRuleViolationException("Dữ liệu hộp tự chọn không hợp lệ");
        }
        requirePositiveId(command.boxId(), "Mẫu hộp không hợp lệ");
        if (command.boxQuantity() <= 0) {
            throw new BusinessRuleViolationException("Số lượng hộp phải lớn hơn 0");
        }
    }

    private int addQuantities(int first, int second) {
        try {
            return Math.addExact(first, second);
        } catch (ArithmeticException exception) {
            throw new BusinessRuleViolationException("Số lượng bánh quá lớn");
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

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    private String formatVnd(BigDecimal amount) {
        return NumberFormat.getNumberInstance(VIETNAMESE).format(amount) + "đ";
    }

    private record ValidatedCustomBox(
        Box box,
        Map<Long, Integer> cakeQuantities
    ) {
    }
}
