SET XACT_ABORT ON;

ALTER TABLE dbo.PRODUCT
ADD shelf_life_days INT NULL;

EXEC(N'
    ALTER TABLE dbo.PRODUCT
    ADD CONSTRAINT CK_PRODUCT_shelf_life_days CHECK (
        shelf_life_days IS NULL
        OR (shelf_life_days >= 1 AND shelf_life_days <= 90)
    );
');

EXEC(N'
    UPDATE dbo.PRODUCT
    SET shelf_life_days = 30
    WHERE product_type IN (''MOONCAKE'', ''GIFT_SET'');
');
