-- Replace only the active GIFT_BOX policy. Historical tiers stay available
-- for ORDER_ITEM.volume_discount_tier_id references.
UPDATE dbo.VOLUME_DISCOUNT_TIER
SET is_active = 0
WHERE discount_group = 'GIFT_BOX'
  AND is_active = 1;

INSERT INTO dbo.VOLUME_DISCOUNT_TIER
    (discount_group, min_quantity, max_quantity, discount_percent, valid_from, valid_to, is_active)
VALUES
    ('GIFT_BOX',   5,  19,  5.00, NULL, NULL, 1),
    ('GIFT_BOX',  20,  49,  8.00, NULL, NULL, 1),
    ('GIFT_BOX',  50,  99, 10.00, NULL, NULL, 1),
    ('GIFT_BOX', 100, 199, 12.00, NULL, NULL, 1),
    ('GIFT_BOX', 200, 499, 15.00, NULL, NULL, 1),
    ('GIFT_BOX', 500, NULL, 18.00, NULL, NULL, 1);
