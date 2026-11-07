CREATE TABLE dbo.STOCK_BATCH (
 batch_id bigint IDENTITY PRIMARY KEY,
 variant_id bigint NOT NULL REFERENCES dbo.PRODUCT_VARIANT(variant_id),
 batch_code nvarchar(80) NOT NULL,
 produced_on date NULL,
 expires_on date NULL,
 remaining_quantity int NOT NULL CHECK(remaining_quantity>=0),
 received_at datetime2(3) NOT NULL DEFAULT SYSUTCDATETIME(),
 CONSTRAINT UQ_STOCK_BATCH_code UNIQUE(variant_id,batch_code),
 CONSTRAINT CK_STOCK_BATCH_dates CHECK(produced_on IS NULL OR expires_on IS NULL OR expires_on>=produced_on)
);
CREATE TABLE dbo.BATCH_ALLOCATION (
 allocation_id bigint IDENTITY PRIMARY KEY,
 batch_id bigint NOT NULL REFERENCES dbo.STOCK_BATCH(batch_id),
 order_id bigint NULL REFERENCES dbo.SALES_ORDER(order_id),
 operation_key nvarchar(150) NOT NULL,
 quantity_delta int NOT NULL CHECK(quantity_delta<>0),
 created_at datetime2(3) NOT NULL DEFAULT SYSUTCDATETIME(),
 CONSTRAINT UQ_BATCH_ALLOCATION UNIQUE(operation_key,batch_id)
);
CREATE INDEX IX_BATCH_variant ON dbo.STOCK_BATCH(variant_id,expires_on,batch_id);
CREATE INDEX IX_BATCH_ALLOCATION_order ON dbo.BATCH_ALLOCATION(order_id);
-- Existing stock is explicitly unverified; do not invent production/expiry dates.
INSERT dbo.STOCK_BATCH(variant_id,batch_code,remaining_quantity)
 SELECT variant_id,N'OPENING',available_quantity FROM dbo.INVENTORY_BALANCE WHERE available_quantity>0;
