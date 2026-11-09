SET XACT_ABORT ON;

DECLARE @gallery TABLE (
    sku NVARCHAR(64) NOT NULL,
    image_url NVARCHAR(1000) NOT NULL,
    view_label NVARCHAR(50) NOT NULL,
    sort_order INT NOT NULL
);

INSERT INTO @gallery (sku, image_url, view_label, sort_order) VALUES
    (N'MC-CBBB-080', N'/images/products/mooncake-gallery-web/mooncake-cua-bat-buu/01-mat-tren.jpg', N'mặt trên', 1),
    (N'MC-CBBB-080', N'/images/products/mooncake-gallery-web/mooncake-cua-bat-buu/02-goc-nghieng.jpg', N'góc nghiêng', 2),
    (N'MC-CBBB-080', N'/images/products/mooncake-gallery-web/mooncake-cua-bat-buu/03-mat-cat.jpg', N'mặt cắt', 3),
    (N'MC-CBBB-080', N'/images/products/mooncake-gallery-web/mooncake-cua-bat-buu/04-can-canh-nhan.jpg', N'cận cảnh nhân', 4),
    (N'MC-HSMC-120', N'/images/products/mooncake-gallery-web/mooncake-hat-sen-mac-ca-trung-muoi/01-mat-tren.jpg', N'mặt trên', 1),
    (N'MC-HSMC-120', N'/images/products/mooncake-gallery-web/mooncake-hat-sen-mac-ca-trung-muoi/02-goc-nghieng.jpg', N'góc nghiêng', 2),
    (N'MC-HSMC-120', N'/images/products/mooncake-gallery-web/mooncake-hat-sen-mac-ca-trung-muoi/03-mat-cat.jpg', N'mặt cắt', 3),
    (N'MC-HSMC-120', N'/images/products/mooncake-gallery-web/mooncake-hat-sen-mac-ca-trung-muoi/04-can-canh-nhan.jpg', N'cận cảnh nhân', 4),
    (N'MC-KSPM-080', N'/images/products/mooncake-gallery-web/mooncake-kim-sa-pho-mai/01-mat-tren.jpg', N'mặt trên', 1),
    (N'MC-KSPM-080', N'/images/products/mooncake-gallery-web/mooncake-kim-sa-pho-mai/02-goc-nghieng.jpg', N'góc nghiêng', 2),
    (N'MC-KSPM-080', N'/images/products/mooncake-gallery-web/mooncake-kim-sa-pho-mai/03-mat-cat.jpg', N'mặt cắt', 3),
    (N'MC-KSPM-080', N'/images/products/mooncake-gallery-web/mooncake-kim-sa-pho-mai/04-can-canh-nhan.jpg', N'cận cảnh nhân', 4),
    (N'MC-LSTT-080', N'/images/products/mooncake-gallery-web/mooncake-luu-sa-than-tre-trung-muoi/01-mat-tren.jpg', N'mặt trên', 1),
    (N'MC-LSTT-080', N'/images/products/mooncake-gallery-web/mooncake-luu-sa-than-tre-trung-muoi/02-goc-nghieng.jpg', N'góc nghiêng', 2),
    (N'MC-LSTT-080', N'/images/products/mooncake-gallery-web/mooncake-luu-sa-than-tre-trung-muoi/03-mat-cat.jpg', N'mặt cắt', 3),
    (N'MC-LSTT-080', N'/images/products/mooncake-gallery-web/mooncake-luu-sa-than-tre-trung-muoi/04-can-canh-nhan.jpg', N'cận cảnh nhân', 4),
    (N'MC-PBMHA-080', N'/images/products/mooncake-gallery-web/mooncake-phuc-bon-tu-mochi-hanh-nhan/01-mat-tren.jpg', N'mặt trên', 1),
    (N'MC-PBMHA-080', N'/images/products/mooncake-gallery-web/mooncake-phuc-bon-tu-mochi-hanh-nhan/02-goc-nghieng.jpg', N'góc nghiêng', 2),
    (N'MC-PBMHA-080', N'/images/products/mooncake-gallery-web/mooncake-phuc-bon-tu-mochi-hanh-nhan/03-mat-cat.jpg', N'mặt cắt', 3),
    (N'MC-PBMHA-080', N'/images/products/mooncake-gallery-web/mooncake-phuc-bon-tu-mochi-hanh-nhan/04-can-canh-nhan.jpg', N'cận cảnh nhân', 4),
    (N'MC-SCDHDC-120', N'/images/products/mooncake-gallery-web/mooncake-so-co-la-dua-hat-de-cuoi/01-mat-tren.jpg', N'mặt trên', 1),
    (N'MC-SCDHDC-120', N'/images/products/mooncake-gallery-web/mooncake-so-co-la-dua-hat-de-cuoi/02-goc-nghieng.jpg', N'góc nghiêng', 2),
    (N'MC-SCDHDC-120', N'/images/products/mooncake-gallery-web/mooncake-so-co-la-dua-hat-de-cuoi/03-mat-cat.jpg', N'mặt cắt', 3),
    (N'MC-SCDHDC-120', N'/images/products/mooncake-gallery-web/mooncake-so-co-la-dua-hat-de-cuoi/04-can-canh-nhan.jpg', N'cận cảnh nhân', 4),
    (N'MC-SDHK-150', N'/images/products/mooncake-gallery-web/mooncake-so-diep-xot-hong-kong/01-mat-tren.jpg', N'mặt trên', 1),
    (N'MC-SDHK-150', N'/images/products/mooncake-gallery-web/mooncake-so-diep-xot-hong-kong/02-goc-nghieng.jpg', N'góc nghiêng', 2),
    (N'MC-SDHK-150', N'/images/products/mooncake-gallery-web/mooncake-so-diep-xot-hong-kong/03-mat-cat.jpg', N'mặt cắt', 3),
    (N'MC-SDHK-150', N'/images/products/mooncake-gallery-web/mooncake-so-diep-xot-hong-kong/04-can-canh-nhan.jpg', N'cận cảnh nhân', 4),
    (N'MC-TCGQ-080', N'/images/products/mooncake-gallery-web/mooncake-thap-cam-ga-quay/01-mat-tren.jpg', N'mặt trên', 1),
    (N'MC-TCGQ-080', N'/images/products/mooncake-gallery-web/mooncake-thap-cam-ga-quay/02-goc-nghieng.jpg', N'góc nghiêng', 2),
    (N'MC-TCGQ-080', N'/images/products/mooncake-gallery-web/mooncake-thap-cam-ga-quay/03-mat-cat.jpg', N'mặt cắt', 3),
    (N'MC-TCGQ-080', N'/images/products/mooncake-gallery-web/mooncake-thap-cam-ga-quay/04-can-canh-nhan.jpg', N'cận cảnh nhân', 4),
    (N'MC-THRV-150', N'/images/products/mooncake-gallery-web/mooncake-tom-hum-xot-ruou-vang/01-mat-tren.jpg', N'mặt trên', 1),
    (N'MC-THRV-150', N'/images/products/mooncake-gallery-web/mooncake-tom-hum-xot-ruou-vang/02-goc-nghieng.jpg', N'góc nghiêng', 2),
    (N'MC-THRV-150', N'/images/products/mooncake-gallery-web/mooncake-tom-hum-xot-ruou-vang/03-mat-cat.jpg', N'mặt cắt', 3),
    (N'MC-THRV-150', N'/images/products/mooncake-gallery-web/mooncake-tom-hum-xot-ruou-vang/04-can-canh-nhan.jpg', N'cận cảnh nhân', 4),
    (N'MC-TXHN-120', N'/images/products/mooncake-gallery-web/mooncake-tra-xanh-hanh-nhan-trung-muoi/01-mat-tren.jpg', N'mặt trên', 1),
    (N'MC-TXHN-120', N'/images/products/mooncake-gallery-web/mooncake-tra-xanh-hanh-nhan-trung-muoi/02-goc-nghieng.jpg', N'góc nghiêng', 2),
    (N'MC-TXHN-120', N'/images/products/mooncake-gallery-web/mooncake-tra-xanh-hanh-nhan-trung-muoi/03-mat-cat.jpg', N'mặt cắt', 3),
    (N'MC-TXHN-120', N'/images/products/mooncake-gallery-web/mooncake-tra-xanh-hanh-nhan-trung-muoi/04-can-canh-nhan.jpg', N'cận cảnh nhân', 4);

IF (
    SELECT COUNT(*)
    FROM @gallery AS seed
    JOIN dbo.PRODUCT_VARIANT AS variant ON variant.sku = seed.sku
) <> 40
    THROW 51140, 'MoonCake V14 expected 40 gallery images for 10 existing mooncake SKUs.', 1;

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
