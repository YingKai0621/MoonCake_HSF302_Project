-- Replace the generic gift-set photos used by the two EMPTY_BOX variants
-- with images that show the actual four-slot and six-slot box interiors.

DECLARE @box_images TABLE (
    sku NVARCHAR(80) NOT NULL PRIMARY KEY,
    image_url NVARCHAR(1000) NOT NULL,
    alt_text NVARCHAR(250) NOT NULL
);

INSERT INTO @box_images (sku, image_url, alt_text)
VALUES
    (N'BOX-KIM-PHUC-4', N'/images/boxes/box-kim-phuc-4.png',
     N'Vỏ hộp Kim Phúc màu đen với 4 ngăn bánh'),
    (N'BOX-LAM-PHUC-6', N'/images/boxes/box-lam-phuc-6.png',
     N'Vỏ hộp Lam Phúc màu đỏ với 6 ngăn bánh');

UPDATE image
SET image.image_url = seed.image_url,
    image.alt_text = seed.alt_text,
    image.sort_order = 0
FROM dbo.PRODUCT_IMAGE AS image
JOIN dbo.PRODUCT_VARIANT AS variant ON variant.variant_id = image.variant_id
JOIN @box_images AS seed ON seed.sku = variant.sku
WHERE image.is_primary = 1;

IF (
    SELECT COUNT(*)
    FROM @box_images AS seed
    JOIN dbo.PRODUCT_VARIANT AS variant ON variant.sku = seed.sku
    JOIN dbo.PRODUCT_IMAGE AS image
        ON image.variant_id = variant.variant_id
       AND image.is_primary = 1
       AND image.image_url = seed.image_url
) <> 2
    THROW 51005, 'MoonCake V7 did not configure both custom-box images.', 1;
