SET XACT_ABORT ON;

DECLARE @gallery TABLE (
    sku NVARCHAR(80) NOT NULL,
    image_url NVARCHAR(1000) NOT NULL,
    view_label NVARCHAR(50) NOT NULL,
    sort_order INT NOT NULL
);

INSERT INTO @gallery (sku, image_url, view_label, sort_order) VALUES
    (N'GIFT-KIM-PHUC-4', N'/images/products/mooncake-box-gallery-1200-webp/kim-phuc/01-hop-dong.webp', N'hộp đóng', 1),
    (N'GIFT-KIM-PHUC-4', N'/images/products/mooncake-box-gallery-1200-webp/kim-phuc/02-hop-mo-4-banh.webp', N'mở hộp', 2),
    (N'GIFT-KIM-PHUC-4', N'/images/products/mooncake-box-gallery-1200-webp/kim-phuc/03-chi-tiet-hop.webp', N'chi tiết hộp', 3),
    (N'GIFT-LAM-PHUC-6', N'/images/products/mooncake-box-gallery-1200-webp/lam-phuc/01-hop-dong.webp', N'hộp đóng', 1),
    (N'GIFT-LAM-PHUC-6', N'/images/products/mooncake-box-gallery-1200-webp/lam-phuc/02-hop-mo-6-banh.webp', N'mở hộp', 2),
    (N'GIFT-LAM-PHUC-6', N'/images/products/mooncake-box-gallery-1200-webp/lam-phuc/03-chi-tiet-hop.webp', N'chi tiết hộp', 3);

IF (
    SELECT COUNT(*)
    FROM @gallery AS seed
    JOIN dbo.PRODUCT_VARIANT AS variant ON variant.sku = seed.sku
) <> 6
    THROW 51160, 'MoonCake V16 expected 6 gallery images for 2 existing gift-set SKUs.', 1;

INSERT INTO dbo.PRODUCT_IMAGE
    (variant_id, image_url, alt_text, sort_order, is_primary)
SELECT
    variant.variant_id,
    seed.image_url,
    product.product_name + N' - ' + seed.view_label,
    seed.sort_order,
    0
FROM @gallery AS seed
JOIN dbo.PRODUCT_VARIANT AS variant ON variant.sku = seed.sku
JOIN dbo.PRODUCT AS product ON product.product_id = variant.product_id
WHERE NOT EXISTS (
    SELECT 1
    FROM dbo.PRODUCT_IMAGE AS image
    WHERE image.variant_id = variant.variant_id
      AND image.image_url = seed.image_url
);
