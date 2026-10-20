-- MoonCake MVP — SQL Server physical schema (DESIGN DDL; NOT executed).
-- Based on approved Business Rules v1.1 and Logical V5, 19 tables. Target: Microsoft SQL Server 2019+.
-- Create an EMPTY database first; run this script using SSMS / sqlcmd after review.
-- Currency: VND as DECIMAL(18,0). Timestamps: UTC DATETIME2(3).
-- Future payOS/VietQR: add fields/statuses/attempts in a NEW migration; not included here.
-- No reservation column; checkout decrements available_quantity atomically.

SET XACT_ABORT ON;

CREATE TABLE dbo.CATEGORY (
    category_id BIGINT IDENTITY(1,1) NOT NULL,
    slug NVARCHAR(160) NOT NULL,
    category_name NVARCHAR(160) NOT NULL,
    is_active BIT NOT NULL DEFAULT (1),
    CONSTRAINT PK_CATEGORY PRIMARY KEY CLUSTERED (category_id)
);

CREATE TABLE dbo.PRODUCT (
    product_id BIGINT IDENTITY(1,1) NOT NULL,
    category_id BIGINT NOT NULL,
    slug NVARCHAR(180) NOT NULL,
    product_type VARCHAR(20) NOT NULL,
    product_name NVARCHAR(220) NOT NULL,
    short_description NVARCHAR(600) NOT NULL,
    detailed_description NVARCHAR(MAX) NULL,
    main_ingredients NVARCHAR(700) NULL,
    shelf_life_info NVARCHAR(350) NULL,
    storage_guidance NVARCHAR(600) NULL,
    is_active BIT NOT NULL DEFAULT (1),
    CONSTRAINT PK_PRODUCT PRIMARY KEY CLUSTERED (product_id)
);

CREATE TABLE dbo.PRODUCT_VARIANT (
    variant_id BIGINT IDENTITY(1,1) NOT NULL,
    product_id BIGINT NOT NULL,
    sku NVARCHAR(80) NOT NULL,
    variant_name NVARCHAR(200) NOT NULL,
    weight_g INT NULL,
    filling_label NVARCHAR(180) NULL,
    unit_price DECIMAL(18,0) NOT NULL,
    is_active BIT NOT NULL DEFAULT (1),
    CONSTRAINT PK_PRODUCT_VARIANT PRIMARY KEY CLUSTERED (variant_id)
);

CREATE TABLE dbo.PRODUCT_IMAGE (
    image_id BIGINT IDENTITY(1,1) NOT NULL,
    variant_id BIGINT NOT NULL,
    image_url NVARCHAR(1000) NOT NULL,
    alt_text NVARCHAR(250) NULL,
    sort_order INT NOT NULL DEFAULT (0),
    is_primary BIT NOT NULL DEFAULT (0),
    CONSTRAINT PK_PRODUCT_IMAGE PRIMARY KEY CLUSTERED (image_id)
);

CREATE TABLE dbo.BOX (
    box_id BIGINT IDENTITY(1,1) NOT NULL,
    empty_box_variant_id BIGINT NOT NULL,
    capacity_cakes INT NOT NULL,
    material NVARCHAR(180) NULL,
    box_notes NVARCHAR(500) NULL,
    is_active BIT NOT NULL DEFAULT (1),
    CONSTRAINT PK_BOX PRIMARY KEY CLUSTERED (box_id)
);

CREATE TABLE dbo.ACCOUNT (
    account_id BIGINT IDENTITY(1,1) NOT NULL,
    email NVARCHAR(254) NOT NULL,
    password_hash NVARCHAR(255) NOT NULL,
    role VARCHAR(16) NOT NULL DEFAULT ('CUSTOMER'),
    status VARCHAR(16) NOT NULL DEFAULT ('ACTIVE'),
    created_at DATETIME2(3) NOT NULL DEFAULT (SYSUTCDATETIME()),
    CONSTRAINT PK_ACCOUNT PRIMARY KEY CLUSTERED (account_id)
);

CREATE TABLE dbo.CUSTOMER (
    customer_id BIGINT IDENTITY(1,1) NOT NULL,
    account_id BIGINT NULL,
    full_name NVARCHAR(160) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    email NVARCHAR(254) NULL,
    created_at DATETIME2(3) NOT NULL DEFAULT (SYSUTCDATETIME()),
    CONSTRAINT PK_CUSTOMER PRIMARY KEY CLUSTERED (customer_id)
);

CREATE TABLE dbo.CART (
    cart_id BIGINT IDENTITY(1,1) NOT NULL,
    customer_id BIGINT NULL,
    guest_token NVARCHAR(128) NULL,
    cart_status VARCHAR(16) NOT NULL DEFAULT ('ACTIVE'),
    created_at DATETIME2(3) NOT NULL DEFAULT (SYSUTCDATETIME()),
    updated_at DATETIME2(3) NOT NULL DEFAULT (SYSUTCDATETIME()),
    CONSTRAINT PK_CART PRIMARY KEY CLUSTERED (cart_id)
);

CREATE TABLE dbo.BOX_CONFIGURATION (
    configuration_id BIGINT IDENTITY(1,1) NOT NULL,
    box_id BIGINT NOT NULL,
    cart_id BIGINT NULL,
    preset_variant_id BIGINT NULL,
    configuration_type VARCHAR(12) NOT NULL,
    configuration_status VARCHAR(16) NOT NULL DEFAULT ('DRAFT'),
    created_at DATETIME2(3) NOT NULL DEFAULT (SYSUTCDATETIME()),
    updated_at DATETIME2(3) NOT NULL DEFAULT (SYSUTCDATETIME()),
    CONSTRAINT PK_BOX_CONFIGURATION PRIMARY KEY CLUSTERED (configuration_id)
);

CREATE TABLE dbo.BOX_CONFIGURATION_ITEM (
    configuration_item_id BIGINT IDENTITY(1,1) NOT NULL,
    configuration_id BIGINT NOT NULL,
    mooncake_variant_id BIGINT NOT NULL,
    quantity_per_box INT NOT NULL,
    CONSTRAINT PK_BOX_CONFIGURATION_ITEM PRIMARY KEY CLUSTERED (configuration_item_id)
);

CREATE TABLE dbo.CART_ITEM (
    cart_item_id BIGINT IDENTITY(1,1) NOT NULL,
    cart_id BIGINT NOT NULL,
    variant_id BIGINT NULL,
    configuration_id BIGINT NULL,
    quantity INT NOT NULL,
    added_at DATETIME2(3) NOT NULL DEFAULT (SYSUTCDATETIME()),
    CONSTRAINT PK_CART_ITEM PRIMARY KEY CLUSTERED (cart_item_id)
);

CREATE TABLE dbo.BUSINESS (
    business_id BIGINT IDENTITY(1,1) NOT NULL,
    business_name NVARCHAR(220) NOT NULL,
    tax_code NVARCHAR(30) NULL,
    contact_name NVARCHAR(160) NOT NULL,
    contact_phone VARCHAR(20) NOT NULL,
    contact_email NVARCHAR(254) NULL,
    is_active BIT NOT NULL DEFAULT (1),
    CONSTRAINT PK_BUSINESS PRIMARY KEY CLUSTERED (business_id)
);

CREATE TABLE dbo.SALES_ORDER (
    order_id BIGINT IDENTITY(1,1) NOT NULL,
    order_code NVARCHAR(40) NOT NULL,
    customer_id BIGINT NULL,
    business_id BIGINT NULL,
    order_type VARCHAR(12) NOT NULL,
    buyer_name_snapshot NVARCHAR(160) NOT NULL,
    buyer_phone_snapshot VARCHAR(20) NOT NULL,
    buyer_email_snapshot NVARCHAR(254) NULL,
    recipient_name NVARCHAR(160) NOT NULL,
    recipient_phone VARCHAR(20) NOT NULL,
    shipping_address_snapshot NVARCHAR(500) NOT NULL,
    order_note NVARCHAR(1000) NULL,
    subtotal_amount DECIMAL(18,0) NOT NULL,
    discount_amount DECIMAL(18,0) NOT NULL DEFAULT (0),
    shipping_fee DECIMAL(18,0) NOT NULL DEFAULT (0),
    total_amount DECIMAL(18,0) NOT NULL,
    order_status VARCHAR(16) NOT NULL DEFAULT ('PLACED'),
    created_at DATETIME2(3) NOT NULL DEFAULT (SYSUTCDATETIME()),
    updated_at DATETIME2(3) NOT NULL DEFAULT (SYSUTCDATETIME()),
    CONSTRAINT PK_SALES_ORDER PRIMARY KEY CLUSTERED (order_id)
);

CREATE TABLE dbo.VOLUME_DISCOUNT_TIER (
    volume_discount_tier_id BIGINT IDENTITY(1,1) NOT NULL,
    discount_group VARCHAR(12) NOT NULL,
    min_quantity INT NOT NULL,
    max_quantity INT NULL,
    discount_percent DECIMAL(5,2) NOT NULL,
    valid_from DATETIME2(3) NULL,
    valid_to DATETIME2(3) NULL,
    is_active BIT NOT NULL DEFAULT (1),
    CONSTRAINT PK_VOLUME_DISCOUNT_TIER PRIMARY KEY CLUSTERED (volume_discount_tier_id)
);

CREATE TABLE dbo.ORDER_ITEM (
    order_item_id BIGINT IDENTITY(1,1) NOT NULL,
    order_id BIGINT NOT NULL,
    variant_id BIGINT NULL,
    configuration_id BIGINT NULL,
    volume_discount_tier_id BIGINT NULL,
    item_kind VARCHAR(12) NOT NULL,
    inventory_source_snapshot VARCHAR(16) NOT NULL,
    product_name_snapshot NVARCHAR(220) NOT NULL,
    sku_snapshot NVARCHAR(80) NULL,
    discount_group_snapshot VARCHAR(12) NULL,
    box_name_snapshot NVARCHAR(220) NULL,
    box_sku_snapshot NVARCHAR(80) NULL,
    quantity INT NOT NULL,
    unit_base_price_snapshot DECIMAL(18,0) NOT NULL,
    discount_pct_snapshot DECIMAL(5,2) NOT NULL DEFAULT (0),
    unit_discount_snapshot DECIMAL(18,0) NOT NULL DEFAULT (0),
    unit_final_price_snapshot DECIMAL(18,0) NOT NULL,
    line_total DECIMAL(18,0) NOT NULL,
    CONSTRAINT PK_ORDER_ITEM PRIMARY KEY CLUSTERED (order_item_id)
);

CREATE TABLE dbo.ORDER_ITEM_COMPONENT (
    order_component_id BIGINT IDENTITY(1,1) NOT NULL,
    order_item_id BIGINT NOT NULL,
    component_variant_id BIGINT NOT NULL,
    component_role VARCHAR(8) NOT NULL,
    component_name_snapshot NVARCHAR(220) NOT NULL,
    quantity_per_set INT NOT NULL,
    unit_price_snapshot DECIMAL(18,0) NOT NULL,
    CONSTRAINT PK_ORDER_ITEM_COMPONENT PRIMARY KEY CLUSTERED (order_component_id)
);

CREATE TABLE dbo.PAYMENT (
    payment_id BIGINT IDENTITY(1,1) NOT NULL,
    order_id BIGINT NOT NULL,
    payment_method VARCHAR(16) NOT NULL,
    payment_status VARCHAR(16) NOT NULL DEFAULT ('UNPAID'),
    amount DECIMAL(18,0) NOT NULL,
    paid_at DATETIME2(3) NULL,
    created_at DATETIME2(3) NOT NULL DEFAULT (SYSUTCDATETIME()),
    updated_at DATETIME2(3) NOT NULL DEFAULT (SYSUTCDATETIME()),
    CONSTRAINT PK_PAYMENT PRIMARY KEY CLUSTERED (payment_id)
);

CREATE TABLE dbo.INVENTORY_BALANCE (
    variant_id BIGINT NOT NULL,
    available_quantity INT NOT NULL DEFAULT (0),
    low_stock_threshold INT NOT NULL DEFAULT (5),
    stock_version BIGINT NOT NULL DEFAULT (0),
    updated_at DATETIME2(3) NOT NULL DEFAULT (SYSUTCDATETIME()),
    CONSTRAINT PK_INVENTORY_BALANCE PRIMARY KEY CLUSTERED (variant_id)
);

CREATE TABLE dbo.STOCK_MOVEMENT (
    movement_id BIGINT IDENTITY(1,1) NOT NULL,
    variant_id BIGINT NOT NULL,
    order_id BIGINT NULL,
    performed_by_account_id BIGINT NULL,
    movement_type VARCHAR(20) NOT NULL,
    quantity_delta INT NOT NULL,
    reason NVARCHAR(500) NULL,
    operation_key NVARCHAR(150) NULL,
    operation_group_key NVARCHAR(150) NULL,
    created_at DATETIME2(3) NOT NULL DEFAULT (SYSUTCDATETIME()),
    CONSTRAINT PK_STOCK_MOVEMENT PRIMARY KEY CLUSTERED (movement_id)
);

-- Relationships — ON DELETE NO ACTION (retain historical orders / stock ledger).
ALTER TABLE dbo.PRODUCT ADD CONSTRAINT FK_PRODUCT_category_id FOREIGN KEY (category_id) REFERENCES dbo.CATEGORY(category_id);
ALTER TABLE dbo.PRODUCT_VARIANT ADD CONSTRAINT FK_PRODUCT_VARIANT_product_id FOREIGN KEY (product_id) REFERENCES dbo.PRODUCT(product_id);
ALTER TABLE dbo.PRODUCT_IMAGE ADD CONSTRAINT FK_PRODUCT_IMAGE_variant_id FOREIGN KEY (variant_id) REFERENCES dbo.PRODUCT_VARIANT(variant_id);
ALTER TABLE dbo.BOX ADD CONSTRAINT FK_BOX_empty_box_variant_id FOREIGN KEY (empty_box_variant_id) REFERENCES dbo.PRODUCT_VARIANT(variant_id);
ALTER TABLE dbo.CUSTOMER ADD CONSTRAINT FK_CUSTOMER_account_id FOREIGN KEY (account_id) REFERENCES dbo.ACCOUNT(account_id);
ALTER TABLE dbo.CART ADD CONSTRAINT FK_CART_customer_id FOREIGN KEY (customer_id) REFERENCES dbo.CUSTOMER(customer_id);
ALTER TABLE dbo.BOX_CONFIGURATION ADD CONSTRAINT FK_BOX_CONFIGURATION_box_id FOREIGN KEY (box_id) REFERENCES dbo.BOX(box_id);
ALTER TABLE dbo.BOX_CONFIGURATION ADD CONSTRAINT FK_BOX_CONFIGURATION_cart_id FOREIGN KEY (cart_id) REFERENCES dbo.CART(cart_id);
ALTER TABLE dbo.BOX_CONFIGURATION ADD CONSTRAINT FK_BOX_CONFIGURATION_preset_variant_id FOREIGN KEY (preset_variant_id) REFERENCES dbo.PRODUCT_VARIANT(variant_id);
ALTER TABLE dbo.BOX_CONFIGURATION_ITEM ADD CONSTRAINT FK_BOX_CONFIGURATION_ITEM_configuration_id FOREIGN KEY (configuration_id) REFERENCES dbo.BOX_CONFIGURATION(configuration_id);
ALTER TABLE dbo.BOX_CONFIGURATION_ITEM ADD CONSTRAINT FK_BOX_CONFIGURATION_ITEM_mooncake_variant_id FOREIGN KEY (mooncake_variant_id) REFERENCES dbo.PRODUCT_VARIANT(variant_id);
ALTER TABLE dbo.CART_ITEM ADD CONSTRAINT FK_CART_ITEM_cart_id FOREIGN KEY (cart_id) REFERENCES dbo.CART(cart_id);
ALTER TABLE dbo.CART_ITEM ADD CONSTRAINT FK_CART_ITEM_variant_id FOREIGN KEY (variant_id) REFERENCES dbo.PRODUCT_VARIANT(variant_id);
ALTER TABLE dbo.CART_ITEM ADD CONSTRAINT FK_CART_ITEM_configuration_id FOREIGN KEY (configuration_id) REFERENCES dbo.BOX_CONFIGURATION(configuration_id);
ALTER TABLE dbo.SALES_ORDER ADD CONSTRAINT FK_SALES_ORDER_customer_id FOREIGN KEY (customer_id) REFERENCES dbo.CUSTOMER(customer_id);
ALTER TABLE dbo.SALES_ORDER ADD CONSTRAINT FK_SALES_ORDER_business_id FOREIGN KEY (business_id) REFERENCES dbo.BUSINESS(business_id);
ALTER TABLE dbo.ORDER_ITEM ADD CONSTRAINT FK_ORDER_ITEM_order_id FOREIGN KEY (order_id) REFERENCES dbo.SALES_ORDER(order_id);
ALTER TABLE dbo.ORDER_ITEM ADD CONSTRAINT FK_ORDER_ITEM_variant_id FOREIGN KEY (variant_id) REFERENCES dbo.PRODUCT_VARIANT(variant_id);
ALTER TABLE dbo.ORDER_ITEM ADD CONSTRAINT FK_ORDER_ITEM_configuration_id FOREIGN KEY (configuration_id) REFERENCES dbo.BOX_CONFIGURATION(configuration_id);
ALTER TABLE dbo.ORDER_ITEM ADD CONSTRAINT FK_ORDER_ITEM_volume_discount_tier_id FOREIGN KEY (volume_discount_tier_id) REFERENCES dbo.VOLUME_DISCOUNT_TIER(volume_discount_tier_id);
ALTER TABLE dbo.ORDER_ITEM_COMPONENT ADD CONSTRAINT FK_ORDER_ITEM_COMPONENT_order_item_id FOREIGN KEY (order_item_id) REFERENCES dbo.ORDER_ITEM(order_item_id);
ALTER TABLE dbo.ORDER_ITEM_COMPONENT ADD CONSTRAINT FK_ORDER_ITEM_COMPONENT_component_variant_id FOREIGN KEY (component_variant_id) REFERENCES dbo.PRODUCT_VARIANT(variant_id);
ALTER TABLE dbo.PAYMENT ADD CONSTRAINT FK_PAYMENT_order_id FOREIGN KEY (order_id) REFERENCES dbo.SALES_ORDER(order_id);
ALTER TABLE dbo.INVENTORY_BALANCE ADD CONSTRAINT FK_INVENTORY_BALANCE_variant_id FOREIGN KEY (variant_id) REFERENCES dbo.PRODUCT_VARIANT(variant_id);
ALTER TABLE dbo.STOCK_MOVEMENT ADD CONSTRAINT FK_STOCK_MOVEMENT_variant_id FOREIGN KEY (variant_id) REFERENCES dbo.PRODUCT_VARIANT(variant_id);
ALTER TABLE dbo.STOCK_MOVEMENT ADD CONSTRAINT FK_STOCK_MOVEMENT_order_id FOREIGN KEY (order_id) REFERENCES dbo.SALES_ORDER(order_id);
ALTER TABLE dbo.STOCK_MOVEMENT ADD CONSTRAINT FK_STOCK_MOVEMENT_performed_by_account_id FOREIGN KEY (performed_by_account_id) REFERENCES dbo.ACCOUNT(account_id);

-- A custom box referenced by a cart item MUST belong to the same cart.
ALTER TABLE dbo.BOX_CONFIGURATION ADD CONSTRAINT UQ_BOX_CONFIGURATION_cart_config UNIQUE (cart_id, configuration_id);
ALTER TABLE dbo.CART_ITEM ADD CONSTRAINT FK_CART_ITEM_config_same_cart FOREIGN KEY (cart_id, configuration_id) REFERENCES dbo.BOX_CONFIGURATION(cart_id, configuration_id);

-- CHECK constraints
ALTER TABLE dbo.PRODUCT ADD CONSTRAINT CK_PRODUCT_product_type CHECK (product_type IN ('MOONCAKE','EMPTY_BOX','GIFT_SET'));
ALTER TABLE dbo.PRODUCT_VARIANT ADD CONSTRAINT CK_PRODUCT_VARIANT_price CHECK (unit_price >= 0);
ALTER TABLE dbo.PRODUCT_VARIANT ADD CONSTRAINT CK_PRODUCT_VARIANT_weight CHECK (weight_g IS NULL OR weight_g > 0);
ALTER TABLE dbo.BOX ADD CONSTRAINT CK_BOX_capacity CHECK (capacity_cakes > 0);
ALTER TABLE dbo.ACCOUNT ADD CONSTRAINT CK_ACCOUNT_role CHECK (role IN ('ADMIN','CUSTOMER'));
ALTER TABLE dbo.ACCOUNT ADD CONSTRAINT CK_ACCOUNT_status CHECK (status IN ('ACTIVE','DISABLED'));
ALTER TABLE dbo.CART ADD CONSTRAINT CK_CART_owner CHECK ((customer_id IS NOT NULL AND guest_token IS NULL) OR (customer_id IS NULL AND guest_token IS NOT NULL));
ALTER TABLE dbo.CART ADD CONSTRAINT CK_CART_status CHECK (cart_status IN ('ACTIVE','CHECKED_OUT','ABANDONED'));
ALTER TABLE dbo.BOX_CONFIGURATION ADD CONSTRAINT CK_BOX_CONFIGURATION_type CHECK ((configuration_type = 'PRESET' AND preset_variant_id IS NOT NULL AND cart_id IS NULL) OR (configuration_type = 'CUSTOM' AND preset_variant_id IS NULL AND cart_id IS NOT NULL));
ALTER TABLE dbo.BOX_CONFIGURATION ADD CONSTRAINT CK_BOX_CONFIGURATION_status CHECK (configuration_status IN ('DRAFT','READY'));
ALTER TABLE dbo.BOX_CONFIGURATION_ITEM ADD CONSTRAINT CK_BOX_CONFIGURATION_ITEM_qty CHECK (quantity_per_box > 0);
ALTER TABLE dbo.CART_ITEM ADD CONSTRAINT CK_CART_ITEM_target CHECK ((variant_id IS NOT NULL AND configuration_id IS NULL) OR (variant_id IS NULL AND configuration_id IS NOT NULL));
ALTER TABLE dbo.CART_ITEM ADD CONSTRAINT CK_CART_ITEM_qty CHECK (quantity > 0);
ALTER TABLE dbo.SALES_ORDER ADD CONSTRAINT CK_SALES_ORDER_kind CHECK ((order_type = 'RETAIL' AND business_id IS NULL) OR (order_type = 'BUSINESS' AND business_id IS NOT NULL));
ALTER TABLE dbo.SALES_ORDER ADD CONSTRAINT CK_SALES_ORDER_status CHECK (order_status IN ('PLACED','PROCESSING','SHIPPING','COMPLETED','CANCELLED'));
ALTER TABLE dbo.SALES_ORDER ADD CONSTRAINT CK_SALES_ORDER_totals CHECK (subtotal_amount >= 0 AND discount_amount >= 0 AND discount_amount <= subtotal_amount AND shipping_fee >= 0 AND total_amount = subtotal_amount - discount_amount + shipping_fee);
ALTER TABLE dbo.VOLUME_DISCOUNT_TIER ADD CONSTRAINT CK_VOLUME_DISCOUNT_TIER_group CHECK (discount_group IN ('GIFT_BOX','MOONCAKE'));
ALTER TABLE dbo.VOLUME_DISCOUNT_TIER ADD CONSTRAINT CK_VOLUME_DISCOUNT_TIER_bounds CHECK (min_quantity >= 1 AND (max_quantity IS NULL OR max_quantity >= min_quantity));
ALTER TABLE dbo.VOLUME_DISCOUNT_TIER ADD CONSTRAINT CK_VOLUME_DISCOUNT_TIER_percent CHECK (discount_percent >= 0 AND discount_percent <= 100);
ALTER TABLE dbo.VOLUME_DISCOUNT_TIER ADD CONSTRAINT CK_VOLUME_DISCOUNT_TIER_dates CHECK (valid_from IS NULL OR valid_to IS NULL OR valid_to > valid_from);
ALTER TABLE dbo.ORDER_ITEM ADD CONSTRAINT CK_ORDER_ITEM_target CHECK ((item_kind = 'STANDARD' AND variant_id IS NOT NULL AND configuration_id IS NULL AND inventory_source_snapshot = 'STOCKED_SKU') OR (item_kind = 'CUSTOM' AND variant_id IS NULL AND configuration_id IS NOT NULL AND inventory_source_snapshot = 'COMPONENTS'));
ALTER TABLE dbo.ORDER_ITEM ADD CONSTRAINT CK_ORDER_ITEM_qty CHECK (quantity > 0);
ALTER TABLE dbo.ORDER_ITEM ADD CONSTRAINT CK_ORDER_ITEM_group CHECK (discount_group_snapshot IS NULL OR discount_group_snapshot IN ('GIFT_BOX','MOONCAKE'));
ALTER TABLE dbo.ORDER_ITEM ADD CONSTRAINT CK_ORDER_ITEM_price CHECK (unit_base_price_snapshot >= 0 AND unit_discount_snapshot >= 0 AND unit_discount_snapshot <= unit_base_price_snapshot AND unit_final_price_snapshot = unit_base_price_snapshot - unit_discount_snapshot AND line_total = quantity * unit_final_price_snapshot AND discount_pct_snapshot >= 0 AND discount_pct_snapshot <= 100);
ALTER TABLE dbo.ORDER_ITEM_COMPONENT ADD CONSTRAINT CK_ORDER_ITEM_COMPONENT_role CHECK (component_role IN ('BOX','CAKE'));
ALTER TABLE dbo.ORDER_ITEM_COMPONENT ADD CONSTRAINT CK_ORDER_ITEM_COMPONENT_qty CHECK (quantity_per_set > 0);
ALTER TABLE dbo.ORDER_ITEM_COMPONENT ADD CONSTRAINT CK_ORDER_ITEM_COMPONENT_price CHECK (unit_price_snapshot >= 0);
ALTER TABLE dbo.PAYMENT ADD CONSTRAINT CK_PAYMENT_method CHECK (payment_method = 'COD');
ALTER TABLE dbo.PAYMENT ADD CONSTRAINT CK_PAYMENT_status CHECK ((payment_status = 'UNPAID' AND paid_at IS NULL) OR (payment_status = 'PAID' AND paid_at IS NOT NULL));
ALTER TABLE dbo.PAYMENT ADD CONSTRAINT CK_PAYMENT_amount CHECK (amount >= 0);
ALTER TABLE dbo.INVENTORY_BALANCE ADD CONSTRAINT CK_INVENTORY_BALANCE_nonnegative CHECK (available_quantity >= 0 AND low_stock_threshold >= 0 AND stock_version >= 0);
ALTER TABLE dbo.STOCK_MOVEMENT ADD CONSTRAINT CK_STOCK_MOVEMENT_kind CHECK (movement_type IN ('RECEIVE','SALE','CANCEL_RETURN','ADJUST','PACK'));
ALTER TABLE dbo.STOCK_MOVEMENT ADD CONSTRAINT CK_STOCK_MOVEMENT_delta CHECK (quantity_delta <> 0);
ALTER TABLE dbo.STOCK_MOVEMENT ADD CONSTRAINT CK_STOCK_MOVEMENT_direction CHECK ((movement_type = 'SALE' AND quantity_delta < 0) OR (movement_type IN ('RECEIVE','CANCEL_RETURN') AND quantity_delta > 0) OR (movement_type IN ('ADJUST','PACK') AND quantity_delta <> 0));
ALTER TABLE dbo.STOCK_MOVEMENT ADD CONSTRAINT CK_STOCK_MOVEMENT_orderref CHECK (movement_type NOT IN ('SALE','CANCEL_RETURN') OR order_id IS NOT NULL);
ALTER TABLE dbo.STOCK_MOVEMENT ADD CONSTRAINT CK_STOCK_MOVEMENT_idempotency CHECK (movement_type NOT IN ('SALE','CANCEL_RETURN','PACK') OR operation_key IS NOT NULL);
ALTER TABLE dbo.STOCK_MOVEMENT ADD CONSTRAINT CK_STOCK_MOVEMENT_pack_group CHECK (movement_type <> 'PACK' OR operation_group_key IS NOT NULL);

-- Unique constraints and filtered unique indexes
ALTER TABLE dbo.ACCOUNT ADD CONSTRAINT UQ_ACCOUNT_email UNIQUE (email);
ALTER TABLE dbo.BOX ADD CONSTRAINT UQ_BOX_empty_box_variant_id UNIQUE (empty_box_variant_id);
ALTER TABLE dbo.CATEGORY ADD CONSTRAINT UQ_CATEGORY_slug UNIQUE (slug);
ALTER TABLE dbo.PRODUCT ADD CONSTRAINT UQ_PRODUCT_slug UNIQUE (slug);
ALTER TABLE dbo.PRODUCT_VARIANT ADD CONSTRAINT UQ_PRODUCT_VARIANT_sku UNIQUE (sku);
ALTER TABLE dbo.SALES_ORDER ADD CONSTRAINT UQ_SALES_ORDER_order_code UNIQUE (order_code);
CREATE UNIQUE INDEX UX_CUSTOMER_account_id ON dbo.CUSTOMER (account_id) WHERE account_id IS NOT NULL;
CREATE UNIQUE INDEX UX_BUSINESS_tax_code ON dbo.BUSINESS (tax_code) WHERE tax_code IS NOT NULL;
CREATE UNIQUE INDEX UX_BOX_CONFIGURATION_preset_variant_id ON dbo.BOX_CONFIGURATION (preset_variant_id) WHERE preset_variant_id IS NOT NULL;
CREATE UNIQUE INDEX UX_PRODUCT_IMAGE_primary ON dbo.PRODUCT_IMAGE (variant_id) WHERE is_primary = 1;
CREATE UNIQUE INDEX UX_BOX_CONFIGURATION_ITEM_variant ON dbo.BOX_CONFIGURATION_ITEM (configuration_id, mooncake_variant_id);
-- Component snapshots for the same SKU are aggregated into one row per order item.
CREATE UNIQUE INDEX UX_ORDER_ITEM_COMPONENT_item_variant ON dbo.ORDER_ITEM_COMPONENT (order_item_id, component_variant_id);
CREATE UNIQUE INDEX UX_PAYMENT_one_paid_per_order ON dbo.PAYMENT (order_id) WHERE payment_status = 'PAID';
CREATE UNIQUE INDEX UX_STOCK_MOVEMENT_operation_key ON dbo.STOCK_MOVEMENT (operation_key) WHERE operation_key IS NOT NULL;
CREATE UNIQUE INDEX UX_VOLUME_DISCOUNT_TIER_active_group_min ON dbo.VOLUME_DISCOUNT_TIER (discount_group, min_quantity) WHERE is_active = 1;
CREATE UNIQUE INDEX UX_CART_active_customer ON dbo.CART (customer_id) WHERE cart_status = 'ACTIVE' AND customer_id IS NOT NULL;
CREATE UNIQUE INDEX UX_CART_active_guest ON dbo.CART (guest_token) WHERE cart_status = 'ACTIVE' AND guest_token IS NOT NULL;
-- One cart line per purchase target. Adding the same target again must update quantity.
CREATE UNIQUE INDEX UX_CART_ITEM_cart_variant ON dbo.CART_ITEM (cart_id, variant_id) WHERE variant_id IS NOT NULL;
CREATE UNIQUE INDEX UX_CART_ITEM_cart_configuration ON dbo.CART_ITEM (cart_id, configuration_id) WHERE configuration_id IS NOT NULL;

-- Helpful non-unique indexes (chosen for MVP access patterns).
CREATE INDEX IX_PRODUCT_category ON dbo.PRODUCT (category_id);
CREATE INDEX IX_PRODUCT_VARIANT_product ON dbo.PRODUCT_VARIANT (product_id);
CREATE INDEX IX_PRODUCT_IMAGE_variant ON dbo.PRODUCT_IMAGE (variant_id);
CREATE INDEX IX_CART_ITEM_cart ON dbo.CART_ITEM (cart_id);
CREATE INDEX IX_BOX_CONFIGURATION_cart ON dbo.BOX_CONFIGURATION (cart_id);
CREATE INDEX IX_BOX_CONFIGURATION_ITEM_config ON dbo.BOX_CONFIGURATION_ITEM (configuration_id);
CREATE INDEX IX_SALES_ORDER_customer_date ON dbo.SALES_ORDER (customer_id, created_at DESC);
CREATE INDEX IX_SALES_ORDER_status_date ON dbo.SALES_ORDER (order_status, created_at DESC);
CREATE INDEX IX_SALES_ORDER_business ON dbo.SALES_ORDER (business_id);
CREATE INDEX IX_ORDER_ITEM_order ON dbo.ORDER_ITEM (order_id);
CREATE INDEX IX_ORDER_ITEM_variant ON dbo.ORDER_ITEM (variant_id);
CREATE INDEX IX_ORDER_ITEM_COMPONENT_order_item ON dbo.ORDER_ITEM_COMPONENT (order_item_id);
CREATE INDEX IX_PAYMENT_order ON dbo.PAYMENT (order_id);
CREATE INDEX IX_STOCK_MOVEMENT_variant_date ON dbo.STOCK_MOVEMENT (variant_id, created_at DESC);
CREATE INDEX IX_STOCK_MOVEMENT_order ON dbo.STOCK_MOVEMENT (order_id);

-- IMPORTANT: Rules requiring JOINs across entities or concurrency checks stay in service transactions:
-- 1) Verify variant product_type and configuration PRESET/CUSTOM SKU compatibility.
-- 2) Ensure a finished custom box contains exactly BOX.capacity_cakes eligible MOONCAKE variants.
-- 3) At checkout aggregate stock demand across all cart lines, then perform atomic per-variant update:
--    UPDATE dbo.INVENTORY_BALANCE SET available_quantity = available_quantity - @need,
--      stock_version = stock_version + 1, updated_at = SYSUTCDATETIME()
--    WHERE variant_id = @variantId AND available_quantity >= @need;
--    Require @@ROWCOUNT = 1 for EACH affected SKU; commit with order/items/payments/movements.
-- 4) On valid pre-SHIPPING cancellation, restore inventory exactly once and write movements.
-- 5) PRESET GIFT_SET decrements ONLY finished-set SKU. CUSTOM decrements BOX + CAKE components.
-- 6) Discount groups aggregate quantities separately (GIFT_BOX vs loose MOONCAKE); exclude EMPTY_BOX.
-- 7) Compute money, shipment charges and COD state on server, store immutable order snapshots.
-- 8) PACK admin action: negative movements for loose components and positive for finished GIFT_SET, one transaction.
-- 9) PAYMENT row is COD+UNPAID on placement; PAID only after confirmed collected delivery.
-- 10) Do not delete historical rows; use is_active. New migrations expand PAYMENT for payOS/VietQR.
-- 11) A CART may be checked out successfully only once. As the first write in the checkout transaction,
--     atomically claim it with:
--       UPDATE dbo.CART SET cart_status = 'CHECKED_OUT', updated_at = SYSUTCDATETIME()
--       WHERE cart_id = @cartId AND cart_status = 'ACTIVE';
--     Require @@ROWCOUNT = 1. Create order/payment, deduct inventory and write movements in that SAME
--     transaction; any failure rolls back the cart claim. A zero row count must stop the repeated/concurrent
--     request before it creates an order or changes inventory.
-- 12) PRESET configurations become immutable once READY/stocked/sold. Do not UPDATE their BOX or component
--     composition; a changed recipe requires a new GIFT_SET SKU and a new PRESET configuration. Enforce this
--     through the Spring Boot Admin/Service layer rather than a database trigger.
