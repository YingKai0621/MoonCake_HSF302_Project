package com.se201411.project.mooncake.product.service.impl;

import com.se201411.project.mooncake.common.exception.ResourceNotFoundException;
import com.se201411.project.mooncake.entity.BoxConfiguration;
import com.se201411.project.mooncake.entity.BoxConfigurationItem;
import com.se201411.project.mooncake.entity.Product;
import com.se201411.project.mooncake.entity.ProductImage;
import com.se201411.project.mooncake.entity.ProductVariant;
import com.se201411.project.mooncake.enums.ConfigurationStatus;
import com.se201411.project.mooncake.enums.ConfigurationType;
import com.se201411.project.mooncake.enums.ProductType;
import com.se201411.project.mooncake.product.dto.CatalogProductView;
import com.se201411.project.mooncake.product.dto.CatalogVariantView;
import com.se201411.project.mooncake.product.dto.PresetComponentView;
import com.se201411.project.mooncake.product.dto.PresetRecipeView;
import com.se201411.project.mooncake.product.dto.SellableVariantSnapshot;
import com.se201411.project.mooncake.product.repository.PresetConfigurationItemRepository;
import com.se201411.project.mooncake.product.repository.PresetConfigurationRepository;
import com.se201411.project.mooncake.product.repository.ProductImageRepository;
import com.se201411.project.mooncake.product.repository.ProductRepository;
import com.se201411.project.mooncake.product.repository.ProductVariantRepository;
import com.se201411.project.mooncake.product.service.ProductQueryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class ProductQueryServiceImpl implements ProductQueryService {

    private static final String FALLBACK_IMAGE = "/images/branding/mooncake_luxury_emblem_v4.png";
    private static final Locale VIETNAMESE = Locale.forLanguageTag("vi-VN");

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductImageRepository productImageRepository;
    private final PresetConfigurationRepository presetConfigurationRepository;
    private final PresetConfigurationItemRepository presetConfigurationItemRepository;

    public ProductQueryServiceImpl(
        ProductRepository productRepository,
        ProductVariantRepository productVariantRepository,
        ProductImageRepository productImageRepository,
        PresetConfigurationRepository presetConfigurationRepository,
        PresetConfigurationItemRepository presetConfigurationItemRepository
    ) {
        this.productRepository = productRepository;
        this.productVariantRepository = productVariantRepository;
        this.productImageRepository = productImageRepository;
        this.presetConfigurationRepository = presetConfigurationRepository;
        this.presetConfigurationItemRepository = presetConfigurationItemRepository;
    }

    @Override
    public List<CatalogProductView> findActiveCatalog(ProductType productType) {
        Objects.requireNonNull(productType, "productType must not be null");
        return loadCatalogViews(productRepository.findActiveByType(productType));
    }

    @Override
    public CatalogProductView requireActiveProduct(String slug) {
        if (slug == null || slug.isBlank()) {
            throw new ResourceNotFoundException("Không tìm thấy sản phẩm");
        }

        Optional<Product> productResult = productRepository.findActiveBySlug(slug.trim());
        if (productResult.isEmpty()) {
            throw new ResourceNotFoundException("Không tìm thấy sản phẩm: " + slug);
        }
        Product product = productResult.get();

        List<CatalogProductView> catalogProducts = loadCatalogViews(List.of(product));
        if (catalogProducts.isEmpty()) {
            throw new ResourceNotFoundException(
                "Sản phẩm hiện không có SKU đang bán: " + slug
            );
        }
        return catalogProducts.getFirst();
    }

    @Override
    public SellableVariantSnapshot requireSellableVariant(long variantId) {
        Optional<ProductVariant> variantResult = productVariantRepository.findSellableById(variantId);
        if (variantResult.isEmpty()) {
            throw new ResourceNotFoundException("Không tìm thấy SKU đang bán: " + variantId);
        }
        ProductVariant variant = variantResult.get();

        if (variant.getProduct().getProductType() == ProductType.GIFT_SET
            && presetConfigurationRepository.findReadyByGiftSetVariantId(
                variantId,
                ConfigurationType.PRESET,
                ConfigurationStatus.READY
            ).isEmpty()) {
            throw new ResourceNotFoundException("SKU hộp quà chưa có PRESET READY: " + variantId);
        }

        Product product = variant.getProduct();
        return new SellableVariantSnapshot(
            variant.getId(),
            product.getId(),
            variant.getSku(),
            product.getProductType(),
            product.getName() + " - " + variant.getName(),
            variant.getUnitPrice(),
            true
        );
    }

    private List<CatalogProductView> loadCatalogViews(List<Product> products) {
        if (products.isEmpty()) {
            return List.of();
        }

        List<Long> productIds = new ArrayList<>();
        for (Product product : products) {
            productIds.add(product.getId());
        }

        List<ProductVariant> activeVariants = productVariantRepository
            .findActiveByProductIds(productIds);

        Map<Long, BoxConfiguration> presetsByVariantId = loadReadyPresets(activeVariants);
        List<ProductVariant> sellableVariants = new ArrayList<>();
        for (ProductVariant variant : activeVariants) {
            boolean isGiftSet = variant.getProduct().getProductType() == ProductType.GIFT_SET;
            boolean hasReadyPreset = presetsByVariantId.containsKey(variant.getId());
            if (!isGiftSet || hasReadyPreset) {
                sellableVariants.add(variant);
            }
        }

        Map<Long, List<ProductImage>> imagesByVariantId = loadImages(sellableVariants);
        Map<Long, List<BoxConfigurationItem>> itemsByConfigurationId = loadPresetItems(
            presetsByVariantId.values()
        );
        Map<Long, List<ProductVariant>> variantsByProductId = groupVariantsByProduct(
            sellableVariants
        );

        List<CatalogProductView> result = new ArrayList<>();
        for (Product product : products) {
            List<ProductVariant> productVariants = variantsByProductId
                .getOrDefault(product.getId(), List.of());
            if (productVariants.isEmpty()) {
                continue;
            }

            List<CatalogVariantView> variantViews = new ArrayList<>();
            for (ProductVariant variant : productVariants) {
                CatalogVariantView variantView = toVariantView(
                    product,
                    variant,
                    imagesByVariantId.getOrDefault(variant.getId(), List.of()),
                    presetsByVariantId.get(variant.getId()),
                    itemsByConfigurationId
                );
                variantViews.add(variantView);
            }

            result.add(new CatalogProductView(
                product.getId(),
                product.getSlug(),
                product.getProductType(),
                product.getName(),
                product.getShortDescription(),
                product.getDetailedDescription(),
                product.getMainIngredients(),
                product.getShelfLifeInfo(),
                product.getStorageGuidance(),
                variantViews
            ));
        }
        return List.copyOf(result);
    }

    private Map<Long, BoxConfiguration> loadReadyPresets(List<ProductVariant> variants) {
        List<Long> giftSetVariantIds = new ArrayList<>();
        for (ProductVariant variant : variants) {
            if (variant.getProduct().getProductType() == ProductType.GIFT_SET) {
                giftSetVariantIds.add(variant.getId());
            }
        }
        if (giftSetVariantIds.isEmpty()) {
            return Map.of();
        }

        List<BoxConfiguration> configurations = presetConfigurationRepository
            .findReadyByGiftSetVariantIds(
                giftSetVariantIds,
                ConfigurationType.PRESET,
                ConfigurationStatus.READY
            );

        Map<Long, BoxConfiguration> presetsByVariantId = new LinkedHashMap<>();
        for (BoxConfiguration configuration : configurations) {
            long variantId = configuration.getPresetVariant().getId();
            presetsByVariantId.put(variantId, configuration);
        }
        return Map.copyOf(presetsByVariantId);
    }

    private Map<Long, List<ProductImage>> loadImages(List<ProductVariant> variants) {
        if (variants.isEmpty()) {
            return Map.of();
        }

        List<Long> variantIds = new ArrayList<>();
        for (ProductVariant variant : variants) {
            variantIds.add(variant.getId());
        }

        List<ProductImage> images = productImageRepository.findOrderedByVariantIds(variantIds);
        Map<Long, List<ProductImage>> imagesByVariantId = new LinkedHashMap<>();
        for (ProductImage image : images) {
            long variantId = image.getVariant().getId();
            List<ProductImage> variantImages = imagesByVariantId.get(variantId);
            if (variantImages == null) {
                variantImages = new ArrayList<>();
                imagesByVariantId.put(variantId, variantImages);
            }
            variantImages.add(image);
        }
        return imagesByVariantId;
    }

    private Map<Long, List<BoxConfigurationItem>> loadPresetItems(
        Collection<BoxConfiguration> configurations
    ) {
        if (configurations.isEmpty()) {
            return Map.of();
        }

        List<Long> configurationIds = new ArrayList<>();
        for (BoxConfiguration configuration : configurations) {
            configurationIds.add(configuration.getId());
        }

        List<BoxConfigurationItem> items = presetConfigurationItemRepository
            .findByConfigurationIds(configurationIds);
        Map<Long, List<BoxConfigurationItem>> itemsByConfigurationId = new LinkedHashMap<>();
        for (BoxConfigurationItem item : items) {
            long configurationId = item.getConfiguration().getId();
            List<BoxConfigurationItem> configurationItems = itemsByConfigurationId
                .get(configurationId);
            if (configurationItems == null) {
                configurationItems = new ArrayList<>();
                itemsByConfigurationId.put(configurationId, configurationItems);
            }
            configurationItems.add(item);
        }
        return itemsByConfigurationId;
    }

    private CatalogVariantView toVariantView(
        Product product,
        ProductVariant variant,
        List<ProductImage> images,
        BoxConfiguration preset,
        Map<Long, List<BoxConfigurationItem>> itemsByConfigurationId
    ) {
        ProductImage primaryImage = null;
        if (!images.isEmpty()) {
            primaryImage = images.getFirst();
        }

        String imageUrl = FALLBACK_IMAGE;
        String imageAlt = product.getName() + " - " + variant.getName();
        if (primaryImage != null) {
            imageUrl = primaryImage.getImageUrl();
            if (primaryImage.getAltText() != null) {
                imageAlt = primaryImage.getAltText();
            }
        }

        PresetRecipeView presetView = null;
        if (preset != null) {
            List<BoxConfigurationItem> presetItems = itemsByConfigurationId.getOrDefault(
                preset.getId(),
                List.of()
            );
            presetView = toPresetView(preset, presetItems);
        }

        return new CatalogVariantView(
            variant.getId(),
            variant.getSku(),
            variant.getName(),
            variant.getWeightGrams(),
            variant.getFillingLabel(),
            variant.getUnitPrice(),
            formatVnd(variant.getUnitPrice()),
            imageUrl,
            imageAlt,
            presetView
        );
    }

    private PresetRecipeView toPresetView(
        BoxConfiguration preset,
        List<BoxConfigurationItem> items
    ) {
        ProductVariant boxVariant = preset.getBox().getEmptyBoxVariant();
        List<PresetComponentView> components = new ArrayList<>();
        for (BoxConfigurationItem item : items) {
            ProductVariant mooncakeVariant = item.getMooncakeVariant();
            PresetComponentView component = new PresetComponentView(
                mooncakeVariant.getId(),
                mooncakeVariant.getSku(),
                buildComponentDisplayName(mooncakeVariant),
                item.getQuantityPerBox()
            );
            components.add(component);
        }

        return new PresetRecipeView(
            preset.getId(),
            preset.getPresetVariant().getId(),
            boxVariant.getId(),
            boxVariant.getName(),
            preset.getBox().getCapacityCakes(),
            components
        );
    }

    private String buildComponentDisplayName(ProductVariant variant) {
        String productName = variant.getProduct().getName();
        String variantName = variant.getName();

        if (variantName == null || variantName.isBlank()) {
            return productName;
        }
        return productName + " (" + variantName + ")";
    }

    private String formatVnd(BigDecimal amount) {
        return NumberFormat.getNumberInstance(VIETNAMESE).format(amount) + "đ";
    }

    private Map<Long, List<ProductVariant>> groupVariantsByProduct(
        List<ProductVariant> variants
    ) {
        Map<Long, List<ProductVariant>> variantsByProductId = new LinkedHashMap<>();
        for (ProductVariant variant : variants) {
            long productId = variant.getProduct().getId();
            List<ProductVariant> productVariants = variantsByProductId.get(productId);
            if (productVariants == null) {
                productVariants = new ArrayList<>();
                variantsByProductId.put(productId, productVariants);
            }
            productVariants.add(variant);
        }
        return variantsByProductId;
    }
}
