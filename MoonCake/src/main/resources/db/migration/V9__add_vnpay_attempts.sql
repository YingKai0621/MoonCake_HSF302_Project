ALTER TABLE dbo.PAYMENT DROP CONSTRAINT CK_PAYMENT_method;
ALTER TABLE dbo.PAYMENT ADD CONSTRAINT CK_PAYMENT_method CHECK(payment_method IN ('COD','VNPAY'));
CREATE TABLE dbo.VNPAY_ATTEMPT (
 txn_ref varchar(32) NOT NULL PRIMARY KEY,
 order_id bigint NOT NULL REFERENCES dbo.SALES_ORDER(order_id),
 amount decimal(18,0) NOT NULL CHECK(amount>0),
 created_at datetime2(3) NOT NULL,
 expires_at datetime2(3) NOT NULL,
 transaction_no varchar(50) NULL,
 status varchar(16) NOT NULL DEFAULT 'PENDING' CHECK(status IN ('PENDING','PAID','FAILED'))
);
CREATE UNIQUE INDEX UX_VNPAY_transaction ON dbo.VNPAY_ATTEMPT(transaction_no) WHERE transaction_no IS NOT NULL;
CREATE INDEX IX_VNPAY_order ON dbo.VNPAY_ATTEMPT(order_id);
