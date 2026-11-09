package com.se201411.project.mooncake.product.service.impl;

import com.se201411.project.mooncake.common.exception.BusinessRuleViolationException;
import com.se201411.project.mooncake.common.validation.AdminFormValidation;
import com.se201411.project.mooncake.entity.Box;
import com.se201411.project.mooncake.entity.Category;
import com.se201411.project.mooncake.entity.Product;
import com.se201411.project.mooncake.entity.ProductImage;
import com.se201411.project.mooncake.entity.ProductVariant;
import com.se201411.project.mooncake.enums.ProductType;
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
import java.util.Map;

@Service
@Transactional
public class ProductAdminServiceImpl implements ProductAdminService {
    private final ProductAdminRepository repo;
    private final InventoryService inventory;
    private final PresetConfigurationQueryService presets;
    private final AdminAccessService access;
    private final AdminFormValidation validation;
    private final ProductImageStorageService imageStorage;

    public ProductAdminServiceImpl(ProductAdminRepository repo, InventoryService inventory,
                                   PresetConfigurationQueryService presets, AdminAccessService access,
                                   AdminFormValidation validation, ProductImageStorageService imageStorage) {
        this.repo = repo;
        this.inventory = inventory;
        this.presets = presets;
        this.access = access;
        this.validation = validation;
        this.imageStorage = imageStorage;
    }

    @Override
    @Transactional(readOnly = true)
    public AdminCatalogView catalog() {
        access.requireAdmin();
        List<ProductForm> products = new ArrayList<>();
        for (Product product : repo.products()) {
            products.add(productForm(product));
        }
        List<VariantForm> variants = new ArrayList<>();
        for (ProductVariant variant : repo.variants()) {
            variants.add(variantForm(variant));
        }
        List<BoxForm> boxes = new ArrayList<>();
        for (Box box : repo.boxes()) {
            boxes.add(boxForm(box));
        }
        List<ImageForm> images = new ArrayList<>();
        for (ProductImage image : repo.images()) {
            images.add(imageForm(image));
        }
        return new AdminCatalogView(categoryForms(), products, variants, boxes, images);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminProductRow> productRows(ProductType type) {
        access.requireAdmin();
        Map<Long, List<ProductVariant>> variantsByProduct = new HashMap<>();
        for (ProductVariant variant : repo.variants()) {
            long productId = variant.getProduct().getId();
            variantsByProduct.computeIfAbsent(productId, key -> new ArrayList<>()).add(variant);
        }
        Map<Long, List<ImageForm>> imagesByProduct = new HashMap<>();
        for (ProductImage image : repo.images()) {
            long productId = image.getVariant().getProduct().getId();
            imagesByProduct.computeIfAbsent(productId, key -> new ArrayList<>()).add(imageForm(image));
        }

        List<AdminProductRow> rows = new ArrayList<>();
        for (Product product : repo.products()) {
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
            rows.add(new AdminProductRow(
                product.getId(),
                product.getName(),
                product.getCategory().getName(),
                typeLabel(product.getProductType()),
                variants.size(),
                minPrice,
                maxPrice,
                coverImage(imagesByProduct.getOrDefault(product.getId(), List.of())),
                product.isActive()
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
            counts.merge(product.getProductType(), 1L, Long::sum);
            total++;
        }
        List<AdminProductTab> tabs = new ArrayList<>();
        tabs.add(new AdminProductTab(null, "Tất cả", total, selected == null));
        for (ProductType type : List.of(ProductType.GIFT_SET, ProductType.MOONCAKE, ProductType.EMPTY_BOX)) {
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
            imagesBySku.computeIfAbsent(variantId, key -> new ArrayList<>()).add(imageForm(image));
        }

        List<AdminProductDetail.Sku> skus = new ArrayList<>();
        for (ProductVariant variant : repo.variantsOf(productId)) {
            List<ImageForm> images = imagesBySku.getOrDefault(variant.getId(), List.of());
            skus.add(new AdminProductDetail.Sku(variantForm(variant), coverImage(images), images));
        }

        List<BoxForm> boxes = new ArrayList<>();
        for (Box box : repo.boxesOf(productId)) {
            boxes.add(boxForm(box));
        }

        return new AdminProductDetail(
            productForm(product),
            typeLabel(product.getProductType()),
            !skus.isEmpty(),
            product.getProductType() == ProductType.EMPTY_BOX,
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
    public List<ProductTypeOption> productTypes() {
        List<ProductTypeOption> options = new ArrayList<>();
        for (ProductType type : List.of(ProductType.GIFT_SET, ProductType.MOONCAKE, ProductType.EMPTY_BOX)) {
            options.add(new ProductTypeOption(type, typeLabel(type)));
        }
        return options;
    }

    @Override
    public void saveCategory(CategoryForm f) {
        access.requireAdmin();
        validation.validate(f);
        Category v = f.id() == null ? new Category() : repo.category(f.id());
        v.setName(f.name().trim());
        v.setSlug(f.slug());
        v.setActive(f.active());
        repo.save(v);
        repo.flush();
    }

    @Override
    public long saveProduct(ProductForm f) {
        access.requireAdmin();
        validation.validate(f);
        Product v = f.id() == null ? new Product() : repo.product(f.id());
        if (f.id() != null && v.getProductType() != f.productType() && repo.hasVariants(f.id())) {
            throw new BusinessRuleViolationException("Sản phẩm đã có SKU nên không đổi được loại.");
        }
        v.setCategory(repo.category(f.categoryId()));
        v.setProductType(f.productType());
        v.setName(f.name().trim());
        v.setSlug(f.slug());
        v.setShortDescription(f.shortDescription());
        v.setDetailedDescription(f.detailedDescription());
        v.setMainIngredients(f.mainIngredients());
        v.setShelfLifeInfo(f.shelfLifeInfo());
        v.setStorageGuidance(f.storageGuidance());
        v.setActive(f.active());
        repo.save(v);
        repo.flush();
        return v.getId();
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
            null,
            null,
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
            throw new BusinessRuleViolationException("Không đổi được mã SKU hoặc sản phẩm của SKU đã tạo; hãy tạo SKU mới.");
        }
        v.setProduct(repo.product(f.productId()));
        v.setSku(f.sku());
        v.setName(f.name().trim());
        v.setWeightGrams(f.weightGrams());
        v.setFillingLabel(f.fillingLabel());
        v.setUnitPrice(f.unitPrice());
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
                throw new BusinessRuleViolationException("Mẫu hộp đã được dùng nên không đổi được SKU hoặc sức chứa; hãy tạo mẫu mới.");
            }
        }
        ProductVariant variant = repo.variant(f.variantId());
        if (variant.getProduct().getProductType() != ProductType.EMPTY_BOX) {
            throw new BusinessRuleViolationException("Mẫu hộp phải gắn với SKU của sản phẩm loại hộp rỗng.");
        }
        v.setEmptyBoxVariant(variant);
        v.setCapacityCakes(f.capacityCakes());
        v.setMaterial(f.material());
        v.setNotes(f.notes());
        v.setActive(f.active());
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
        v.setImageUrl(f.imageUrl());
        v.setAltText(f.altText());
        v.setSortOrder(f.sortOrder());
        v.setPrimary(f.primary());
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

    private List<CategoryForm> categoryForms() {
        List<CategoryForm> categories = new ArrayList<>();
        for (Category category : repo.categories()) {
            categories.add(new CategoryForm(category.getId(), category.getName(), category.getSlug(), category.isActive()));
        }
        return categories;
    }

    private ProductForm productForm(Product product) {
        return new ProductForm(
            product.getId(),
            product.getCategory().getId(),
            product.getProductType(),
            product.getName(),
            product.getSlug(),
            product.getShortDescription(),
            product.getDetailedDescription(),
            product.getMainIngredients(),
            product.getShelfLifeInfo(),
            product.getStorageGuidance(),
            product.isActive()
        );
    }

    private VariantForm variantForm(ProductVariant variant) {
        return new VariantForm(
            variant.getId(),
            variant.getProduct().getId(),
            variant.getSku(),
            variant.getName(),
            variant.getWeightGrams(),
            variant.getFillingLabel(),
            variant.getUnitPrice(),
            variant.isActive()
        );
    }

    private BoxForm boxForm(Box box) {
        return new BoxForm(
            box.getId(),
            box.getEmptyBoxVariant().getId(),
            box.getCapacityCakes(),
            box.getMaterial(),
            box.getNotes(),
            box.isActive()
        );
    }

    private ImageForm imageForm(ProductImage image) {
        return new ImageForm(
            image.getId(),
            image.getVariant().getId(),
            image.getImageUrl(),
            image.getAltText(),
            image.getSortOrder(),
            image.isPrimary()
        );
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
