-- Add primary images for the catalog seeded by V3/V4.
-- The image files are stored locally under static/images/products so the
-- storefront does not depend on third-party hotlinks at runtime.

DECLARE @catalog_images TABLE (
    sku NVARCHAR(80) NOT NULL PRIMARY KEY,
    image_url NVARCHAR(1000) NOT NULL,
    alt_text NVARCHAR(250) NOT NULL
);

INSERT INTO @catalog_images (sku, image_url, alt_text)
VALUES
    (N'MC-KSPM-080', N'/images/products/mooncake-kim-sa-pho-mai.jpg',
     N'Bánh Kim Sa Phô Mai'),
    (N'MC-PBMHA-080', N'/images/products/mooncake-phuc-bon-tu-mochi-hanh-nhan.jpg',
     N'Bánh Phúc Bồn Tử – Mochi – Hạnh Nhân'),
    (N'MC-CBTN-080', N'/images/products/mooncake-cha-bong-trung-nghien.jpg',
     N'Bánh Chà Bông Trứng Nghiền'),
    (N'MC-TCGQ-080', N'/images/products/mooncake-thap-cam-ga-quay.jpg',
     N'Bánh Thập Cẩm Gà Quay'),
    (N'MC-CBBB-080', N'/images/products/mooncake-cua-bat-buu.jpg',
     N'Bánh Cua Bát Bửu'),
    (N'MC-LSTT-080', N'/images/products/mooncake-luu-sa-than-tre-trung-muoi.jpg',
     N'Bánh Lưu Sa Than Tre Trứng Muối'),
    (N'MC-TXHN-120', N'/images/products/mooncake-tra-xanh-hanh-nhan-trung-muoi.jpg',
     N'Bánh Trà Xanh – Hạnh Nhân Trứng Muối'),
    (N'MC-HSMC-120', N'/images/products/mooncake-hat-sen-mac-ca-trung-muoi.jpg',
     N'Bánh Hạt Sen – Mắc Ca Trứng Muối'),
    (N'MC-SCDHDC-120', N'/images/products/mooncake-so-co-la-dua-hat-de-cuoi.jpg',
     N'Bánh Sô Cô La – Dừa – Hạt Dẻ Cười'),
    (N'MC-LXGQXO-150', N'/images/products/mooncake-lap-xuong-ga-quay-xot-xo.jpg',
     N'Bánh Lạp Xưởng Gà Quay xốt XO Trứng Muối'),
    (N'MC-THRV-150', N'/images/products/mooncake-tom-hum-xot-ruou-vang.jpg',
     N'Bánh Tôm Hùm xốt Rượu Vang Trứng Muối'),
    (N'MC-SDHK-150', N'/images/products/mooncake-so-diep-xot-hong-kong.jpg',
     N'Bánh Sò Điệp xốt Hồng Kông Trứng Muối'),
    (N'BOX-KIM-PHUC-4', N'/images/products/gift-kim-phuc.jpg',
     N'Vỏ hộp Kim Phúc 4 bánh'),
    (N'BOX-LAM-PHUC-6', N'/images/products/gift-lam-phuc.jpg',
     N'Vỏ hộp Lam Phúc 6 bánh'),
    (N'GIFT-KIM-PHUC-4', N'/images/products/gift-kim-phuc.jpg',
     N'Hộp quà Kim Phúc 4 bánh'),
    (N'GIFT-LAM-PHUC-6', N'/images/products/gift-lam-phuc.jpg',
     N'Hộp quà Lam Phúc 6 bánh');

UPDATE image
SET image.image_url = seed.image_url,
    image.alt_text = seed.alt_text,
    image.sort_order = 0
FROM dbo.PRODUCT_IMAGE AS image
JOIN dbo.PRODUCT_VARIANT AS variant ON variant.variant_id = image.variant_id
JOIN @catalog_images AS seed ON seed.sku = variant.sku
WHERE image.is_primary = 1;

INSERT INTO dbo.PRODUCT_IMAGE
    (variant_id, image_url, alt_text, sort_order, is_primary)
SELECT
    variant.variant_id,
    seed.image_url,
    seed.alt_text,
    0,
    1
FROM @catalog_images AS seed
JOIN dbo.PRODUCT_VARIANT AS variant ON variant.sku = seed.sku
WHERE NOT EXISTS (
    SELECT 1
    FROM dbo.PRODUCT_IMAGE AS image
    WHERE image.variant_id = variant.variant_id
      AND image.is_primary = 1
);

IF (
    SELECT COUNT(*)
    FROM @catalog_images AS seed
    JOIN dbo.PRODUCT_VARIANT AS variant ON variant.sku = seed.sku
    JOIN dbo.PRODUCT_IMAGE AS image
        ON image.variant_id = variant.variant_id
       AND image.is_primary = 1
       AND image.image_url = seed.image_url
) <> 16
    THROW 51004, 'MoonCake V6 did not configure all 16 primary catalog images.', 1;
