package com.se201411.project.mooncake.product.dto;

import com.se201411.project.mooncake.entity.Box;
import com.se201411.project.mooncake.entity.Category;
import com.se201411.project.mooncake.entity.Product;
import com.se201411.project.mooncake.entity.ProductVariant;
import com.se201411.project.mooncake.enums.ProductType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;

public final class AdminCatalogForms {
    public static final String SLUG_PATTERN = "[a-z0-9]+(?:-[a-z0-9]+)*";
    public static final String SLUG_MESSAGE = "Đường dẫn chỉ dùng chữ thường không dấu, số và dấu gạch ngang.";
    public static final String IMAGE_URL_MESSAGE = "Đường dẫn ảnh phải bắt đầu bằng https://, /images/ hoặc /uploads/.";
    public static final String PRICE_MIN_MESSAGE = "Giá bán thấp nhất là 1.000đ.";
    public static final String PRICE_MAX_MESSAGE = "Giá bán cao nhất là 100.000.000đ.";
    public static final String PRICE_DIGITS_MESSAGE = "Giá bán phải là số nguyên.";

    private AdminCatalogForms() {
    }

    public record CategoryForm(
        Long id,
        @NotBlank(message = "Vui lòng nhập tên danh mục.")
        @Size(max = 160, message = "Tên danh mục tối đa 160 ký tự.")
        String name,
        @NotBlank(message = "Vui lòng nhập đường dẫn.")
        @Size(max = 160, message = "Đường dẫn tối đa 160 ký tự.")
        @Pattern(regexp = SLUG_PATTERN, message = SLUG_MESSAGE)
        String slug,
        boolean active
    ) {

        /** Entity -> DTO: fills the edit form from a saved category. */
        public static CategoryForm entityToDto(Category category) {
            return new CategoryForm(
                category.getId(),
                category.getName(),
                category.getSlug(),
                category.isActive()
            );
        }

        /** DTO -> Entity: copies only the plain fields; the service loads the entity and checks rules. */
        public void dtoToEntity(Category category) {
            category.setName(name.trim());
            category.setSlug(slug);
            category.setActive(active);
        }
    }

    public record ProductForm(
        Long id,
        @Positive(message = "Vui lòng chọn danh mục.")
        long categoryId,
        @NotNull(message = "Vui lòng chọn loại sản phẩm.")
        ProductType productType,
        @NotBlank(message = "Vui lòng nhập tên sản phẩm.")
        @Size(max = 220, message = "Tên sản phẩm tối đa 220 ký tự.")
        String name,
        @NotBlank(message = "Vui lòng nhập đường dẫn.")
        @Size(max = 180, message = "Đường dẫn tối đa 180 ký tự.")
        @Pattern(regexp = SLUG_PATTERN, message = SLUG_MESSAGE)
        String slug,
        @NotBlank(message = "Vui lòng nhập mô tả ngắn.")
        @Size(max = 600, message = "Mô tả ngắn tối đa 600 ký tự.")
        String shortDescription,
        @Size(max = 4000, message = "Mô tả chi tiết tối đa 4.000 ký tự.")
        String detailedDescription,
        @Size(max = 700, message = "Thành phần chính tối đa 700 ký tự.")
        String mainIngredients,
        @Size(max = 350, message = "Hạn sử dụng tối đa 350 ký tự.")
        String shelfLifeInfo,
        @Size(max = 600, message = "Cách bảo quản tối đa 600 ký tự.")
        String storageGuidance,
        boolean active
    ) {

        /** Entity -> DTO: fills the edit form from a saved product. */
        public static ProductForm entityToDto(Product product) {
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

        /**
         * DTO -> Entity: copies only the plain fields. The service sets the category itself,
         * because it must load it from the database instead of trusting the submitted ID.
         */
        public void dtoToEntity(Product product) {
            product.setProductType(productType);
            product.setName(name.trim());
            product.setSlug(slug);
            product.setShortDescription(shortDescription);
            product.setDetailedDescription(detailedDescription);
            product.setMainIngredients(mainIngredients);
            product.setShelfLifeInfo(shelfLifeInfo);
            product.setStorageGuidance(storageGuidance);
            product.setActive(active);
        }
    }

    public record NewProductForm(
        @Positive(message = "Vui lòng chọn danh mục.")
        long categoryId,
        @NotNull(message = "Vui lòng chọn loại sản phẩm.")
        ProductType productType,
        @NotBlank(message = "Vui lòng nhập tên sản phẩm.")
        @Size(max = 220, message = "Tên sản phẩm tối đa 220 ký tự.")
        String name,
        @NotBlank(message = "Vui lòng nhập đường dẫn.")
        @Size(max = 180, message = "Đường dẫn tối đa 180 ký tự.")
        @Pattern(regexp = SLUG_PATTERN, message = SLUG_MESSAGE)
        String slug,
        @NotBlank(message = "Vui lòng nhập mô tả ngắn.")
        @Size(max = 600, message = "Mô tả ngắn tối đa 600 ký tự.")
        String shortDescription,
        @Size(max = 4000, message = "Mô tả chi tiết tối đa 4.000 ký tự.")
        String detailedDescription,
        @Size(max = 700, message = "Thành phần chính tối đa 700 ký tự.")
        String mainIngredients,
        @Size(max = 350, message = "Hạn sử dụng tối đa 350 ký tự.")
        String shelfLifeInfo,
        @Size(max = 600, message = "Cách bảo quản tối đa 600 ký tự.")
        String storageGuidance,
        boolean active,
        @NotBlank(message = "Vui lòng nhập mã SKU.")
        @Size(max = 80, message = "Mã SKU tối đa 80 ký tự.")
        String sku,
        @NotBlank(message = "Vui lòng nhập tên phiên bản.")
        @Size(max = 200, message = "Tên phiên bản tối đa 200 ký tự.")
        String variantName,
        @NotNull(message = "Vui lòng nhập giá bán.")
        @DecimalMin(value = "1000", message = PRICE_MIN_MESSAGE)
        @DecimalMax(value = "100000000", message = PRICE_MAX_MESSAGE)
        @Digits(integer = 9, fraction = 0, message = PRICE_DIGITS_MESSAGE)
        BigDecimal unitPrice,
        @Min(value = 1, message = "Khối lượng phải lớn hơn 0.")
        @Max(value = 5000, message = "Khối lượng tối đa 5.000g.")
        Integer weightGrams,
        @Size(max = 180, message = "Nhân bánh tối đa 180 ký tự.")
        String fillingLabel,
        @Size(max = 1000, message = "Đường dẫn ảnh tối đa 1.000 ký tự.")
        @Pattern(regexp = "(?:https?://[^\\s]+|/images/[^\\s]+|/uploads/[^\\s]+)?", message = IMAGE_URL_MESSAGE)
        String imageUrl,
        MultipartFile imageFile
    ) {
    }

    public record ImageUploadForm(
        @Positive(message = "Phiên bản không hợp lệ.")
        long variantId,
        MultipartFile imageFile,
        @Size(max = 250, message = "Mô tả ảnh tối đa 250 ký tự.")
        String altText,
        @Min(value = 0, message = "Thứ tự ảnh không hợp lệ.")
        int sortOrder,
        boolean primary
    ) {
    }

    public record VariantForm(
        Long id,
        @Positive(message = "Sản phẩm không hợp lệ.")
        long productId,
        @NotBlank(message = "Vui lòng nhập mã SKU.")
        @Size(max = 80, message = "Mã SKU tối đa 80 ký tự.")
        String sku,
        @NotBlank(message = "Vui lòng nhập tên phiên bản.")
        @Size(max = 200, message = "Tên phiên bản tối đa 200 ký tự.")
        String name,
        @Min(value = 1, message = "Khối lượng phải lớn hơn 0.")
        @Max(value = 5000, message = "Khối lượng tối đa 5.000g.")
        Integer weightGrams,
        @Size(max = 180, message = "Nhân bánh tối đa 180 ký tự.")
        String fillingLabel,
        @NotNull(message = "Vui lòng nhập giá bán.")
        @DecimalMin(value = "1000", message = PRICE_MIN_MESSAGE)
        @DecimalMax(value = "100000000", message = PRICE_MAX_MESSAGE)
        @Digits(integer = 9, fraction = 0, message = PRICE_DIGITS_MESSAGE)
        BigDecimal unitPrice,
        boolean active
    ) {

        /** Entity -> DTO: fills the edit form from a saved SKU. */
        public static VariantForm entityToDto(ProductVariant variant) {
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

        /**
         * DTO -> Entity: copies only the plain fields. SKU, parent product and the
         * on-sale flag stay in the service because they follow extra business rules.
         */
        public void dtoToEntity(ProductVariant variant) {
            variant.setName(name.trim());
            variant.setWeightGrams(weightGrams);
            variant.setFillingLabel(fillingLabel);
            variant.setUnitPrice(unitPrice);
        }
    }

    public record BoxForm(
        Long id,
        @Positive(message = "Vui lòng chọn phiên bản hộp.")
        long variantId,
        @Min(value = 1, message = "Sức chứa phải từ 1 bánh.")
        @Max(value = 24, message = "Sức chứa tối đa 24 bánh.")
        int capacityCakes,
        @Size(max = 180, message = "Chất liệu tối đa 180 ký tự.")
        String material,
        @Size(max = 500, message = "Ghi chú tối đa 500 ký tự.")
        String notes,
        boolean active
    ) {

        /** Entity -> DTO: fills the edit form from a saved box model. */
        public static BoxForm entityToDto(Box box) {
            return new BoxForm(
                box.getId(),
                box.getEmptyBoxVariant().getId(),
                box.getCapacityCakes(),
                box.getMaterial(),
                box.getNotes(),
                box.isActive()
            );
        }

        /** DTO -> Entity: copies only the plain fields; the service sets the box SKU itself. */
        public void dtoToEntity(Box box) {
            box.setCapacityCakes(capacityCakes);
            box.setMaterial(material);
            box.setNotes(notes);
            box.setActive(active);
        }
    }

    public record ImageForm(
        Long id,
        @Positive(message = "Phiên bản không hợp lệ.")
        long variantId,
        @NotBlank(message = "Vui lòng nhập đường dẫn ảnh.")
        @Size(max = 1000, message = "Đường dẫn ảnh tối đa 1.000 ký tự.")
        @Pattern(regexp = "(?:https?://[^\\s]+|/images/[^\\s]+|/uploads/[^\\s]+)", message = IMAGE_URL_MESSAGE)
        String imageUrl,
        @Size(max = 250, message = "Mô tả ảnh tối đa 250 ký tự.")
        String altText,
        @Min(value = 0, message = "Thứ tự ảnh không hợp lệ.")
        int sortOrder,
        boolean primary
    ) {
    }
}
