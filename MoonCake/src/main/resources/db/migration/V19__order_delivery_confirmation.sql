SET XACT_ABORT ON;

ALTER TABLE dbo.SALES_ORDER
DROP CONSTRAINT CK_SALES_ORDER_status;

ALTER TABLE dbo.SALES_ORDER
ADD delivered_at DATETIME2(3) NULL,
    customer_confirmed_at DATETIME2(3) NULL;

EXEC(N'
    ALTER TABLE dbo.SALES_ORDER
    ADD CONSTRAINT CK_SALES_ORDER_status CHECK (
        order_status IN (
            ''PLACED'',
            ''PROCESSING'',
            ''SHIPPING'',
            ''DELIVERED'',
            ''COMPLETED'',
            ''CANCELLED''
        )
    );
');
