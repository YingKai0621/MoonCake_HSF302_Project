-- Enable the two V3 empty-box SKUs for the MoonCake CUSTOM gift-box flow.
-- Mor'dan publishes complete combo prices, not standalone empty-box prices.
-- These are approved demo prices for this academic MoonCake project.

SET XACT_ABORT ON;

UPDATE dbo.PRODUCT
SET short_description = CASE slug
        WHEN N'vo-hop-kim-phuc-4-banh'
            THEN N'Vỏ hộp quà 4 bánh dùng để mua riêng hoặc tạo hộp bánh tự chọn.'
        WHEN N'vo-hop-lam-phuc-6-banh'
            THEN N'Vỏ hộp quà 6 bánh dùng để mua riêng hoặc tạo hộp bánh tự chọn.'
    END,
    detailed_description = CASE slug
        WHEN N'vo-hop-kim-phuc-4-banh'
            THEN N'Mẫu hộp sức chứa 4 bánh, hỗ trợ cấu hình hộp quà CUSTOM.'
        WHEN N'vo-hop-lam-phuc-6-banh'
            THEN N'Mẫu hộp sức chứa 6 bánh, hỗ trợ cấu hình hộp quà CUSTOM.'
    END,
    is_active = 1
WHERE slug IN (N'vo-hop-kim-phuc-4-banh', N'vo-hop-lam-phuc-6-banh');

IF @@ROWCOUNT <> 2
    THROW 51003, 'MoonCake V4 expected exactly two V3 empty-box products.', 1;

UPDATE dbo.PRODUCT_VARIANT
SET unit_price = CASE sku
        WHEN N'BOX-KIM-PHUC-4' THEN CAST(495000 AS DECIMAL(18,0))
        WHEN N'BOX-LAM-PHUC-6' THEN CAST(590000 AS DECIMAL(18,0))
    END,
    is_active = 1
WHERE sku IN (N'BOX-KIM-PHUC-4', N'BOX-LAM-PHUC-6');

IF @@ROWCOUNT <> 2
    THROW 51004, 'MoonCake V4 expected exactly two V3 empty-box SKUs.', 1;

UPDATE box
SET box.box_notes = CASE variant.sku
        WHEN N'BOX-KIM-PHUC-4'
            THEN N'Mẫu hộp 4 bánh dùng cho PRESET và hộp quà CUSTOM.'
        WHEN N'BOX-LAM-PHUC-6'
            THEN N'Mẫu hộp 6 bánh dùng cho PRESET và hộp quà CUSTOM.'
    END,
    box.is_active = 1
FROM dbo.BOX AS box
JOIN dbo.PRODUCT_VARIANT AS variant
    ON variant.variant_id = box.empty_box_variant_id
WHERE variant.sku IN (N'BOX-KIM-PHUC-4', N'BOX-LAM-PHUC-6');

IF @@ROWCOUNT <> 2
    THROW 51005, 'MoonCake V4 expected exactly two V3 BOX records.', 1;

IF (
    SELECT COUNT(*)
    FROM dbo.INVENTORY_BALANCE AS balance
    JOIN dbo.PRODUCT_VARIANT AS variant ON variant.variant_id = balance.variant_id
    WHERE variant.sku IN (N'BOX-KIM-PHUC-4', N'BOX-LAM-PHUC-6')
) <> 2
    THROW 51006, 'MoonCake V4 cannot enable an empty-box SKU without inventory balance.', 1;
