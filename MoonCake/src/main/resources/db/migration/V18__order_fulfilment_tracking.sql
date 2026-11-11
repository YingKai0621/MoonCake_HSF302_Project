SET XACT_ABORT ON;

ALTER TABLE dbo.SALES_ORDER
ADD processing_at DATETIME2(3) NULL,
    shipping_at DATETIME2(3) NULL,
    completed_at DATETIME2(3) NULL,
    cancelled_at DATETIME2(3) NULL,
    shipping_carrier VARCHAR(20) NULL,
    tracking_code NVARCHAR(50) NULL,
    estimated_delivery_date DATE NULL;

EXEC(N'
    ALTER TABLE dbo.SALES_ORDER
    ADD CONSTRAINT CK_SALES_ORDER_shipping_carrier CHECK (
        shipping_carrier IS NULL
        OR shipping_carrier IN (''GHN'', ''GHTK'', ''VIETTEL_POST'', ''STORE'')
    );
');
