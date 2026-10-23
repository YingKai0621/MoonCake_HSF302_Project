-- Store the final COD cash-rounding difference without rewriting historical orders.
-- Existing orders receive 0 and continue to satisfy their original totals.

SET XACT_ABORT ON;

ALTER TABLE dbo.SALES_ORDER
DROP CONSTRAINT CK_SALES_ORDER_totals;

ALTER TABLE dbo.SALES_ORDER
ADD rounding_adjustment DECIMAL(18,0) NOT NULL
    CONSTRAINT DF_SALES_ORDER_rounding_adjustment DEFAULT (0) WITH VALUES;

-- SQL Server compiles one migration batch before executing it. Dynamic SQL makes
-- this constraint compile only after the new column has been added above.
EXEC(N'
    ALTER TABLE dbo.SALES_ORDER
    ADD CONSTRAINT CK_SALES_ORDER_totals CHECK (
        subtotal_amount >= 0
        AND discount_amount >= 0
        AND discount_amount <= subtotal_amount
        AND shipping_fee >= 0
        AND rounding_adjustment >= -499
        AND rounding_adjustment <= 500
        AND total_amount >= 0
        AND (rounding_adjustment = 0 OR total_amount % 1000 = 0)
        AND total_amount = subtotal_amount - discount_amount + shipping_fee + rounding_adjustment
    );
');
