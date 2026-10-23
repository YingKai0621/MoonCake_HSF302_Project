package com.se201411.project.mooncake.cart.service.impl;

import com.se201411.project.mooncake.cart.dto.CartLineSnapshot;
import com.se201411.project.mooncake.cart.dto.CheckoutCartSnapshot;
import com.se201411.project.mooncake.cart.dto.CheckoutComponentQuote;
import com.se201411.project.mooncake.cart.dto.CheckoutLineQuote;
import com.se201411.project.mooncake.cart.dto.CheckoutQuote;
import com.se201411.project.mooncake.cart.dto.DiscountGroupSummary;
import com.se201411.project.mooncake.cart.repository.CustomBoxConfigurationItemRepository;
import com.se201411.project.mooncake.cart.repository.CustomBoxConfigurationRepository;
import com.se201411.project.mooncake.cart.repository.VolumeDiscountTierRepository;
import com.se201411.project.mooncake.cart.service.CashRoundingPolicy;
import com.se201411.project.mooncake.cart.service.PricingService;
import com.se201411.project.mooncake.common.exception.BusinessRuleViolationException;
import com.se201411.project.mooncake.common.exception.ResourceNotFoundException;
import com.se201411.project.mooncake.entity.BoxConfiguration;
import com.se201411.project.mooncake.entity.BoxConfigurationItem;
import com.se201411.project.mooncake.entity.ProductVariant;
import com.se201411.project.mooncake.entity.VolumeDiscountTier;
import com.se201411.project.mooncake.enums.ComponentRole;
import com.se201411.project.mooncake.enums.ConfigurationStatus;
import com.se201411.project.mooncake.enums.ConfigurationType;
import com.se201411.project.mooncake.enums.DiscountGroup;
import com.se201411.project.mooncake.enums.InventorySource;
import com.se201411.project.mooncake.enums.OrderItemKind;
import com.se201411.project.mooncake.enums.ProductType;
import com.se201411.project.mooncake.product.dto.CartVariantSnapshot;
import com.se201411.project.mooncake.product.dto.PresetComponentSnapshot;
import com.se201411.project.mooncake.product.dto.PresetRecipeSnapshot;
import com.se201411.project.mooncake.product.dto.SellableVariantSnapshot;
import com.se201411.project.mooncake.product.service.PresetConfigurationQueryService;
import com.se201411.project.mooncake.product.service.ProductQueryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class PricingServiceImpl implements PricingService {

    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal SHIPPING_FEE_SMALL = BigDecimal.valueOf(30_000);
    private static final BigDecimal SHIPPING_FEE_LARGE = BigDecimal.valueOf(150_000);
    private static final int LARGE_SHIPMENT_BOX_COUNT = 50;

    private final ProductQueryService productQueryService;
    private final PresetConfigurationQueryService presetQueryService;
    private final CustomBoxConfigurationRepository configurationRepository;
    private final CustomBoxConfigurationItemRepository configurationItemRepository;
    private final VolumeDiscountTierRepository tierRepository;
    private final Clock clock;

    public PricingServiceImpl(
        ProductQueryService productQueryService,
        PresetConfigurationQueryService presetQueryService,
        CustomBoxConfigurationRepository configurationRepository,
        CustomBoxConfigurationItemRepository configurationItemRepository,
        VolumeDiscountTierRepository tierRepository,
        Clock clock
    ) {
        this.productQueryService = productQueryService;
        this.presetQueryService = presetQueryService;
        this.configurationRepository = configurationRepository;
        this.configurationItemRepository = configurationItemRepository;
        this.tierRepository = tierRepository;
        this.clock = clock;
    }

    @Override
    public CheckoutQuote calculateCheckoutQuote(CheckoutCartSnapshot cart) {
        if (cart == null || cart.lines().isEmpty()) {
            throw new BusinessRuleViolationException("Giỏ hàng đang trống");
        }

        List<PricedLine> pricedLines = new ArrayList<>();
        for (CartLineSnapshot cartLine : cart.lines()) {
            if (cartLine.variantId() != null) {
                pricedLines.add(priceVariantLine(cartLine));
            } else {
                pricedLines.add(priceCustomLine(cart.cartId(), cartLine));
            }
        }

        Map<DiscountGroup, Integer> groupQuantities = calculateGroupQuantities(pricedLines);
        Map<DiscountGroup, VolumeDiscountTier> tiers = findTiers(groupQuantities);

        List<CheckoutLineQuote> quoteLines = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal discount = BigDecimal.ZERO;
        Map<DiscountGroup, BigDecimal> groupSubtotals = zeroMoneyByGroup();
        Map<DiscountGroup, BigDecimal> groupDiscounts = zeroMoneyByGroup();

        for (PricedLine pricedLine : pricedLines) {
            VolumeDiscountTier tier = tiers.get(pricedLine.discountGroup());
            BigDecimal discountPercent = BigDecimal.ZERO;
            Long tierId = null;
            if (tier != null) {
                discountPercent = tier.getDiscountPercent();
                tierId = tier.getId();
            }

            BigDecimal unitDiscount = calculateUnitDiscount(
                pricedLine.unitBasePrice(),
                discountPercent
            );
            BigDecimal unitFinalPrice = pricedLine.unitBasePrice().subtract(unitDiscount);
            BigDecimal quantity = BigDecimal.valueOf(pricedLine.cartLine().quantity());
            BigDecimal lineSubtotal = pricedLine.unitBasePrice().multiply(quantity);
            BigDecimal lineDiscount = unitDiscount.multiply(quantity);
            BigDecimal lineTotal = unitFinalPrice.multiply(quantity);

            subtotal = subtotal.add(lineSubtotal);
            discount = discount.add(lineDiscount);
            addGroupMoney(groupSubtotals, pricedLine.discountGroup(), lineSubtotal);
            addGroupMoney(groupDiscounts, pricedLine.discountGroup(), lineDiscount);

            quoteLines.add(new CheckoutLineQuote(
                pricedLine.cartLine(),
                tierId,
                pricedLine.itemKind(),
                pricedLine.inventorySource(),
                pricedLine.productName(),
                pricedLine.sku(),
                pricedLine.discountGroup(),
                pricedLine.boxName(),
                pricedLine.boxSku(),
                pricedLine.unitBasePrice(),
                discountPercent,
                unitDiscount,
                unitFinalPrice,
                lineTotal,
                pricedLine.components()
            ));
        }

        int giftBoxQuantity = groupQuantities.getOrDefault(DiscountGroup.GIFT_BOX, 0);
        BigDecimal shippingFee = giftBoxQuantity >= LARGE_SHIPMENT_BOX_COUNT
            ? SHIPPING_FEE_LARGE
            : SHIPPING_FEE_SMALL;
        BigDecimal totalBeforeRounding = subtotal.subtract(discount).add(shippingFee);
        BigDecimal total = CashRoundingPolicy.roundToNearestThousand(totalBeforeRounding);
        BigDecimal roundingAdjustment = total.subtract(totalBeforeRounding);

        List<DiscountGroupSummary> summaries = buildSummaries(
            groupQuantities,
            groupSubtotals,
            groupDiscounts
        );
        return new CheckoutQuote(
            quoteLines,
            summaries,
            subtotal,
            discount,
            shippingFee,
            roundingAdjustment,
            total
        );
    }

    private PricedLine priceVariantLine(CartLineSnapshot cartLine) {
        SellableVariantSnapshot variant = productQueryService.requireSellableVariant(
            cartLine.variantId()
        );

        DiscountGroup discountGroup = null;
        List<CheckoutComponentQuote> components = List.of();
        String boxName = null;
        String boxSku = null;

        if (variant.productType() == ProductType.MOONCAKE) {
            discountGroup = DiscountGroup.MOONCAKE;
        } else if (variant.productType() == ProductType.GIFT_SET) {
            discountGroup = DiscountGroup.GIFT_BOX;
            PresetRecipeDetails recipeDetails = loadPresetRecipe(variant.variantId());
            components = recipeDetails.components();
            boxName = recipeDetails.boxName();
            boxSku = recipeDetails.boxSku();
        }

        return new PricedLine(
            cartLine,
            OrderItemKind.STANDARD,
            InventorySource.STOCKED_SKU,
            variant.displayName(),
            variant.sku(),
            discountGroup,
            boxName,
            boxSku,
            variant.unitPrice(),
            components
        );
    }

    private PresetRecipeDetails loadPresetRecipe(long giftSetVariantId) {
        PresetRecipeSnapshot recipe = presetQueryService.requireReadyPreset(giftSetVariantId);

        List<Long> componentIds = new ArrayList<>();
        componentIds.add(recipe.boxVariantId());
        int cakeCount = 0;
        for (PresetComponentSnapshot component : recipe.components()) {
            componentIds.add(component.variantId());
            cakeCount = Math.addExact(cakeCount, component.quantityPerBox());
        }
        if (cakeCount != recipe.capacityCakes()) {
            throw new BusinessRuleViolationException(
                "Công thức hộp quà có sẵn không đúng sức chứa"
            );
        }

        Map<Long, CartVariantSnapshot> variants = loadVariantSnapshots(componentIds);
        CartVariantSnapshot box = requireVariant(variants, recipe.boxVariantId());
        List<CheckoutComponentQuote> components = new ArrayList<>();
        components.add(new CheckoutComponentQuote(
            box.variantId(),
            ComponentRole.BOX,
            box.displayName(),
            1,
            box.unitPrice()
        ));

        for (PresetComponentSnapshot component : recipe.components()) {
            CartVariantSnapshot cake = requireVariant(variants, component.variantId());
            components.add(new CheckoutComponentQuote(
                cake.variantId(),
                ComponentRole.CAKE,
                cake.displayName(),
                component.quantityPerBox(),
                cake.unitPrice()
            ));
        }

        return new PresetRecipeDetails(
            box.displayName(),
            box.sku(),
            List.copyOf(components)
        );
    }

    private PricedLine priceCustomLine(long cartId, CartLineSnapshot cartLine) {
        long configurationId = cartLine.configurationId();
        Optional<BoxConfiguration> configurationResult = configurationRepository
            .findOwnedConfiguration(
                configurationId,
                cartId,
                ConfigurationType.CUSTOM
            );
        if (configurationResult.isEmpty()) {
            throw new ResourceNotFoundException(
                "Không tìm thấy hộp tự chọn trong giỏ hàng"
            );
        }
        BoxConfiguration configuration = configurationResult.get();

        if (configuration.getStatus() != ConfigurationStatus.READY) {
            throw new BusinessRuleViolationException(
                "Hộp tự chọn chưa hoàn tất thành phần"
            );
        }

        ProductVariant boxVariant = configuration.getBox().getEmptyBoxVariant();
        if (!configuration.getBox().isActive()) {
            throw new BusinessRuleViolationException("Mẫu hộp không còn được bán");
        }
        requireActiveVariant(boxVariant, ProductType.EMPTY_BOX, "Vỏ hộp không còn được bán");

        List<BoxConfigurationItem> items = configurationItemRepository
            .findByConfigurationIds(List.of(configurationId));
        List<CheckoutComponentQuote> components = new ArrayList<>();
        components.add(new CheckoutComponentQuote(
            boxVariant.getId(),
            ComponentRole.BOX,
            displayName(boxVariant),
            1,
            boxVariant.getUnitPrice()
        ));

        int cakeCount = 0;
        BigDecimal unitBasePrice = boxVariant.getUnitPrice();
        for (BoxConfigurationItem item : items) {
            ProductVariant cake = item.getMooncakeVariant();
            requireActiveVariant(cake, ProductType.MOONCAKE, "Bánh trong hộp không còn được bán");
            cakeCount = Math.addExact(cakeCount, item.getQuantityPerBox());
            BigDecimal componentTotal = cake.getUnitPrice().multiply(
                BigDecimal.valueOf(item.getQuantityPerBox())
            );
            unitBasePrice = unitBasePrice.add(componentTotal);
            components.add(new CheckoutComponentQuote(
                cake.getId(),
                ComponentRole.CAKE,
                displayName(cake),
                item.getQuantityPerBox(),
                cake.getUnitPrice()
            ));
        }

        if (cakeCount != configuration.getBox().getCapacityCakes()) {
            throw new BusinessRuleViolationException(
                "Hộp tự chọn phải có đúng "
                    + configuration.getBox().getCapacityCakes()
                    + " bánh"
            );
        }

        return new PricedLine(
            cartLine,
            OrderItemKind.CUSTOM,
            InventorySource.COMPONENTS,
            "Hộp quà tự chọn - " + boxVariant.getProduct().getName(),
            null,
            DiscountGroup.GIFT_BOX,
            displayName(boxVariant),
            boxVariant.getSku(),
            unitBasePrice,
            List.copyOf(components)
        );
    }

    private Map<Long, CartVariantSnapshot> loadVariantSnapshots(List<Long> variantIds) {
        List<CartVariantSnapshot> snapshots = productQueryService.findVariantsForCart(variantIds);
        Map<Long, CartVariantSnapshot> result = new LinkedHashMap<>();
        for (CartVariantSnapshot snapshot : snapshots) {
            result.put(snapshot.variantId(), snapshot);
        }
        return result;
    }

    private CartVariantSnapshot requireVariant(
        Map<Long, CartVariantSnapshot> variants,
        long variantId
    ) {
        CartVariantSnapshot variant = variants.get(variantId);
        if (variant == null) {
            throw new ResourceNotFoundException("Không tìm thấy SKU thành phần: " + variantId);
        }
        return variant;
    }

    private void requireActiveVariant(
        ProductVariant variant,
        ProductType expectedType,
        String errorMessage
    ) {
        boolean valid = variant.isActive()
            && variant.getProduct().isActive()
            && variant.getProduct().getCategory().isActive()
            && variant.getProduct().getProductType() == expectedType;
        if (!valid) {
            throw new BusinessRuleViolationException(errorMessage);
        }
    }

    private String displayName(ProductVariant variant) {
        return variant.getProduct().getName() + " - " + variant.getName();
    }

    private Map<DiscountGroup, Integer> calculateGroupQuantities(
        List<PricedLine> pricedLines
    ) {
        Map<DiscountGroup, Integer> quantities = new EnumMap<>(DiscountGroup.class);
        for (PricedLine line : pricedLines) {
            if (line.discountGroup() == null) {
                continue;
            }
            int current = quantities.getOrDefault(line.discountGroup(), 0);
            quantities.put(
                line.discountGroup(),
                Math.addExact(current, line.cartLine().quantity())
            );
        }
        return quantities;
    }

    private Map<DiscountGroup, VolumeDiscountTier> findTiers(
        Map<DiscountGroup, Integer> groupQuantities
    ) {
        Map<DiscountGroup, VolumeDiscountTier> result = new EnumMap<>(DiscountGroup.class);
        LocalDateTime now = LocalDateTime.now(clock);
        for (DiscountGroup group : DiscountGroup.values()) {
            int quantity = groupQuantities.getOrDefault(group, 0);
            if (quantity == 0) {
                continue;
            }

            List<VolumeDiscountTier> matches = tierRepository.findApplicableTiers(
                group,
                quantity,
                now
            );
            if (matches.size() > 1) {
                throw new BusinessRuleViolationException(
                    "Các bậc chiết khấu " + group + " đang bị chồng khoảng"
                );
            }
            if (!matches.isEmpty()) {
                result.put(group, matches.getFirst());
            }
        }
        return result;
    }

    private BigDecimal calculateUnitDiscount(
        BigDecimal unitBasePrice,
        BigDecimal discountPercent
    ) {
        return unitBasePrice.multiply(discountPercent)
            .divide(ONE_HUNDRED, 0, RoundingMode.HALF_UP);
    }

    private Map<DiscountGroup, BigDecimal> zeroMoneyByGroup() {
        Map<DiscountGroup, BigDecimal> result = new EnumMap<>(DiscountGroup.class);
        result.put(DiscountGroup.GIFT_BOX, BigDecimal.ZERO);
        result.put(DiscountGroup.MOONCAKE, BigDecimal.ZERO);
        return result;
    }

    private void addGroupMoney(
        Map<DiscountGroup, BigDecimal> totals,
        DiscountGroup group,
        BigDecimal amount
    ) {
        if (group == null) {
            return;
        }
        totals.put(group, totals.get(group).add(amount));
    }

    private List<DiscountGroupSummary> buildSummaries(
        Map<DiscountGroup, Integer> quantities,
        Map<DiscountGroup, BigDecimal> subtotals,
        Map<DiscountGroup, BigDecimal> discounts
    ) {
        List<DiscountGroupSummary> summaries = new ArrayList<>();
        for (DiscountGroup group : DiscountGroup.values()) {
            int quantity = quantities.getOrDefault(group, 0);
            if (quantity > 0) {
                summaries.add(new DiscountGroupSummary(
                    group,
                    quantity,
                    subtotals.get(group),
                    discounts.get(group)
                ));
            }
        }
        return List.copyOf(summaries);
    }

    private record PricedLine(
        CartLineSnapshot cartLine,
        OrderItemKind itemKind,
        InventorySource inventorySource,
        String productName,
        String sku,
        DiscountGroup discountGroup,
        String boxName,
        String boxSku,
        BigDecimal unitBasePrice,
        List<CheckoutComponentQuote> components
    ) {
    }

    private record PresetRecipeDetails(
        String boxName,
        String boxSku,
        List<CheckoutComponentQuote> components
    ) {
    }
}
