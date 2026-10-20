-- MoonCake MVP volume discounts: exactly four active tiers.
-- Quantities below 50 have no row and therefore receive 0% discount.

INSERT INTO dbo.VOLUME_DISCOUNT_TIER
    (discount_group, min_quantity, max_quantity, discount_percent, valid_from, valid_to, is_active)
VALUES
    ('GIFT_BOX',  50,  99,  5.00, NULL, NULL, 1),
    ('GIFT_BOX', 100, NULL, 10.00, NULL, NULL, 1),
    ('MOONCAKE',  50,  99,  5.00, NULL, NULL, 1),
    ('MOONCAKE', 100, NULL, 10.00, NULL, NULL, 1);

