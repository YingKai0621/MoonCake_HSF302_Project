-- Local development helper for testing the complete customer checkout flow.
-- This is intentionally not a Flyway migration.
-- Safe to rerun: each demo SKU is only topped up to 100 when below that level.

USE MoonCakeDB;
GO

SET NOCOUNT ON;
SET XACT_ABORT ON;

IF DB_NAME() <> N'MoonCakeDB'
    THROW 51020, 'Safety check failed: this script may run only on MoonCakeDB.', 1;

IF (
    SELECT COUNT(*)
    FROM dbo.PRODUCT_VARIANT
    WHERE sku IN (
        N'MC-KSPM-080', N'MC-PBMHA-080', N'MC-CBTN-080', N'MC-TCGQ-080',
        N'MC-CBBB-080', N'MC-LSTT-080', N'MC-TXHN-120', N'MC-HSMC-120',
        N'MC-SCDHDC-120', N'MC-LXGQXO-150', N'MC-THRV-150', N'MC-SDHK-150',
        N'BOX-KIM-PHUC-4', N'BOX-LAM-PHUC-6',
        N'GIFT-KIM-PHUC-4', N'GIFT-LAM-PHUC-6'
    )
) <> 16
    THROW 51021, 'Expected 16 V3 demo SKUs. Run Flyway before this script.', 1;

IF (
    SELECT COUNT(*)
    FROM dbo.INVENTORY_BALANCE AS balance
    JOIN dbo.PRODUCT_VARIANT AS variant
        ON variant.variant_id = balance.variant_id
    WHERE variant.sku IN (
        N'MC-KSPM-080', N'MC-PBMHA-080', N'MC-CBTN-080', N'MC-TCGQ-080',
        N'MC-CBBB-080', N'MC-LSTT-080', N'MC-TXHN-120', N'MC-HSMC-120',
        N'MC-SCDHDC-120', N'MC-LXGQXO-150', N'MC-THRV-150', N'MC-SDHK-150',
        N'BOX-KIM-PHUC-4', N'BOX-LAM-PHUC-6',
        N'GIFT-KIM-PHUC-4', N'GIFT-LAM-PHUC-6'
    )
) <> 16
    THROW 51022, 'Expected inventory balances for all 16 V3 demo SKUs.', 1;

BEGIN TRANSACTION;

DECLARE @target_quantity INT = 100;
DECLARE @changes TABLE (
    variant_id BIGINT NOT NULL,
    quantity_delta INT NOT NULL
);

UPDATE balance
SET
    available_quantity = @target_quantity,
    stock_version = stock_version + 1,
    updated_at = SYSUTCDATETIME()
OUTPUT
    INSERTED.variant_id,
    INSERTED.available_quantity - DELETED.available_quantity
INTO @changes (variant_id, quantity_delta)
FROM dbo.INVENTORY_BALANCE AS balance
JOIN dbo.PRODUCT_VARIANT AS variant
    ON variant.variant_id = balance.variant_id
WHERE variant.sku IN (
    N'MC-KSPM-080', N'MC-PBMHA-080', N'MC-CBTN-080', N'MC-TCGQ-080',
    N'MC-CBBB-080', N'MC-LSTT-080', N'MC-TXHN-120', N'MC-HSMC-120',
    N'MC-SCDHDC-120', N'MC-LXGQXO-150', N'MC-THRV-150', N'MC-SDHK-150',
    N'BOX-KIM-PHUC-4', N'BOX-LAM-PHUC-6',
    N'GIFT-KIM-PHUC-4', N'GIFT-LAM-PHUC-6'
)
  AND balance.available_quantity < @target_quantity;

INSERT INTO dbo.STOCK_MOVEMENT (
    variant_id,
    order_id,
    performed_by_account_id,
    movement_type,
    quantity_delta,
    reason,
    operation_key,
    operation_group_key,
    created_at
)
SELECT
    inventory_change.variant_id,
    NULL,
    NULL,
    'ADJUST',
    inventory_change.quantity_delta,
    N'Bổ sung tồn kho demo để kiểm thử local',
    NULL,
    NULL,
    SYSUTCDATETIME()
FROM @changes AS inventory_change;

COMMIT TRANSACTION;

SELECT
    variant.sku,
    product.product_name,
    balance.available_quantity
FROM dbo.INVENTORY_BALANCE AS balance
JOIN dbo.PRODUCT_VARIANT AS variant
    ON variant.variant_id = balance.variant_id
JOIN dbo.PRODUCT AS product
    ON product.product_id = variant.product_id
WHERE variant.sku IN (
    N'MC-KSPM-080', N'MC-PBMHA-080', N'MC-CBTN-080', N'MC-TCGQ-080',
    N'MC-CBBB-080', N'MC-LSTT-080', N'MC-TXHN-120', N'MC-HSMC-120',
    N'MC-SCDHDC-120', N'MC-LXGQXO-150', N'MC-THRV-150', N'MC-SDHK-150',
    N'BOX-KIM-PHUC-4', N'BOX-LAM-PHUC-6',
    N'GIFT-KIM-PHUC-4', N'GIFT-LAM-PHUC-6'
)
ORDER BY variant.sku;
GO
