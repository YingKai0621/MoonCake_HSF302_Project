package com.se201411.project.mooncake.product.service.impl;

import com.se201411.project.mooncake.common.exception.BusinessRuleViolationException;
import com.se201411.project.mooncake.common.validation.AdminFormValidation;
import com.se201411.project.mooncake.entity.Box;
import com.se201411.project.mooncake.entity.Category;
import com.se201411.project.mooncake.entity.Product;
import com.se201411.project.mooncake.entity.ProductImage;
import com.se201411.project.mooncake.entity.ProductVariant;
import com.se201411.project.mooncake.enums.ConfigurationStatus;
import com.se201411.project.mooncake.enums.ConfigurationType;
import com.se201411.project.mooncake.enums.ProductType;
import com.se201411.project.mooncake.entity.BoxConfiguration;
import com.se201411.project.mooncake.inventory.dto.AdminInventoryView.Balance;
import com.se201411.project.mooncake.inventory.service.InventoryAdminService;
import com.se201411.project.mooncake.inventory.service.InventoryService;
import com.se201411.project.mooncake.product.dto.AdminCatalogForms.BoxForm;
import com.se201411.project.mooncake.product.dto.AdminCatalogForms.CategoryForm;
import com.se201411.project.mooncake.product.dto.AdminCatalogForms.ImageForm;
import com.se201411.project.mooncake.product.dto.AdminCatalogForms.ImageUploadForm;
import com.se201411.project.mooncake.product.dto.AdminCatalogForms.NewProductForm;
import com.se201411.project.mooncake.product.dto.AdminCatalogForms.ProductForm;
import com.se201411.project.mooncake.product.dto.AdminCatalogForms.VariantForm;
import com.se201411.project.mooncake.product.dto.AdminCatalogView;
import com.se201411.project.mooncake.product.dto.AdminProductDetail;
import com.se201411.project.mooncake.product.dto.AdminProductRow;
import com.se201411.project.mooncake.product.dto.AdminProductTab;
import com.se201411.project.mooncake.product.dto.ProductTypeOption;
import com.se201411.project.mooncake.product.repository.PresetConfigurationRepository;
import com.se201411.project.mooncake.product.repository.ProductAdminRepository;
import com.se201411.project.mooncake.product.service.PresetConfigurationQueryService;
import com.se201411.project.mooncake.product.service.ProductImageStorageService;
import com.se201411.project.mooncake.product.service.ProductAdminService;
import com.se201411.project.mooncake.security.service.AdminAccessService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Service
@Transactional
public class ProductAdminServiceImpl implements ProductAdminService {
    private final ProductAdminRepository repo;
    private final InventoryService inventory;
    private final PresetConfigurationQueryService presets;
    private final AdminAccessService access;
    private final AdminFormValidation validation;
    private final ProductImageStorageService imageStorage;
    private final InventoryAdminService inventoryAdmin;
    private final PresetConfigurationRepository presetConfigurations;

    public ProductAdminServiceImpl(ProductAdminRepository repo, InventoryService inventory,
                                   PresetConfigurationQueryService presets, AdminAccessService access,
                                   AdminFormValidation validation, ProductImageStorageService imageStorage,
                                   InventoryAdminService inventoryAdmin,
                                   PresetConfigurationRepository presetConfigurations) {
        this.repo = repo;
        this.inventory = inventory;
        this.presets = presets;
        this.access = access;
        this.validation = validation;
        this.imageStorage = imageStorage;
        this.inventoryAdmin = inventoryAdmin;
        this.presetConfigurations = presetConfigurations;
    }

    @Override
    @Transactional(readOnly = true)
    public AdminCatalogView catalog() {
        access.requireAdmin();
        List<ProductForm> products = new ArrayList<>();
        for (Product product : repo.products()) {
            products.add(ProductForm.entityToDto(product));
        }
        List<VariantForm> variants = new ArrayList<>();
        for (ProductVariant variant : repo.variants()) {
            variants.add(VariantForm.entityToDto(variant));
        }
        List<BoxForm> boxes = new ArrayList<>();
        for (Box box : repo.boxes()) {
            boxes.add(BoxForm.entityToDto(box));
        }
        List<ImageForm> images = new ArrayList<>();
        for (ProductImage image : repo.images()) {
            images.add(ImageForm.entityToDto(image));
        }
        return new AdminCatalogView(categoryForms(), products, variants, boxes, images);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminProductRow> productRows(ProductType type) {
        access.requireAdmin();
        List<ProductVariant> allVariants = repo.variants();
        Map<Long, Integer> stock = stockByVariant();
        Set<Long> readyGiftSets = readyGiftSets(allVariants);
        Map<Long, List<ProductVariant>> variantsByProduct = new HashMap<>();
        for (ProductVariant variant : allVariants) {
            long productId = variant.getProduct().getId();
            variantsByProduct.computeIfAbsent(productId, key -> new ArrayList<>()).add(variant);
        }
        Map<Long, List<ImageForm>> imagesByProduct = new HashMap<>();
        for (ProductImage image : repo.images()) {
            long productId = image.getVariant().getProduct().getId();
            imagesByProduct.computeIfAbsent(productId, key -> new ArrayList<>()).add(ImageForm.entityToDto(image));
        }

        List<AdminProductRow> rows = new ArrayList<>();
        for (Product product : repo.products()) {
            if (type == null && product.getProductType() == ProductType.GIFT_SET) {
                continue;
            }
            if (type != null && product.getProductType() != type) {
                continue;
            }
            List<ProductVariant> variants = variantsByProduct.getOrDefault(product.getId(), List.of());
            BigDecimal minPrice = null;
            BigDecimal maxPrice = null;
            for (ProductVariant variant : variants) {
                BigDecimal price = variant.getUnitPrice();
                if (minPrice == null || price.compareTo(minPrice) < 0) {
                    minPrice = price;
                }
                if (maxPrice == null || price.compareTo(maxPrice) > 0) {
                    maxPrice = price;
                }
            }
            WebStatus status = productStatus(product, variants, readyGiftSets, stock);
            String imageUrl = coverImage(imagesByProduct.getOrDefault(product.getId(), List.of()));
            rows.add(AdminProductRow.entityToDto(
                product,
                typeLabel(product.getProductType()),
                variants.size(),
                minPrice,
                maxPrice,
                imageUrl,
                status.label(),
                status.tone()
            ));
        }
        return rows;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminProductTab> productTabs(ProductType selected) {
        access.requireAdmin();
        Map<ProductType, Long> counts = new HashMap<>();
        long total = 0;
        for (Product product : repo.products()) {
            if (product.getProductType() == ProductType.GIFT_SET) {
                continue;
            }
            counts.merge(product.getProductType(), 1L, Long::sum);
            total++;
        }
        List<AdminProductTab> tabs = new ArrayList<>();
        tabs.add(new AdminProductTab(null, "Tất cả", total, selected == null));
        for (ProductType type : List.of(ProductType.MOONCAKE, ProductType.EMPTY_BOX)) {
            tabs.add(new AdminProductTab(type, typeLabel(type), counts.getOrDefault(type, 0L), type == selected));
        }
        return tabs;
    }

    @Override
    @Transactional(readOnly = true)
    public AdminProductDetail productDetail(long productId) {
        access.requireAdmin();
        Product product = repo.productForView(productId);

        Map<Long, List<ImageForm>> imagesBySku = new HashMap<>();
        for (ProductImage image : repo.imagesOf(productId)) {
            long variantId = image.getVariant().getId();
            imagesBySku.computeIfAbsent(variantId, key -> new ArrayList<>()).add(ImageForm.entityToDto(image));
        }

        List<ProductVariant> variants = repo.variantsOf(productId);
        Map<Long, Integer> stock = stockByVariant();
        Set<Long> readyGiftSets = readyGiftSets(variants);
        List<AdminProductDetail.Sku> skus = new ArrayList<>();
        for (ProductVariant variant : variants) {
            List<ImageForm> images = imagesBySku.getOrDefault(variant.getId(), List.of());
            int quantity = stock.getOrDefault(variant.getId(), 0);
            WebStatus status = skuStatus(product, variant, readyGiftSets, quantity);
            skus.add(new AdminProductDetail.Sku(
                VariantForm.entityToDto(variant),
                coverImage(images),
                images,
                quantity,
                status.label(),
                status.tone(),
                variantDeleteBlocker(variant.getId())
            ));
        }
        WebStatus productStatus = productStatus(product, variants, readyGiftSets, stock);

        List<BoxForm> boxes = new ArrayList<>();
        for (Box box : repo.boxesOf(productId)) {
            boxes.add(BoxForm.entityToDto(box));
        }

        return new AdminProductDetail(
            ProductForm.entityToDto(product),
            typeLabel(product.getProductType()),
            !skus.isEmpty(),
            product.getProductType() == ProductType.EMPTY_BOX,
            productStatus.label(),
            productStatus.tone(),
            deleteBlocker(productId),
            skus,
            boxes
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryForm> categories() {
        access.requireAdmin();
        return categoryForms();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryForm> categoriesFor(boolean giftBoxes) {
        access.requireAdmin();
        Set<Long> withGiftBoxes = new HashSet<>();
        Set<Long> withOtherProducts = new HashSet<>();
        for (Product product : repo.products()) {
            long categoryId = product.getCategory().getId();
            if (product.getProductType() == ProductType.GIFT_SET) {
                withGiftBoxes.add(categoryId);
            } else {
                withOtherProducts.add(categoryId);
            }
        }
        List<CategoryForm> result = new ArrayList<>();
        for (CategoryForm category : categoryForms()) {
            boolean empty = !withGiftBoxes.contains(category.id()) && !withOtherProducts.contains(category.id());
            boolean matches = giftBoxes
                ? withGiftBoxes.contains(category.id())
                : withOtherProducts.contains(category.id());
            if (matches || empty) {
                result.add(category);
            }
        }
        return result;
    }

    @Override
    public List<ProductTypeOption> productTypes() {
        List<ProductTypeOption> options = new ArrayList<>();
        for (ProductType type : List.of(ProductType.MOONCAKE, ProductType.EMPTY_BOX)) {
            options.add(new ProductTypeOption(type, typeLabel(type)));
        }
        return options;
    }

    @Override
    public void saveCategory(CategoryForm f) {
        access.requireAdmin();
        validation.validate(f);
        if (repo.categorySlugUsed(f.slug(), f.id())) {
            throw new BusinessRuleViolationException("Đường dẫn này đã được danh mục khác dùng.");
        }
        Category v = f.id() == null ? new Category() : repo.category(f.id());
        f.dtoToEntity(v);
        repo.save(v);
        repo.flush();
    }

    @Override
    public long saveProduct(ProductForm f) {
        access.requireAdmin();
        validation.validate(f);
        Product v = f.id() == null ? new Product() : repo.product(f.id());
        if (repo.productSlugUsed(f.slug(), f.id())) {
            throw new BusinessRuleViolationException("Đường dẫn này đã được sản phẩm khác dùng.");
        }
        if (f.id() != null && v.getProductType() != f.productType() && repo.hasVariants(f.id())) {
            throw new BusinessRuleViolationException("Sản phẩm đã có SKU nên không đổi được loại.");
        }
        v.setCategory(repo.category(f.categoryId()));
        f.dtoToEntity(v);
        repo.save(v);
        repo.flush();
        return v.getId();
    }

    @Override
    public void setProductVisible(long productId, boolean visible) {
        access.requireAdmin();
        ProductForm current = ProductForm.entityToDto(repo.product(productId));
        saveProduct(new ProductForm(
            current.id(),
            current.categoryId(),
            current.productType(),
            current.name(),
            current.slug(),
            current.shortDescription(),
            current.detailedDescription(),
            current.mainIngredients(),
            current.shelfLifeInfo(),
            current.storageGuidance(),
            visible
        ));
    }

    @Override
    public long setVariantOnSale(long variantId, boolean onSale) {
        access.requireAdmin();
        VariantForm current = VariantForm.entityToDto(repo.variant(variantId));
        saveVariant(new VariantForm(
            current.id(),
            current.productId(),
            current.sku(),
            current.name(),
            current.weightGrams(),
            current.fillingLabel(),
            current.unitPrice(),
            onSale
        ));
        return current.productId();
    }

    @Override
    public long duplicateProduct(long productId) {
        access.requireAdmin();
        Product source = repo.product(productId);
        Product copy = new Product();
        copy.setCategory(source.getCategory());
        copy.setProductType(source.getProductType());
        copy.setName(source.getName() + " (bản mới)");
        copy.setSlug(freeSlug(source.getSlug()));
        copy.setShortDescription(source.getShortDescription());
        copy.setDetailedDescription(source.getDetailedDescription());
        copy.setMainIngredients(source.getMainIngredients());
        copy.setShelfLifeInfo(source.getShelfLifeInfo());
        copy.setStorageGuidance(source.getStorageGuidance());
        copy.setActive(false);
        repo.save(copy);
        repo.flush();

        for (ProductVariant variant : repo.variantsOf(productId)) {
            ProductVariant newVariant = new ProductVariant();
            newVariant.setProduct(copy);
            newVariant.setSku(freeSku(variant.getSku()));
            newVariant.setName(variant.getName());
            newVariant.setWeightGrams(variant.getWeightGrams());
            newVariant.setFillingLabel(variant.getFillingLabel());
            newVariant.setUnitPrice(variant.getUnitPrice());
            newVariant.setActive(false);
            repo.save(newVariant);
            inventory.initializeBalance(newVariant.getId());
            for (ProductImage image : repo.imagesOf(productId)) {
                if (!image.getVariant().getId().equals(variant.getId())) {
                    continue;
                }
                ProductImage newImage = new ProductImage();
                newImage.setVariant(newVariant);
                newImage.setImageUrl(image.getImageUrl());
                newImage.setAltText(image.getAltText());
                newImage.setSortOrder(image.getSortOrder());
                newImage.setPrimary(image.isPrimary());
                repo.save(newImage);
            }
        }
        repo.flush();
        return copy.getId();
    }

    private String freeSlug(String base) {
        int number = 2;
        String candidate = base + "-" + number;
        while (repo.productSlugUsed(candidate, null)) {
            number++;
            candidate = base + "-" + number;
        }
        return candidate;
    }

    private String freeSku(String base) {
        int number = 2;
        String candidate = base + "-" + number;
        while (repo.skuExists(candidate)) {
            number++;
            candidate = base + "-" + number;
        }
        return candidate;
    }

    @Override
    public long createProduct(NewProductForm f) {
        access.requireAdmin();
        validation.validate(f);
        ProductForm productForm = new ProductForm(
            null,
            f.categoryId(),
            f.productType(),
            f.name(),
            f.slug(),
            f.shortDescription(),
            f.detailedDescription(),
            f.mainIngredients(),
            f.shelfLifeInfo(),
            f.storageGuidance(),
            f.active()
        );
        long productId = saveProduct(productForm);

        VariantForm variantForm = new VariantForm(
            null,
            productId,
            f.sku().trim(),
            f.variantName(),
            f.weightGrams(),
            f.fillingLabel(),
            f.unitPrice(),
            false
        );
        saveVariant(variantForm);

        String imageUrl = null;
        if (f.imageFile() != null && !f.imageFile().isEmpty()) {
            imageUrl = imageStorage.store(f.imageFile());
        } else if (f.imageUrl() != null && !f.imageUrl().isBlank()) {
            imageUrl = f.imageUrl().trim();
        }
        if (imageUrl != null) {
            long variantId = repo.variantsOf(productId).get(0).getId();
            saveImage(new ImageForm(null, variantId, imageUrl, f.name(), 0, true));
        }
        return productId;
    }

    @Override
    public void saveVariant(VariantForm f) {
        access.requireAdmin();
        validation.validate(f);
        ProductVariant v = f.id() == null ? new ProductVariant() : repo.variant(f.id());
        if (f.id() != null && (!v.getProduct().getId().equals(f.productId()) || !v.getSku().equals(f.sku()))) {
            throw new BusinessRuleViolationException("Không đổi được mã SKU hoặc sản phẩm của SKU đã tạo. Hãy tạo SKU mới.");
        }
        Product product = repo.product(f.productId());
        String sku = f.sku().trim();
        if (f.id() == null) {
            sku = sku.toUpperCase();
            checkNewSku(sku);
        }
        checkVariantDetails(product, f);
        v.setProduct(product);
        v.setSku(sku);
        f.dtoToEntity(v);
        v.setActive(false);
        repo.save(v);
        inventory.initializeBalance(v.getId());
        if (f.active()) {
            inventory.requireBalance(v.getId());
            if (v.getProduct().getProductType() == ProductType.GIFT_SET) {
                presets.requireReadyPreset(v.getId());
            }
        }
        v.setActive(f.active());
        repo.flush();
    }

    @Override
    public long saveBox(BoxForm f) {
        access.requireAdmin();
        validation.validate(f);
        Box v = f.id() == null ? new Box() : repo.box(f.id());
        if (f.id() != null) {
            boolean variantChanged = !v.getEmptyBoxVariant().getId().equals(f.variantId());
            boolean capacityChanged = v.getCapacityCakes() != f.capacityCakes();
            if (variantChanged || (capacityChanged && repo.boxReferenced(f.id()))) {
                throw new BusinessRuleViolationException("Mẫu hộp đã được dùng nên không đổi được SKU hoặc sức chứa. Hãy tạo mẫu mới.");
            }
        }
        ProductVariant variant = repo.variant(f.variantId());
        if (variant.getProduct().getProductType() != ProductType.EMPTY_BOX) {
            throw new BusinessRuleViolationException("Mẫu hộp phải gắn với SKU của sản phẩm loại hộp rỗng.");
        }
        v.setEmptyBoxVariant(variant);
        f.dtoToEntity(v);
        repo.save(v);
        repo.flush();
        return variant.getProduct().getId();
    }

    @Override
    public long removeImage(long imageId) {
        access.requireAdmin();
        ProductImage image = repo.image(imageId);
        long productId = image.getVariant().getProduct().getId();
        repo.removeImage(image);
        return productId;
    }

    @Override
    public long saveImage(ImageForm f) {
        access.requireAdmin();
        validation.validate(f);
        ProductVariant variant = repo.variant(f.variantId());
        ProductImage v = f.id() == null ? new ProductImage() : repo.image(f.id());
        if (f.id() != null && !v.getVariant().getId().equals(f.variantId())) {
            throw new BusinessRuleViolationException("Ảnh phải giữ nguyên SKU.");
        }
        if (f.primary()) {
            repo.clearPrimary(f.variantId());
            if (f.id() != null) {
                v = repo.image(f.id());
            }
        }
        v.setVariant(variant);
        f.dtoToEntity(v);
        repo.save(v);
        repo.flush();
        return variant.getProduct().getId();
    }

    @Override
    public long uploadImage(ImageUploadForm f) {
        access.requireAdmin();
        validation.validate(f);
        repo.variant(f.variantId());
        String imageUrl = imageStorage.store(f.imageFile());
        ImageForm image = new ImageForm(null, f.variantId(), imageUrl, f.altText(), f.sortOrder(), f.primary());
        return saveImage(image);
    }

    @Override
    public String deleteProduct(long productId) {
        access.requireAdmin();
        Product product = repo.product(productId);
        String blocker = deleteBlocker(productId);
        if (blocker != null) {
            throw new BusinessRuleViolationException(blocker);
        }
        String name = product.getName();
        repo.deleteProduct(productId);
        return name;
    }

    @Override
    public long deleteVariant(long variantId) {
        access.requireAdmin();
        ProductVariant variant = repo.variant(variantId);
        long productId = variant.getProduct().getId();
        String blocker = variantDeleteBlocker(variantId);
        if (blocker != null) {
            throw new BusinessRuleViolationException(blocker);
        }
        repo.deleteVariant(variantId);
        return productId;
    }

    private void checkNewSku(String sku) {
        if (!sku.matches("[A-Z0-9]+(?:[-_][A-Z0-9]+)*")) {
            throw new BusinessRuleViolationException("Mã SKU chỉ dùng chữ không dấu, số và dấu gạch ngang, ví dụ: GIFT-4-TRANG.");
        }
        if (repo.skuExists(sku)) {
            throw new BusinessRuleViolationException("Mã SKU " + sku + " đã tồn tại.");
        }
    }

    private void checkVariantDetails(Product product, VariantForm f) {
        BigDecimal thousand = BigDecimal.valueOf(1000);
        if (f.unitPrice().remainder(thousand).signum() != 0) {
            throw new BusinessRuleViolationException("Giá bán phải là bội số của 1.000đ.");
        }
        if (product.getProductType() != ProductType.MOONCAKE) {
            return;
        }
        if (f.weightGrams() == null) {
            throw new BusinessRuleViolationException("Bánh lẻ cần nhập khối lượng.");
        }
        if (f.fillingLabel() == null || f.fillingLabel().isBlank()) {
            throw new BusinessRuleViolationException("Bánh lẻ cần nhập nhân bánh.");
        }
    }

    private String variantDeleteBlocker(long variantId) {
        long orders = repo.variantOrderCount(variantId);
        if (orders > 0) {
            return "Phiên bản đã có trong " + orders + " đơn hàng nên không xóa được. Hãy bấm Ngừng bán phiên bản nếu không muốn bán nữa.";
        }
        if (repo.variantCartCount(variantId) > 0) {
            return "Phiên bản đang nằm trong giỏ hàng của khách nên không xóa được. Hãy bấm Ngừng bán phiên bản nếu không muốn bán nữa.";
        }
        if (repo.variantRecipeCount(variantId) > 0) {
            return "Phiên bản đang được dùng trong công thức hộp quà nên không xóa được.";
        }
        if (repo.variantStockHistoryCount(variantId) > 0 || repo.variantStockOnHand(variantId) > 0) {
            return "Phiên bản đã có lịch sử nhập xuất kho nên không xóa được. Hãy bấm Ngừng bán phiên bản nếu không muốn bán nữa.";
        }
        return null;
    }

    private String deleteBlocker(long productId) {
        long orders = repo.orderCount(productId);
        if (orders > 0) {
            return "Sản phẩm đã có trong " + orders + " đơn hàng nên không xóa được.";
        }
        long cartItems = repo.cartItemCount(productId);
        if (cartItems > 0) {
            return "Sản phẩm đang nằm trong giỏ hàng của " + cartItems + " khách nên không xóa được.";
        }
        long recipes = repo.recipeCount(productId);
        if (recipes > 0) {
            return "Sản phẩm đang được dùng trong công thức hộp quà hoặc hộp khách tự chọn nên không xóa được.";
        }
        if (repo.stockHistoryCount(productId) > 0 || repo.stockOnHand(productId) > 0) {
            return "Sản phẩm đã có lịch sử nhập xuất kho nên không xóa được.";
        }
        return null;
    }

    private Map<Long, Integer> stockByVariant() {
        Map<Long, Integer> stock = new HashMap<>();
        for (Balance balance : inventoryAdmin.balances()) {
            stock.put(balance.variantId(), balance.quantity());
        }
        return stock;
    }

    private Set<Long> readyGiftSets(List<ProductVariant> variants) {
        List<Long> giftSetIds = new ArrayList<>();
        for (ProductVariant variant : variants) {
            if (variant.getProduct().getProductType() == ProductType.GIFT_SET) {
                giftSetIds.add(variant.getId());
            }
        }
        Set<Long> ready = new HashSet<>();
        if (giftSetIds.isEmpty()) {
            return ready;
        }
        List<BoxConfiguration> presetsFound = presetConfigurations.findReadyByGiftSetVariantIds(
            giftSetIds,
            ConfigurationType.PRESET,
            ConfigurationStatus.READY
        );
        for (BoxConfiguration preset : presetsFound) {
            ready.add(preset.getPresetVariant().getId());
        }
        return ready;
    }

    private WebStatus productStatus(Product product, List<ProductVariant> variants,
                                    Set<Long> readyGiftSets, Map<Long, Integer> stock) {
        if (!product.isActive()) {
            return new WebStatus("Đang ẩn", "muted");
        }
        if (!product.getCategory().isActive()) {
            return new WebStatus("Danh mục đang ẩn", "muted");
        }
        if (variants.isEmpty()) {
            return new WebStatus("Chưa có phiên bản", "warning");
        }
        boolean anyOpen = false;
        boolean anyReady = false;
        boolean anyInStock = false;
        for (ProductVariant variant : variants) {
            if (!variant.isActive()) {
                continue;
            }
            anyOpen = true;
            if (needsRecipe(product, variant, readyGiftSets)) {
                continue;
            }
            anyReady = true;
            if (stock.getOrDefault(variant.getId(), 0) > 0) {
                anyInStock = true;
            }
        }
        if (!anyOpen) {
            return new WebStatus("Chưa mở bán", "warning");
        }
        if (!anyReady) {
            return new WebStatus("Chưa có công thức", "warning");
        }
        if (!anyInStock) {
            return new WebStatus("Kho đang hết", "danger");
        }
        return new WebStatus("Đang bán trên web", "success");
    }

    private WebStatus skuStatus(Product product, ProductVariant variant, Set<Long> readyGiftSets, int quantity) {
        if (!variant.isActive()) {
            return new WebStatus("Ngừng bán", "muted");
        }
        if (needsRecipe(product, variant, readyGiftSets)) {
            return new WebStatus("Chưa có công thức", "warning");
        }
        if (!product.isActive()) {
            return new WebStatus("Sản phẩm đang ẩn", "muted");
        }
        if (!product.getCategory().isActive()) {
            return new WebStatus("Danh mục đang ẩn", "muted");
        }
        if (quantity <= 0) {
            return new WebStatus("Kho đang hết", "danger");
        }
        return new WebStatus("Đang bán trên web", "success");
    }

    private boolean needsRecipe(Product product, ProductVariant variant, Set<Long> readyGiftSets) {
        return product.getProductType() == ProductType.GIFT_SET && !readyGiftSets.contains(variant.getId());
    }

    private record WebStatus(String label, String tone) {
    }

    private List<CategoryForm> categoryForms() {
        List<CategoryForm> categories = new ArrayList<>();
        for (Category category : repo.categories()) {
            categories.add(CategoryForm.entityToDto(category));
        }
        return categories;
    }

    private String coverImage(List<ImageForm> images) {
        for (ImageForm image : images) {
            if (image.primary()) {
                return image.imageUrl();
            }
        }
        if (images.isEmpty()) {
            return null;
        }
        return images.get(0).imageUrl();
    }

    private String typeLabel(ProductType type) {
        if (type == ProductType.GIFT_SET) {
            return "Hộp quà";
        }
        if (type == ProductType.MOONCAKE) {
            return "Bánh lẻ";
        }
        return "Hộp rỗng";
    }
}
