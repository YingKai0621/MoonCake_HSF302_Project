package com.se201411.project.mooncake.product.service;

import com.se201411.project.mooncake.enums.ProductType;
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

import java.util.List;

public interface ProductAdminService {
    AdminCatalogView catalog();

    List<AdminProductRow> productRows(ProductType type);

    List<AdminProductTab> productTabs(ProductType selected);

    AdminProductDetail productDetail(long productId);

    List<CategoryForm> categories();

    List<ProductTypeOption> productTypes();

    void saveCategory(CategoryForm form);

    long saveProduct(ProductForm form);

    long createProduct(NewProductForm form);

    void saveVariant(VariantForm form);

    long saveBox(BoxForm form);

    long saveImage(ImageForm form);

    long uploadImage(ImageUploadForm form);

    long removeImage(long imageId);
}
