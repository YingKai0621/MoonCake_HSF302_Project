CREATE TABLE dbo.FEEDBACK (
 feedback_id bigint IDENTITY PRIMARY KEY,
 customer_id bigint NOT NULL REFERENCES dbo.CUSTOMER(customer_id),
 content nvarchar(2000) NOT NULL,
 created_at datetime2(3) NOT NULL DEFAULT SYSUTCDATETIME()
);
CREATE INDEX IX_FEEDBACK_created ON dbo.FEEDBACK(created_at DESC);
