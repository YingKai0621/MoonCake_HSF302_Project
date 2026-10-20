package com.se201411.project.mooncake.product.dto;

import com.se201411.project.mooncake.enums.ProductType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * Immutable database-backed view model shared by the catalog and product-detail pages.
 */
public final class CatalogProductView {

    private final long id;
    private final String slug;
    private final ProductType type;
    private final String name;
    private final String description;
    private final String detailedDescription;
    private final String mainIngredients;
    private final String shelfLifeInformation;
    private final String storageInstructions;
    private final List<CatalogVariantView> variants;

    public CatalogProductView(
        long id,
        String slug,
        ProductType type,
        String name,
        String description,
        String detailedDescription,
        String mainIngredients,
        String shelfLifeInformation,
        String storageInstructions,
        List<CatalogVariantView> variants
    ) {
        this.id = id;
        this.slug = Objects.requireNonNull(slug, "slug must not be null");
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.description = Objects.requireNonNull(description, "description must not be null");
        this.detailedDescription = detailedDescription;
        this.mainIngredients = mainIngredients;
        this.shelfLifeInformation = shelfLifeInformation;
        this.storageInstructions = storageInstructions;
        this.variants = List.copyOf(variants);
        if (this.variants.isEmpty()) {
            throw new IllegalArgumentException("A catalog product must have at least one sellable variant");
        }
    }

    public long getId() {
        return id;
    }

    public String getSlug() {
        return slug;
    }

    public ProductType getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getDetailedDescription() {
        return detailedDescription;
    }

    public String getMainIngredients() {
        return mainIngredients;
    }

    public String getShelfLifeInformation() {
        return shelfLifeInformation;
    }

    public String getStorageInstructions() {
        return storageInstructions;
    }

    public List<CatalogVariantView> getVariants() {
        return variants;
    }

    public CatalogVariantView getPrimaryVariant() {
        return variants.getFirst();
    }

    public BigDecimal getPrice() {
        return getPrimaryVariant().unitPrice();
    }

    public String getFormattedPrice() {
        return getPrimaryVariant().formattedPrice();
    }

    public String getImageUrl() {
        return getPrimaryVariant().imageUrl();
    }

    public String getImageAlt() {
        return getPrimaryVariant().imageAlt();
    }

    public Integer getBoxCount() {
        PresetRecipeView recipe = getPrimaryVariant().presetRecipe();
        if (recipe == null) {
            return null;
        }
        return recipe.capacityCakes();
    }

    public Integer getWeight() {
        return getPrimaryVariant().weightGrams();
    }

    public String getFilling() {
        return getPrimaryVariant().fillingLabel();
    }

    public String getFillingDisplayName() {
        return getFilling();
    }

    public List<Integer> getBoxCounts() {
        Set<Integer> boxCounts = new TreeSet<>();
        for (CatalogVariantView variant : variants) {
            PresetRecipeView recipe = variant.presetRecipe();
            if (recipe != null) {
                boxCounts.add(recipe.capacityCakes());
            }
        }
        return List.copyOf(boxCounts);
    }

    public List<Integer> getWeights() {
        Set<Integer> weights = new TreeSet<>();
        for (CatalogVariantView variant : variants) {
            Integer weight = variant.weightGrams();
            if (weight != null) {
                weights.add(weight);
            }
        }
        return List.copyOf(weights);
    }

    public List<String> getFillings() {
        Set<String> fillings = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (CatalogVariantView variant : variants) {
            String filling = variant.fillingLabel();
            if (filling != null && !filling.isBlank()) {
                fillings.add(filling);
            }
        }
        return List.copyOf(fillings);
    }

    public String getBoxCountFilterValues() {
        return joinIntegerValues(getBoxCounts());
    }

    public String getWeightFilterValues() {
        return joinIntegerValues(getWeights());
    }

    public String getFillingFilterValues() {
        return String.join("|", getFillings());
    }

    public String getSearchText() {
        List<String> searchParts = new ArrayList<>();
        searchParts.add(name);
        searchParts.add(description);

        for (CatalogVariantView variant : variants) {
            searchParts.add(variant.sku());
            searchParts.add(variant.name());

            String filling = variant.fillingLabel();
            if (filling != null && !filling.isBlank()) {
                searchParts.add(filling);
            }
        }

        return String.join(" ", searchParts);
    }

    public boolean isBoxCountAvailable() {
        return !getBoxCounts().isEmpty();
    }

    public boolean isWeightAvailable() {
        return !getWeights().isEmpty();
    }

    public boolean isFillingAvailable() {
        return !getFillings().isEmpty();
    }

    public String getDetailUrl() {
        return "/products/" + slug;
    }

    public String getCategory() {
        if (type == ProductType.MOONCAKE) {
            return "mooncakes";
        }
        if (type == ProductType.GIFT_SET) {
            return "gift-boxes";
        }
        return "empty-boxes";
    }

    public String getProductType() {
        if (type == ProductType.MOONCAKE) {
            return "Bánh Trung Thu";
        }
        if (type == ProductType.GIFT_SET) {
            return "Hộp quà Trung Thu";
        }
        return "Hộp rỗng";
    }

    public String getPackageSpecification() {
        Integer boxCount = getBoxCount();
        if (boxCount == null) {
            return null;
        }
        return "Hộp " + boxCount + " bánh";
    }

    public boolean isMultipleVariants() {
        return variants.size() > 1;
    }

    private String joinIntegerValues(List<Integer> values) {
        List<String> textValues = new ArrayList<>();
        for (Integer value : values) {
            textValues.add(String.valueOf(value));
        }
        return String.join("|", textValues);
    }
}
