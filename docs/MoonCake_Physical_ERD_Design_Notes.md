# MoonCake MVP — Physical ERD cho Microsoft SQL Server

**Cơ sở:** Business Rules v1.1 và Logical ERD V5 COD-only; Physical được chuyển đổi thành 19 bảng, 5 trang draw.io. Schema đã được triển khai bằng Flyway V1/V2 và kiểm thử trên Microsoft SQL Server 2019 Developer.

## 1. Phạm vi đã khóa

- 4 kiểu mua: bánh lẻ; hộp rỗng; hộp đóng sẵn có SKU tồn kho riêng; hộp tự chọn lấy bánh + hộp rỗng từ tồn thành phần.
- COD duy nhất trong MVP. Khách đặt tự động, không duyệt đơn, không có `reserved_quantity` hoặc hạn chờ chuyển khoản.
- Đặt đơn thành công là claim giỏ `ACTIVE → CHECKED_OUT`, kiểm tra + tạo đơn + trừ tồn trong 1 transaction; một giỏ chỉ checkout thành công **đúng một lần**. Hủy hợp lệ trước `SHIPPING` hoàn tồn **đúng một lần**.
- Chiết khấu theo **hai nhóm độc lập** (`GIFT_BOX`, `MOONCAKE`), ngưỡng 50/100: 5%/10%. Hộp rỗng không giảm.
- Phí vận chuyển demo: dưới 50 hộp quà là 30.000 VND; từ 50 hộp là 150.000 VND (có thể cấu hình ở service); giữ snapshot phí trên đơn.
- Sau này tích hợp payOS/VietQR bằng migration riêng, không cần tạo các bảng provider ngay trong MVP.

## 2. Quy ước Physical

- `BIGINT IDENTITY(1,1)` cho PK (riêng `INVENTORY_BALANCE.variant_id` dùng PK/FK trực tiếp).
- `NVARCHAR` cho tiếng Việt, `VARCHAR` cho trạng thái/điện thoại; `DATETIME2(3)` lưu UTC.
- Giá tiền VND sử dụng `DECIMAL(18,0)`; phần trăm giảm là `DECIMAL(5,2)`; không dùng `FLOAT` cho tiền.
- `NOT NULL`, `DEFAULT`, `CHECK`, `UNIQUE`, filtered index hỗ trợ toàn vẹn dữ liệu; `ON DELETE NO ACTION` để giữ lịch sử.
- `PK`, `FK`, `UQ`, `NN` trong draw.io: khóa chính, khóa ngoại, duy nhất, bắt buộc; `?` nghĩa là nullable; `DEF` nghĩa là có giá trị mặc định.

## 3. Danh sách 19 bảng và thuộc tính

### CATEGORY
| Cột | Kiểu SQL Server | Ghi chú |
|---|---|---|
| `category_id` | `BIGINT IDENTITY(1,1) NOT NULL` | PK |
| `slug` | `NVARCHAR(160) NOT NULL` | UQ |
| `category_name` | `NVARCHAR(160) NOT NULL` |  |
| `is_active` | `BIT NOT NULL DEFAULT (1)` |  |

### PRODUCT
| Cột | Kiểu SQL Server | Ghi chú |
|---|---|---|
| `product_id` | `BIGINT IDENTITY(1,1) NOT NULL` | PK |
| `category_id` | `BIGINT NOT NULL` | FK → CATEGORY |
| `slug` | `NVARCHAR(180) NOT NULL` | UQ |
| `product_type` | `VARCHAR(20) NOT NULL` |  |
| `product_name` | `NVARCHAR(220) NOT NULL` |  |
| `short_description` | `NVARCHAR(600) NOT NULL` |  |
| `detailed_description` | `NVARCHAR(MAX) NULL` |  |
| `main_ingredients` | `NVARCHAR(700) NULL` |  |
| `shelf_life_info` | `NVARCHAR(350) NULL` |  |
| `storage_guidance` | `NVARCHAR(600) NULL` |  |
| `is_active` | `BIT NOT NULL DEFAULT (1)` |  |

### PRODUCT_VARIANT
| Cột | Kiểu SQL Server | Ghi chú |
|---|---|---|
| `variant_id` | `BIGINT IDENTITY(1,1) NOT NULL` | PK |
| `product_id` | `BIGINT NOT NULL` | FK → PRODUCT |
| `sku` | `NVARCHAR(80) NOT NULL` | UQ |
| `variant_name` | `NVARCHAR(200) NOT NULL` |  |
| `weight_g` | `INT NULL` |  |
| `filling_label` | `NVARCHAR(180) NULL` |  |
| `unit_price` | `DECIMAL(18,0) NOT NULL` |  |
| `is_active` | `BIT NOT NULL DEFAULT (1)` |  |

### PRODUCT_IMAGE
| Cột | Kiểu SQL Server | Ghi chú |
|---|---|---|
| `image_id` | `BIGINT IDENTITY(1,1) NOT NULL` | PK |
| `variant_id` | `BIGINT NOT NULL` | FK → PRODUCT_VARIANT |
| `image_url` | `NVARCHAR(1000) NOT NULL` |  |
| `alt_text` | `NVARCHAR(250) NULL` |  |
| `sort_order` | `INT NOT NULL DEFAULT (0)` |  |
| `is_primary` | `BIT NOT NULL DEFAULT (0)` |  |

### BOX
| Cột | Kiểu SQL Server | Ghi chú |
|---|---|---|
| `box_id` | `BIGINT IDENTITY(1,1) NOT NULL` | PK |
| `empty_box_variant_id` | `BIGINT NOT NULL` | FK → PRODUCT_VARIANT, UQ |
| `capacity_cakes` | `INT NOT NULL` |  |
| `material` | `NVARCHAR(180) NULL` |  |
| `box_notes` | `NVARCHAR(500) NULL` |  |
| `is_active` | `BIT NOT NULL DEFAULT (1)` |  |

### ACCOUNT
| Cột | Kiểu SQL Server | Ghi chú |
|---|---|---|
| `account_id` | `BIGINT IDENTITY(1,1) NOT NULL` | PK |
| `email` | `NVARCHAR(254) NOT NULL` | UQ |
| `password_hash` | `NVARCHAR(255) NOT NULL` |  |
| `role` | `VARCHAR(16) NOT NULL DEFAULT ('CUSTOMER')` |  |
| `status` | `VARCHAR(16) NOT NULL DEFAULT ('ACTIVE')` |  |
| `created_at` | `DATETIME2(3) NOT NULL DEFAULT (SYSUTCDATETIME())` |  |

### CUSTOMER
| Cột | Kiểu SQL Server | Ghi chú |
|---|---|---|
| `customer_id` | `BIGINT IDENTITY(1,1) NOT NULL` | PK |
| `account_id` | `BIGINT NULL` | FK → ACCOUNT |
| `full_name` | `NVARCHAR(160) NOT NULL` |  |
| `phone` | `VARCHAR(20) NOT NULL` |  |
| `email` | `NVARCHAR(254) NULL` |  |
| `created_at` | `DATETIME2(3) NOT NULL DEFAULT (SYSUTCDATETIME())` |  |

### CART
| Cột | Kiểu SQL Server | Ghi chú |
|---|---|---|
| `cart_id` | `BIGINT IDENTITY(1,1) NOT NULL` | PK |
| `customer_id` | `BIGINT NULL` | FK → CUSTOMER |
| `guest_token` | `NVARCHAR(128) NULL` |  |
| `cart_status` | `VARCHAR(16) NOT NULL DEFAULT ('ACTIVE')` |  |
| `created_at` | `DATETIME2(3) NOT NULL DEFAULT (SYSUTCDATETIME())` |  |
| `updated_at` | `DATETIME2(3) NOT NULL DEFAULT (SYSUTCDATETIME())` |  |

### BOX_CONFIGURATION
| Cột | Kiểu SQL Server | Ghi chú |
|---|---|---|
| `configuration_id` | `BIGINT IDENTITY(1,1) NOT NULL` | PK |
| `box_id` | `BIGINT NOT NULL` | FK → BOX |
| `cart_id` | `BIGINT NULL` | FK → CART |
| `preset_variant_id` | `BIGINT NULL` | FK → PRODUCT_VARIANT |
| `configuration_type` | `VARCHAR(12) NOT NULL` |  |
| `configuration_status` | `VARCHAR(16) NOT NULL DEFAULT ('DRAFT')` |  |
| `created_at` | `DATETIME2(3) NOT NULL DEFAULT (SYSUTCDATETIME())` |  |
| `updated_at` | `DATETIME2(3) NOT NULL DEFAULT (SYSUTCDATETIME())` |  |

### BOX_CONFIGURATION_ITEM
| Cột | Kiểu SQL Server | Ghi chú |
|---|---|---|
| `configuration_item_id` | `BIGINT IDENTITY(1,1) NOT NULL` | PK |
| `configuration_id` | `BIGINT NOT NULL` | FK → BOX_CONFIGURATION |
| `mooncake_variant_id` | `BIGINT NOT NULL` | FK → PRODUCT_VARIANT |
| `quantity_per_box` | `INT NOT NULL` |  |

### CART_ITEM
| Cột | Kiểu SQL Server | Ghi chú |
|---|---|---|
| `cart_item_id` | `BIGINT IDENTITY(1,1) NOT NULL` | PK |
| `cart_id` | `BIGINT NOT NULL` | FK → CART |
| `variant_id` | `BIGINT NULL` | FK → PRODUCT_VARIANT |
| `configuration_id` | `BIGINT NULL` | FK → BOX_CONFIGURATION |
| `quantity` | `INT NOT NULL` |  |
| `added_at` | `DATETIME2(3) NOT NULL DEFAULT (SYSUTCDATETIME())` |  |

### BUSINESS
| Cột | Kiểu SQL Server | Ghi chú |
|---|---|---|
| `business_id` | `BIGINT IDENTITY(1,1) NOT NULL` | PK |
| `business_name` | `NVARCHAR(220) NOT NULL` |  |
| `tax_code` | `NVARCHAR(30) NULL` |  |
| `contact_name` | `NVARCHAR(160) NOT NULL` |  |
| `contact_phone` | `VARCHAR(20) NOT NULL` |  |
| `contact_email` | `NVARCHAR(254) NULL` |  |
| `is_active` | `BIT NOT NULL DEFAULT (1)` |  |

### SALES_ORDER
| Cột | Kiểu SQL Server | Ghi chú |
|---|---|---|
| `order_id` | `BIGINT IDENTITY(1,1) NOT NULL` | PK |
| `order_code` | `NVARCHAR(40) NOT NULL` | UQ |
| `customer_id` | `BIGINT NULL` | FK → CUSTOMER |
| `business_id` | `BIGINT NULL` | FK → BUSINESS |
| `order_type` | `VARCHAR(12) NOT NULL` |  |
| `buyer_name_snapshot` | `NVARCHAR(160) NOT NULL` |  |
| `buyer_phone_snapshot` | `VARCHAR(20) NOT NULL` |  |
| `buyer_email_snapshot` | `NVARCHAR(254) NULL` |  |
| `recipient_name` | `NVARCHAR(160) NOT NULL` |  |
| `recipient_phone` | `VARCHAR(20) NOT NULL` |  |
| `shipping_address_snapshot` | `NVARCHAR(500) NOT NULL` |  |
| `order_note` | `NVARCHAR(1000) NULL` |  |
| `subtotal_amount` | `DECIMAL(18,0) NOT NULL` |  |
| `discount_amount` | `DECIMAL(18,0) NOT NULL DEFAULT (0)` |  |
| `shipping_fee` | `DECIMAL(18,0) NOT NULL DEFAULT (0)` |  |
| `total_amount` | `DECIMAL(18,0) NOT NULL` |  |
| `order_status` | `VARCHAR(16) NOT NULL DEFAULT ('PLACED')` |  |
| `created_at` | `DATETIME2(3) NOT NULL DEFAULT (SYSUTCDATETIME())` |  |
| `updated_at` | `DATETIME2(3) NOT NULL DEFAULT (SYSUTCDATETIME())` |  |

### VOLUME_DISCOUNT_TIER
| Cột | Kiểu SQL Server | Ghi chú |
|---|---|---|
| `volume_discount_tier_id` | `BIGINT IDENTITY(1,1) NOT NULL` | PK |
| `discount_group` | `VARCHAR(12) NOT NULL` |  |
| `min_quantity` | `INT NOT NULL` |  |
| `max_quantity` | `INT NULL` |  |
| `discount_percent` | `DECIMAL(5,2) NOT NULL` |  |
| `valid_from` | `DATETIME2(3) NULL` |  |
| `valid_to` | `DATETIME2(3) NULL` |  |
| `is_active` | `BIT NOT NULL DEFAULT (1)` |  |

### ORDER_ITEM
| Cột | Kiểu SQL Server | Ghi chú |
|---|---|---|
| `order_item_id` | `BIGINT IDENTITY(1,1) NOT NULL` | PK |
| `order_id` | `BIGINT NOT NULL` | FK → SALES_ORDER |
| `variant_id` | `BIGINT NULL` | FK → PRODUCT_VARIANT |
| `configuration_id` | `BIGINT NULL` | FK → BOX_CONFIGURATION |
| `volume_discount_tier_id` | `BIGINT NULL` | FK → VOLUME_DISCOUNT_TIER |
| `item_kind` | `VARCHAR(12) NOT NULL` |  |
| `inventory_source_snapshot` | `VARCHAR(16) NOT NULL` |  |
| `product_name_snapshot` | `NVARCHAR(220) NOT NULL` |  |
| `sku_snapshot` | `NVARCHAR(80) NULL` |  |
| `discount_group_snapshot` | `VARCHAR(12) NULL` |  |
| `box_name_snapshot` | `NVARCHAR(220) NULL` |  |
| `box_sku_snapshot` | `NVARCHAR(80) NULL` |  |
| `quantity` | `INT NOT NULL` |  |
| `unit_base_price_snapshot` | `DECIMAL(18,0) NOT NULL` |  |
| `discount_pct_snapshot` | `DECIMAL(5,2) NOT NULL DEFAULT (0)` |  |
| `unit_discount_snapshot` | `DECIMAL(18,0) NOT NULL DEFAULT (0)` |  |
| `unit_final_price_snapshot` | `DECIMAL(18,0) NOT NULL` |  |
| `line_total` | `DECIMAL(18,0) NOT NULL` |  |

### ORDER_ITEM_COMPONENT
| Cột | Kiểu SQL Server | Ghi chú |
|---|---|---|
| `order_component_id` | `BIGINT IDENTITY(1,1) NOT NULL` | PK |
| `order_item_id` | `BIGINT NOT NULL` | FK → ORDER_ITEM |
| `component_variant_id` | `BIGINT NOT NULL` | FK → PRODUCT_VARIANT |
| `component_role` | `VARCHAR(8) NOT NULL` |  |
| `component_name_snapshot` | `NVARCHAR(220) NOT NULL` |  |
| `quantity_per_set` | `INT NOT NULL` |  |
| `unit_price_snapshot` | `DECIMAL(18,0) NOT NULL` |  |

### PAYMENT
| Cột | Kiểu SQL Server | Ghi chú |
|---|---|---|
| `payment_id` | `BIGINT IDENTITY(1,1) NOT NULL` | PK |
| `order_id` | `BIGINT NOT NULL` | FK → SALES_ORDER |
| `payment_method` | `VARCHAR(16) NOT NULL` |  |
| `payment_status` | `VARCHAR(16) NOT NULL DEFAULT ('UNPAID')` |  |
| `amount` | `DECIMAL(18,0) NOT NULL` |  |
| `paid_at` | `DATETIME2(3) NULL` |  |
| `created_at` | `DATETIME2(3) NOT NULL DEFAULT (SYSUTCDATETIME())` |  |
| `updated_at` | `DATETIME2(3) NOT NULL DEFAULT (SYSUTCDATETIME())` |  |

### INVENTORY_BALANCE
| Cột | Kiểu SQL Server | Ghi chú |
|---|---|---|
| `variant_id` | `BIGINT NOT NULL` | PK, FK → PRODUCT_VARIANT |
| `available_quantity` | `INT NOT NULL DEFAULT (0)` |  |
| `low_stock_threshold` | `INT NOT NULL DEFAULT (5)` |  |
| `stock_version` | `BIGINT NOT NULL DEFAULT (0)` |  |
| `updated_at` | `DATETIME2(3) NOT NULL DEFAULT (SYSUTCDATETIME())` |  |

### STOCK_MOVEMENT
| Cột | Kiểu SQL Server | Ghi chú |
|---|---|---|
| `movement_id` | `BIGINT IDENTITY(1,1) NOT NULL` | PK |
| `variant_id` | `BIGINT NOT NULL` | FK → PRODUCT_VARIANT |
| `order_id` | `BIGINT NULL` | FK → SALES_ORDER |
| `performed_by_account_id` | `BIGINT NULL` | FK → ACCOUNT |
| `movement_type` | `VARCHAR(20) NOT NULL` |  |
| `quantity_delta` | `INT NOT NULL` |  |
| `reason` | `NVARCHAR(500) NULL` |  |
| `operation_key` | `NVARCHAR(150) NULL` |  |
| `operation_group_key` | `NVARCHAR(150) NULL` |  |
| `created_at` | `DATETIME2(3) NOT NULL DEFAULT (SYSUTCDATETIME())` |  |

## 4. Các ràng buộc đặc biệt

- `CART_ITEM` và `ORDER_ITEM`: mỗi dòng mua chính xác 1 đích (SKU **hoặc** CUSTOM BOX); kiểm tra XOR ở `CHECK`.
- `CART_ITEM`: hai filtered unique index `(cart_id,variant_id)` và `(cart_id,configuration_id)` ngăn dòng trùng theo từng loại target. Thêm lại cùng target phải tăng `quantity` ở service, không `INSERT` dòng mới.
- `ORDER_ITEM_COMPONENT`: unique index `(order_item_id,component_variant_id)` bảo đảm các component cùng SKU trong một hộp được cộng gộp thành đúng một dòng snapshot.
- `BOX_CONFIGURATION`: PRESET có `preset_variant_id`, không có `cart_id`; CUSTOM ngược lại. `CART_ITEM` có khóa ngoại kép `(cart_id,configuration_id)` bảo vệ chủ sở hữu cấu hình.
- PRESET đại diện công thức của SKU GIFT_SET đã đóng sẵn. Sau khi PRESET `READY`/SKU được mở bán, có tồn hoặc có lịch sử, không sửa `box_id` hay `BOX_CONFIGURATION_ITEM`; công thức mới phải dùng SKU GIFT_SET và PRESET configuration mới. Quy tắc bất biến liên dòng này do Admin/Service bảo vệ, không thêm trigger vào MVP.
- Đủ sức chứa: `SUM(quantity_per_box) == BOX.capacity_cakes` phải kiểm tra ở service trong lúc cấu hình READY/checkout (SQL CHECK không thể kiểm tra tổng các dòng).
- `BOX.empty_box_variant_id` phải là SKU `EMPTY_BOX`; `preset_variant_id` phải là SKU `GIFT_SET`; item bánh phải là `MOONCAKE`. Kiểm tra ở service (cần JOIN).
- `STOCK_MOVEMENT.operation_key` duy nhất khi khác NULL; dùng khóa nghiệp vụ cho SALE và CANCEL_RETURN nhằm chống ghi nhận trùng. Mọi bước cập nhật số lượng và lịch sử chạy trong cùng transaction.
- Bán PRESET: chỉ trừ SKU thành phẩm. Bán CUSTOM: trừ SKU vỏ hộp và từng SKU bánh × số bộ. `ORDER_ITEM_COMPONENT` giữ snapshot lịch sử.
- Có filtered unique index để mỗi đơn chỉ tối đa một `PAYMENT` trạng thái PAID; chưa có trường payOS/webhook (chỉ thêm ở migration sau).
- Mọi giá/số lượng/chiết khấu phải tính ở backend; Checkout không tin tổng tiền frontend gửi.
- Quan hệ `PAYMENT` 1 đơn → 0..N lần giao dịch, dù MVP COD thông thường chỉ 1 bản ghi.
- Checkout một lần dùng `CART.cart_status` làm cổng đồng thời: request phải atomically đổi đúng một dòng `ACTIVE → CHECKED_OUT` trong cùng transaction tạo order/payment/movements và trừ tồn. `@@ROWCOUNT = 0` nghĩa là giỏ không còn hợp lệ để checkout; request phải dừng trước mọi tác động. Rollback transaction cũng rollback trạng thái giỏ.

## 5. Tham khảo transaction khi triển khai backend (không phải script chạy trực tiếp)

```sql
-- Thao tác claim giỏ là write đầu tiên của checkout transaction.
UPDATE dbo.CART
SET cart_status = 'CHECKED_OUT',
    updated_at = SYSUTCDATETIME()
WHERE cart_id = @cartId AND cart_status = 'ACTIVE';
-- REQUIRE: @@ROWCOUNT = 1; nếu bằng 0 → giỏ đã checkout/không còn ACTIVE, rollback/dừng.

UPDATE dbo.INVENTORY_BALANCE
SET available_quantity = available_quantity - @quantity,
    stock_version = stock_version + 1,
    updated_at = SYSUTCDATETIME()
WHERE variant_id = @variantId AND available_quantity >= @quantity;
-- REQUIRE: @@ROWCOUNT = 1; nếu thiếu tồn → rollback toàn bộ đặt hàng.
```

## 6. Những kiểm tra cần làm trước khi dùng SQL thật

1. Chạy trên SQL Server 2019+ **database trống**; kiểm thử FK/CHECK/index và nhập bộ dữ liệu test.
2. Kiểm thử đơn 55 hộp quà + 105 bánh lẻ: ngưỡng và giá snapshot chính xác.
3. Kiểm thử PRESET/CUSTOM: không bao giờ trừ tồn 2 lần cho một bộ quà.
4. Kiểm thử hai lượt đặt đồng thời vào SKU còn 1 sản phẩm: tối đa một đơn thành công.
5. Kiểm thử hủy trước SHIPPING hai lần: tồn chỉ hoàn một lần; hủy sau SHIPPING không hoàn tự động.
6. Kiểm thử trạng thái COD: checkout là UNPAID, chỉ ghi PAID khi đã thu tiền.
7. Kiểm thử thêm cùng SKU/CUSTOM config hai lần: unique index không cho dòng trùng; service tăng `quantity` đúng.
8. Kiểm thử hai request đồng thời checkout cùng một cart: đúng một request claim được cart, chỉ một order và một bộ stock movement được tạo.
9. Kiểm thử sửa PRESET đã READY/đã có tồn: service từ chối và yêu cầu SKU/config mới; recipe cũ không đổi.
10. Không sửa/delete `V1__initial_schema.sql` hoặc `V2__seed_volume_discount_tiers.sql` sau khi đã áp dụng/chia sẻ; thay đổi tiếp theo phải dùng migration version mới.

**Lưu ý:** DDL tham chiếu đã được đối chiếu với `V1__initial_schema.sql`; Flyway đã áp dụng V1/V2 thành công trên `MoonCakeDB` trống và lần chạy lại xác nhận schema ở version 2, không có migration pending.
