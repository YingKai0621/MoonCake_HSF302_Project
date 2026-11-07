CREATE TABLE dbo.PRODUCT_VIDEO (
 video_id bigint IDENTITY PRIMARY KEY,
 product_id bigint NOT NULL REFERENCES dbo.PRODUCT(product_id),
 video_url nvarchar(1000) NOT NULL,
 caption nvarchar(250) NOT NULL,
 sort_order int NOT NULL DEFAULT 0 CHECK(sort_order>=0)
);
CREATE INDEX IX_PRODUCT_VIDEO_product ON dbo.PRODUCT_VIDEO(product_id,sort_order);
