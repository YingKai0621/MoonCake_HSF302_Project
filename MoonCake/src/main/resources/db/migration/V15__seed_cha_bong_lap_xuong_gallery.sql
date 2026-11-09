SET XACT_ABORT ON;

DECLARE @gallery TABLE (
    sku NVARCHAR(80) NOT NULL,
    image_url NVARCHAR(1000) NOT NULL,
    view_label NVARCHAR(50) NOT NULL,
    sort_order INT NOT NULL
);

INSERT INTO @gallery (sku, image_url, view_label, sort_order) VALUES
    (N'MC-CBTN-080', N'/images/products/cha-bong-trung-nghien/cha-bong-trung-nghien-01.webp', N'ảnh 1', 1),
    (N'MC-CBTN-080', N'/images/products/cha-bong-trung-nghien/cha-bong-trung-nghien-02.webp', N'ảnh 2', 2),
    (N'MC-CBTN-080', N'/images/products/cha-bong-trung-nghien/cha-bong-trung-nghien-03.webp', N'ảnh 3', 3),
    (N'MC-LXGQXO-150', N'/images/products/lap-xuong-ga-quay-xot-xo/lap-xuong-ga-quay-xot-xo-01.webp', N'ảnh 1', 1),
    (N'MC-LXGQXO-150', N'/images/products/lap-xuong-ga-quay-xot-xo/lap-xuong-ga-quay-xot-xo-02.webp', N'ảnh 2', 2),
    (N'MC-LXGQXO-150', N'/images/products/lap-xuong-ga-quay-xot-xo/lap-xuong-ga-quay-xot-xo-03.webp', N'ảnh 3', 3);

IF (
    SELECT COUNT(*)
    FROM @gallery AS seed
    JOIN dbo.PRODUCT_VARIANT AS variant ON variant.sku = seed.sku
) <> 6
    THROW 51150, 'MoonCake V15 expected 6 gallery images for 2 existing mooncake SKUs.', 1;

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
