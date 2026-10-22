package com.se201411.project.mooncake.product.service;

import com.se201411.project.mooncake.product.dto.AdminCatalogForms.*;
import com.se201411.project.mooncake.product.dto.AdminCatalogView;

public interface ProductAdminService {
    AdminCatalogView catalog();

    void saveCategory(CategoryForm form);

    void saveProduct(ProductForm form);

    void saveVariant(VariantForm form);

    void saveBox(BoxForm form);

    void saveImage(ImageForm form);

    void removeImage(long imageId);
}
