SET XACT_ABORT ON;

UPDATE dbo.PRODUCT
SET detailed_description = N'Bánh 150g, có thể mua lẻ hoặc chọn vào hộp quà tự chọn theo sở thích.'
WHERE slug IN (
    N'banh-lap-xuong-ga-quay-xot-xo',
    N'banh-tom-hum-xot-ruou-vang',
    N'banh-so-diep-xot-hong-kong'
);

IF @@ROWCOUNT <> 3
    THROW 51013, 'MoonCake V13 expected exactly three 150g mooncake products.', 1;

UPDATE dbo.PRODUCT
SET detailed_description = CASE slug
        WHEN N'vo-hop-kim-phuc-4-banh'
            THEN N'Vỏ hộp Kim Phúc 4 ngăn. Chọn vỏ hộp rồi chọn đủ 4 chiếc bánh yêu thích để tạo hộp quà mang dấu ấn riêng.'
        WHEN N'vo-hop-lam-phuc-6-banh'
            THEN N'Vỏ hộp Lam Phúc 6 ngăn. Chọn vỏ hộp rồi chọn đủ 6 chiếc bánh yêu thích để tạo hộp quà mang dấu ấn riêng.'
    END
WHERE slug IN (N'vo-hop-kim-phuc-4-banh', N'vo-hop-lam-phuc-6-banh');

IF @@ROWCOUNT <> 2
    THROW 51014, 'MoonCake V13 expected exactly two empty-box products.', 1;

UPDATE dbo.PRODUCT
SET short_description = CASE slug
        WHEN N'hop-qua-kim-phuc'
            THEN N'Hộp quà gồm bốn chiếc bánh trung thu được phối sẵn, kèm trà và bộ dao nĩa.'
        WHEN N'hop-qua-lam-phuc'
            THEN N'Hộp quà gồm sáu chiếc bánh trung thu được phối sẵn, kèm trà và bộ dao nĩa.'
    END,
    detailed_description = CASE slug
        WHEN N'hop-qua-kim-phuc'
            THEN N'Bốn hương vị bánh được chọn lọc và đóng trong hộp Kim Phúc sang trọng, kèm trà và bộ dao nĩa, phù hợp làm quà biếu gia đình, đối tác.'
        WHEN N'hop-qua-lam-phuc'
            THEN N'Sáu hương vị bánh được chọn lọc và đóng trong hộp Lam Phúc sang trọng, kèm trà và bộ dao nĩa, phù hợp làm quà biếu gia đình, đối tác.'
    END,
    shelf_life_info = N'Xem trên bao bì từng chiếc bánh.'
WHERE slug IN (N'hop-qua-kim-phuc', N'hop-qua-lam-phuc');

IF @@ROWCOUNT <> 2
    THROW 51015, 'MoonCake V13 expected exactly two gift-set products.', 1;
