-- MoonCake MVP demo catalog.
-- Product names, weights, prices and PRESET recipes were referenced from the
-- public Mor'dan Bakery 2026 catalog on 2026-10-20, then rewritten for this
-- academic project. No third-party image is copied or hotlinked.
-- Sources:
--   https://mordanbakery.vn/cua-hang/
--   https://mordanbakery.vn/san-pham/combo-kim-phuc/
--   https://mordanbakery.vn/san-pham/combo-lam-phuc/
-- Inventory starts at zero because source-site availability is not MoonCake stock.

SET XACT_ABORT ON;

INSERT INTO dbo.CATEGORY (slug, category_name, is_active)
VALUES
    (N'banh-trung-thu', N'Bánh trung thu', 1),
    (N'hop-qua-trung-thu', N'Hộp quà Trung thu', 1);

INSERT INTO dbo.PRODUCT
    (category_id, slug, product_type, product_name, short_description,
     detailed_description, main_ingredients, shelf_life_info, storage_guidance, is_active)
SELECT
    category.category_id,
    seed.slug,
    seed.product_type,
    seed.product_name,
    seed.short_description,
    seed.detailed_description,
    seed.main_ingredients,
    seed.shelf_life_info,
    seed.storage_guidance,
    seed.is_active
FROM (VALUES
    (N'banh-trung-thu', N'banh-kim-sa-pho-mai', 'MOONCAKE',
     N'Bánh Kim Sa Phô Mai',
     N'Bánh trung thu nhân kim sa kết hợp vị phô mai béo dịu.',
     N'Mẫu bánh nhỏ gọn, phù hợp thưởng thức riêng hoặc phối trong hộp quà.',
     CAST(NULL AS NVARCHAR(700)),
     N'Xem ngày in trực tiếp trên bao bì sản phẩm.',
     N'Bảo quản nơi khô ráo, thoáng mát và tránh ánh nắng trực tiếp.', 1),
    (N'banh-trung-thu', N'banh-phuc-bon-tu-mochi-hanh-nhan', 'MOONCAKE',
     N'Bánh Phúc Bồn Tử – Mochi – Hạnh Nhân',
     N'Bánh trung thu kết hợp phúc bồn tử, mochi và hạnh nhân.',
     N'Hương vị trái cây kết hợp lớp mochi và hạt hạnh nhân.',
     CAST(NULL AS NVARCHAR(700)),
     N'Xem ngày in trực tiếp trên bao bì sản phẩm.',
     N'Bảo quản nơi khô ráo, thoáng mát và tránh ánh nắng trực tiếp.', 1),
    (N'banh-trung-thu', N'banh-cha-bong-trung-nghien', 'MOONCAKE',
     N'Bánh Chà Bông Trứng Nghiền',
     N'Bánh trung thu nhân mặn với chà bông và trứng nghiền.',
     N'Mẫu bánh nhân mặn cỡ nhỏ, phù hợp dùng riêng hoặc phối hộp quà.',
     CAST(NULL AS NVARCHAR(700)),
     N'Xem ngày in trực tiếp trên bao bì sản phẩm.',
     N'Bảo quản nơi khô ráo, thoáng mát và tránh ánh nắng trực tiếp.', 1),
    (N'banh-trung-thu', N'banh-thap-cam-ga-quay', 'MOONCAKE',
     N'Bánh Thập Cẩm Gà Quay',
     N'Bánh nướng nhân thập cẩm gà quay đậm vị.',
     N'Nhân mặn phối chà bông gà, lạp xưởng, các loại hạt và mứt.',
     N'Chà bông gà, lạp xưởng gà quay, mè, hạt điều, hạt dưa, mứt bí, lá chanh, gừng và bột mì.',
     N'Xem ngày in trực tiếp trên bao bì sản phẩm.',
     N'Bảo quản nơi khô ráo, thoáng mát và tránh ánh nắng trực tiếp.', 1),
    (N'banh-trung-thu', N'banh-cua-bat-buu', 'MOONCAKE',
     N'Bánh Cua Bát Bửu',
     N'Bánh nướng nhân thập cẩm cua với vị mặn ngọt cân bằng.',
     N'Nhân thập cẩm kết hợp cua, chà bông, lạp xưởng, các loại hạt và mứt.',
     N'Mè trắng, lạp xưởng, hạt điều, mứt bí, chà bông gà, mứt gừng, dăm bông gà, chả cua, bột mì và bột nếp.',
     N'Xem ngày in trực tiếp trên bao bì sản phẩm.',
     N'Bảo quản nơi khô ráo, thoáng mát và tránh ánh nắng trực tiếp.', 1),
    (N'banh-trung-thu', N'banh-luu-sa-than-tre-trung-muoi', 'MOONCAKE',
     N'Bánh Lưu Sa Than Tre Trứng Muối',
     N'Bánh trung thu vỏ than tre với nhân lưu sa trứng muối.',
     N'Phiên bản bánh nhỏ với lớp vỏ màu than tre và nhân lưu sa mặn béo.',
     N'Sô cô la đen, hạnh nhân, lòng đỏ trứng muối, rượu rum, bột mì, đường, bơ và sữa.',
     N'Xem ngày in trực tiếp trên bao bì sản phẩm.',
     N'Bảo quản nơi khô ráo, thoáng mát và tránh ánh nắng trực tiếp.', 1),
    (N'banh-trung-thu', N'banh-tra-xanh-hanh-nhan-trung-muoi', 'MOONCAKE',
     N'Bánh Trà Xanh – Hạnh Nhân Trứng Muối',
     N'Bánh nướng trà xanh kết hợp hạnh nhân và trứng muối.',
     N'Vị trà xanh dịu kết hợp hạnh nhân bùi và lòng đỏ trứng muối.',
     N'Bột mì, nước đường, bột trà xanh, hạnh nhân lát và lòng đỏ trứng muối.',
     N'Xem ngày in trực tiếp trên bao bì sản phẩm.',
     N'Bảo quản nơi khô ráo, thoáng mát và tránh ánh nắng trực tiếp.', 1),
    (N'banh-trung-thu', N'banh-hat-sen-mac-ca-trung-muoi', 'MOONCAKE',
     N'Bánh Hạt Sen – Mắc Ca Trứng Muối',
     N'Bánh nướng nhân hạt sen, mắc ca và trứng muối.',
     N'Nhân hạt sen kết hợp mắc ca và vị mặn nhẹ của trứng muối.',
     N'Hạt sen, mắc ca, trứng muối, bột mì, bơ, trứng gà và dầu thực vật.',
     N'Xem ngày in trực tiếp trên bao bì sản phẩm.',
     N'Bảo quản nơi khô ráo, thoáng mát và tránh ánh nắng trực tiếp.', 1),
    (N'banh-trung-thu', N'banh-so-co-la-dua-hat-de-cuoi', 'MOONCAKE',
     N'Bánh Sô Cô La – Dừa – Hạt Dẻ Cười',
     N'Bánh trung thu kết hợp sô cô la, dừa và hạt dẻ cười.',
     N'Hương vị hiện đại với sô cô la, dừa và hạt dẻ cười.',
     N'Sô cô la, dừa và hạt dẻ cười.',
     N'Xem ngày in trực tiếp trên bao bì sản phẩm.',
     N'Bảo quản nơi khô ráo, thoáng mát và tránh ánh nắng trực tiếp.', 1),
    (N'banh-trung-thu', N'banh-lap-xuong-ga-quay-xot-xo', 'MOONCAKE',
     N'Bánh Lạp Xưởng Gà Quay xốt XO Trứng Muối',
     N'Bánh nướng nhân mặn lạp xưởng gà quay, xốt XO và trứng muối.',
     N'Phiên bản 150g dùng làm bánh lẻ hoặc thành phần hộp quà PRESET.',
     N'Lạp xưởng gà quay, xốt XO và trứng muối.',
     N'Xem ngày in trực tiếp trên bao bì sản phẩm.',
     N'Bảo quản nơi khô ráo, thoáng mát và tránh ánh nắng trực tiếp.', 1),
    (N'banh-trung-thu', N'banh-tom-hum-xot-ruou-vang', 'MOONCAKE',
     N'Bánh Tôm Hùm xốt Rượu Vang Trứng Muối',
     N'Bánh nướng nhân mặn tôm hùm, xốt rượu vang và trứng muối.',
     N'Phiên bản 150g dùng làm bánh lẻ hoặc thành phần hộp quà PRESET.',
     N'Tôm hùm, xốt rượu vang và trứng muối.',
     N'Xem ngày in trực tiếp trên bao bì sản phẩm.',
     N'Bảo quản nơi khô ráo, thoáng mát và tránh ánh nắng trực tiếp.', 1),
    (N'banh-trung-thu', N'banh-so-diep-xot-hong-kong', 'MOONCAKE',
     N'Bánh Sò Điệp xốt Hồng Kông Trứng Muối',
     N'Bánh nướng nhân mặn sò điệp, xốt Hồng Kông và trứng muối.',
     N'Phiên bản 150g dùng làm bánh lẻ hoặc thành phần hộp quà PRESET.',
     N'Sò điệp, xốt Hồng Kông và trứng muối.',
     N'Xem ngày in trực tiếp trên bao bì sản phẩm.',
     N'Bảo quản nơi khô ráo, thoáng mát và tránh ánh nắng trực tiếp.', 1),
    (N'hop-qua-trung-thu', N'vo-hop-kim-phuc-4-banh', 'EMPTY_BOX',
     N'Vỏ hộp Kim Phúc 4 bánh',
     N'SKU vỏ hộp nội bộ dùng để mô tả hộp quà Kim Phúc.',
     N'Vỏ hộp phục vụ cấu hình PRESET; hiện chưa mở bán riêng.',
     CAST(NULL AS NVARCHAR(700)), CAST(NULL AS NVARCHAR(350)), CAST(NULL AS NVARCHAR(600)), 0),
    (N'hop-qua-trung-thu', N'vo-hop-lam-phuc-6-banh', 'EMPTY_BOX',
     N'Vỏ hộp Lam Phúc 6 bánh',
     N'SKU vỏ hộp nội bộ dùng để mô tả hộp quà Lam Phúc.',
     N'Vỏ hộp phục vụ cấu hình PRESET; hiện chưa mở bán riêng.',
     CAST(NULL AS NVARCHAR(700)), CAST(NULL AS NVARCHAR(350)), CAST(NULL AS NVARCHAR(600)), 0),
    (N'hop-qua-trung-thu', N'hop-qua-kim-phuc', 'GIFT_SET',
     N'Hộp quà Kim Phúc',
     N'Hộp quà PRESET gồm bốn bánh, kèm trà và bộ dao nĩa.',
     N'Thành phần bánh được khóa theo cấu hình PRESET Kim Phúc. Trà và bộ dao nĩa là phụ kiện mô tả, không phải SKU tồn kho trong MVP.',
     CAST(NULL AS NVARCHAR(700)),
     N'Hạn sử dụng của từng bánh xem trực tiếp trên bao bì.',
     N'Bảo quản nơi khô ráo, thoáng mát và tránh ánh nắng trực tiếp.', 1),
    (N'hop-qua-trung-thu', N'hop-qua-lam-phuc', 'GIFT_SET',
     N'Hộp quà Lam Phúc',
     N'Hộp quà PRESET gồm sáu bánh, kèm trà và bộ dao nĩa.',
     N'Thành phần bánh được khóa theo cấu hình PRESET Lam Phúc. Trà và bộ dao nĩa là phụ kiện mô tả, không phải SKU tồn kho trong MVP.',
     CAST(NULL AS NVARCHAR(700)),
     N'Hạn sử dụng của từng bánh xem trực tiếp trên bao bì.',
     N'Bảo quản nơi khô ráo, thoáng mát và tránh ánh nắng trực tiếp.', 1)
) AS seed
    (category_slug, slug, product_type, product_name, short_description,
     detailed_description, main_ingredients, shelf_life_info, storage_guidance, is_active)
JOIN dbo.CATEGORY AS category ON category.slug = seed.category_slug;

INSERT INTO dbo.PRODUCT_VARIANT
    (product_id, sku, variant_name, weight_g, filling_label, unit_price, is_active)
SELECT
    product.product_id,
    seed.sku,
    seed.variant_name,
    seed.weight_g,
    seed.filling_label,
    seed.unit_price,
    seed.is_active
FROM (VALUES
    (N'banh-kim-sa-pho-mai', N'MC-KSPM-080', N'Bánh 80g', 80, N'Kim sa phô mai', CAST(95000 AS DECIMAL(18,0)), 1),
    (N'banh-phuc-bon-tu-mochi-hanh-nhan', N'MC-PBMHA-080', N'Bánh 80g', 80, N'Phúc bồn tử – mochi – hạnh nhân', CAST(95000 AS DECIMAL(18,0)), 1),
    (N'banh-cha-bong-trung-nghien', N'MC-CBTN-080', N'Bánh 80g', 80, N'Chà bông trứng nghiền', CAST(105000 AS DECIMAL(18,0)), 1),
    (N'banh-thap-cam-ga-quay', N'MC-TCGQ-080', N'Bánh 80g', 80, N'Thập cẩm gà quay', CAST(110000 AS DECIMAL(18,0)), 1),
    (N'banh-cua-bat-buu', N'MC-CBBB-080', N'Bánh 80g', 80, N'Cua bát bửu', CAST(110000 AS DECIMAL(18,0)), 1),
    (N'banh-luu-sa-than-tre-trung-muoi', N'MC-LSTT-080', N'Bánh 80g', 80, N'Lưu sa than tre trứng muối', CAST(115000 AS DECIMAL(18,0)), 1),
    (N'banh-tra-xanh-hanh-nhan-trung-muoi', N'MC-TXHN-120', N'Bánh 120g', 120, N'Trà xanh – hạnh nhân trứng muối', CAST(135000 AS DECIMAL(18,0)), 1),
    (N'banh-hat-sen-mac-ca-trung-muoi', N'MC-HSMC-120', N'Bánh 120g', 120, N'Hạt sen – mắc ca trứng muối', CAST(135000 AS DECIMAL(18,0)), 1),
    (N'banh-so-co-la-dua-hat-de-cuoi', N'MC-SCDHDC-120', N'Bánh 120g', 120, N'Sô cô la – dừa – hạt dẻ cười', CAST(145000 AS DECIMAL(18,0)), 1),
    (N'banh-lap-xuong-ga-quay-xot-xo', N'MC-LXGQXO-150', N'Bánh 150g', 150, N'Lạp xưởng gà quay xốt XO trứng muối', CAST(165000 AS DECIMAL(18,0)), 1),
    (N'banh-tom-hum-xot-ruou-vang', N'MC-THRV-150', N'Bánh 150g', 150, N'Tôm hùm xốt rượu vang trứng muối', CAST(175000 AS DECIMAL(18,0)), 1),
    (N'banh-so-diep-xot-hong-kong', N'MC-SDHK-150', N'Bánh 150g', 150, N'Sò điệp xốt Hồng Kông trứng muối', CAST(185000 AS DECIMAL(18,0)), 1),
    (N'vo-hop-kim-phuc-4-banh', N'BOX-KIM-PHUC-4', N'Vỏ hộp 4 bánh', CAST(NULL AS INT), CAST(NULL AS NVARCHAR(180)), CAST(0 AS DECIMAL(18,0)), 0),
    (N'vo-hop-lam-phuc-6-banh', N'BOX-LAM-PHUC-6', N'Vỏ hộp 6 bánh', CAST(NULL AS INT), CAST(NULL AS NVARCHAR(180)), CAST(0 AS DECIMAL(18,0)), 0),
    (N'hop-qua-kim-phuc', N'GIFT-KIM-PHUC-4', N'Hộp quà 4 bánh', CAST(NULL AS INT), N'PRESET 4 bánh', CAST(590000 AS DECIMAL(18,0)), 1),
    (N'hop-qua-lam-phuc', N'GIFT-LAM-PHUC-6', N'Hộp quà 6 bánh', CAST(NULL AS INT), N'PRESET 6 bánh', CAST(930000 AS DECIMAL(18,0)), 1)
) AS seed (product_slug, sku, variant_name, weight_g, filling_label, unit_price, is_active)
JOIN dbo.PRODUCT AS product ON product.slug = seed.product_slug;

INSERT INTO dbo.BOX (empty_box_variant_id, capacity_cakes, material, box_notes, is_active)
SELECT
    variant.variant_id,
    seed.capacity_cakes,
    CAST(NULL AS NVARCHAR(180)),
    seed.box_notes,
    1
FROM (VALUES
    (N'BOX-KIM-PHUC-4', 4, N'Mẫu hộp dùng cho PRESET Kim Phúc; phụ kiện được ghi trong mô tả GIFT_SET.'),
    (N'BOX-LAM-PHUC-6', 6, N'Mẫu hộp dùng cho PRESET Lam Phúc; phụ kiện được ghi trong mô tả GIFT_SET.')
) AS seed (empty_box_sku, capacity_cakes, box_notes)
JOIN dbo.PRODUCT_VARIANT AS variant ON variant.sku = seed.empty_box_sku;

INSERT INTO dbo.BOX_CONFIGURATION
    (box_id, cart_id, preset_variant_id, configuration_type, configuration_status, created_at, updated_at)
SELECT
    box.box_id,
    NULL,
    gift_variant.variant_id,
    'PRESET',
    'READY',
    SYSUTCDATETIME(),
    SYSUTCDATETIME()
FROM (VALUES
    (N'BOX-KIM-PHUC-4', N'GIFT-KIM-PHUC-4'),
    (N'BOX-LAM-PHUC-6', N'GIFT-LAM-PHUC-6')
) AS seed (empty_box_sku, gift_set_sku)
JOIN dbo.PRODUCT_VARIANT AS box_variant ON box_variant.sku = seed.empty_box_sku
JOIN dbo.BOX AS box ON box.empty_box_variant_id = box_variant.variant_id
JOIN dbo.PRODUCT_VARIANT AS gift_variant ON gift_variant.sku = seed.gift_set_sku;

INSERT INTO dbo.BOX_CONFIGURATION_ITEM
    (configuration_id, mooncake_variant_id, quantity_per_box)
SELECT
    configuration.configuration_id,
    cake_variant.variant_id,
    seed.quantity_per_box
FROM (VALUES
    (N'GIFT-KIM-PHUC-4', N'MC-LXGQXO-150', 1),
    (N'GIFT-KIM-PHUC-4', N'MC-SDHK-150', 1),
    (N'GIFT-KIM-PHUC-4', N'MC-HSMC-120', 1),
    (N'GIFT-KIM-PHUC-4', N'MC-TXHN-120', 1),
    (N'GIFT-LAM-PHUC-6', N'MC-LXGQXO-150', 1),
    (N'GIFT-LAM-PHUC-6', N'MC-SDHK-150', 1),
    (N'GIFT-LAM-PHUC-6', N'MC-THRV-150', 1),
    (N'GIFT-LAM-PHUC-6', N'MC-TXHN-120', 1),
    (N'GIFT-LAM-PHUC-6', N'MC-HSMC-120', 1),
    (N'GIFT-LAM-PHUC-6', N'MC-SCDHDC-120', 1)
) AS seed (gift_set_sku, cake_sku, quantity_per_box)
JOIN dbo.PRODUCT_VARIANT AS gift_variant ON gift_variant.sku = seed.gift_set_sku
JOIN dbo.BOX_CONFIGURATION AS configuration
    ON configuration.preset_variant_id = gift_variant.variant_id
JOIN dbo.PRODUCT_VARIANT AS cake_variant ON cake_variant.sku = seed.cake_sku;

INSERT INTO dbo.INVENTORY_BALANCE
    (variant_id, available_quantity, low_stock_threshold, stock_version, updated_at)
SELECT
    variant.variant_id,
    0,
    5,
    0,
    SYSUTCDATETIME()
FROM dbo.PRODUCT_VARIANT AS variant
WHERE variant.sku IN (
    N'MC-KSPM-080', N'MC-PBMHA-080', N'MC-CBTN-080', N'MC-TCGQ-080',
    N'MC-CBBB-080', N'MC-LSTT-080', N'MC-TXHN-120', N'MC-HSMC-120',
    N'MC-SCDHDC-120', N'MC-LXGQXO-150', N'MC-THRV-150', N'MC-SDHK-150',
    N'BOX-KIM-PHUC-4', N'BOX-LAM-PHUC-6',
    N'GIFT-KIM-PHUC-4', N'GIFT-LAM-PHUC-6'
);

-- Fail migration atomically if any seed row was lost because of an unexpected
-- schema/data mismatch. Flyway records V3 only after all checks succeed.
IF (SELECT COUNT(*) FROM dbo.PRODUCT_VARIANT WHERE sku IN (
    N'MC-KSPM-080', N'MC-PBMHA-080', N'MC-CBTN-080', N'MC-TCGQ-080',
    N'MC-CBBB-080', N'MC-LSTT-080', N'MC-TXHN-120', N'MC-HSMC-120',
    N'MC-SCDHDC-120', N'MC-LXGQXO-150', N'MC-THRV-150', N'MC-SDHK-150',
    N'BOX-KIM-PHUC-4', N'BOX-LAM-PHUC-6',
    N'GIFT-KIM-PHUC-4', N'GIFT-LAM-PHUC-6'
)) <> 16
    THROW 51001, 'MoonCake V3 catalog seed did not create exactly 16 SKUs.', 1;

IF EXISTS (
    SELECT 1
    FROM dbo.BOX_CONFIGURATION AS configuration
    JOIN dbo.BOX AS box ON box.box_id = configuration.box_id
    JOIN dbo.BOX_CONFIGURATION_ITEM AS item
        ON item.configuration_id = configuration.configuration_id
    WHERE configuration.preset_variant_id IN (
        SELECT variant_id
        FROM dbo.PRODUCT_VARIANT
        WHERE sku IN (N'GIFT-KIM-PHUC-4', N'GIFT-LAM-PHUC-6')
    )
    GROUP BY configuration.configuration_id, box.capacity_cakes
    HAVING SUM(item.quantity_per_box) <> box.capacity_cakes
)
    THROW 51002, 'MoonCake V3 PRESET composition does not match box capacity.', 1;
