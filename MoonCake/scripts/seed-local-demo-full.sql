-- Local demo only. Run the WHOLE file in one SQL Server query console.
-- Select MoonCakeDB / MoonCakeTestDB, migrate through V4, register a CUSTOMER,
-- then replace the email below. No account/password is created by this script.
SET NOCOUNT ON;
SET XACT_ABORT ON;
DECLARE @CustomerEmail nvarchar(254) = N'togiabao06@gmail.com';
DECLARE @Now datetime2(3) = SYSUTCDATETIME();
DECLARE @Start datetime2(3) = DATEADD(day,-30,@Now);
IF DB_NAME() NOT IN (N'MoonCakeDB',N'MoonCakeTestDB')
    THROW 51100, 'Select the local MoonCakeDB or MoonCakeTestDB first.', 1;
IF @@TRANCOUNT <> 0
    THROW 51101, 'Run in auto-commit mode without an existing transaction.', 1;
IF NOT EXISTS (SELECT 1 FROM dbo.flyway_schema_history WHERE version='4' AND success=1)
    THROW 51102, 'Run Flyway through V4 first.', 1;
BEGIN TRY
    BEGIN TRANSACTION;
    DECLARE @LockResult int;
    EXEC @LockResult = sys.sp_getapplock @Resource=N'MoonCake:demo-seed-v1',
        @LockMode='Exclusive', @LockOwner='Transaction', @LockTimeout=10000;
    IF @LockResult < 0 THROW 51103, 'Could not lock demo seed.', 1;
    IF EXISTS (SELECT 1 FROM dbo.CATEGORY WHERE slug=N'demo-seed-v1')
    BEGIN
        COMMIT;
        PRINT 'Demo seed already exists. No data or inventory changed.';
        RETURN;
    END;
    DECLARE @CustomerId bigint;
    SELECT @CustomerId=c.customer_id FROM dbo.CUSTOMER c
    JOIN dbo.ACCOUNT a ON a.account_id=c.account_id
    WHERE a.email=LOWER(LTRIM(RTRIM(@CustomerEmail))) AND a.role='CUSTOMER' AND a.status='ACTIVE';
    IF @CustomerId IS NULL THROW 51104, 'Register an active CUSTOMER and set @CustomerEmail first.', 1;
    IF EXISTS (SELECT 1 FROM dbo.PRODUCT_VARIANT WHERE sku LIKE N'DEMO-%')
       OR EXISTS (SELECT 1 FROM dbo.SALES_ORDER WHERE order_code LIKE N'DEMO-%')
       OR EXISTS (SELECT 1 FROM dbo.PRODUCT WHERE slug LIKE N'demo-%')
       OR EXISTS (SELECT 1 FROM dbo.CATEGORY WHERE slug LIKE N'demo-%')
        THROW 51105, 'Reserved DEMO identifiers already exist. Inspect them; do not delete history.', 1;

    INSERT dbo.CATEGORY(slug,category_name) VALUES
        (N'demo-seed-v1',N'DEMO - Mooncakes'),(N'demo-boxes',N'DEMO - Empty boxes'),
        (N'demo-gifts',N'DEMO - Gift sets');
    DECLARE @Catalog table(n int PRIMARY KEY, sku nvarchar(80), source_sku nvarchar(80),
        kind varchar(20), title nvarchar(200), price decimal(18,0), capacity int,
        initial_stock int, active bit, variant_id bigint, box_id bigint, config_id bigint);
    INSERT @Catalog(n,sku,source_sku,kind,title,price,capacity,initial_stock,active) VALUES
        (1,N'DEMO-CAKE-01',N'MC-KSPM-080','MOONCAKE',NULL,65000,NULL,3000,1),
        (2,N'DEMO-CAKE-02',N'MC-PBMHA-080','MOONCAKE',NULL,68000,NULL,3000,1),
        (3,N'DEMO-CAKE-03',N'MC-CBTN-080','MOONCAKE',NULL,72000,NULL,3000,1),
        (4,N'DEMO-CAKE-04',N'MC-TCGQ-080','MOONCAKE',NULL,75000,NULL,3000,1),
        (5,N'DEMO-CAKE-05',N'MC-CBBB-080','MOONCAKE',NULL,82000,NULL,1500,1),
        (6,N'DEMO-CAKE-06',N'MC-LSTT-080','MOONCAKE',NULL,85000,NULL,1500,0),
        (7,N'DEMO-CAKE-07',N'MC-TXHN-120','MOONCAKE',NULL,95000,NULL,3,1),
        (8,N'DEMO-CAKE-08',N'MC-HSMC-120','MOONCAKE',NULL,99000,NULL,0,1),
        (9,N'DEMO-BOX-04',NULL,'EMPTY_BOX',N'DEMO - Box 4',45000,4,1000,1),
        (10,N'DEMO-BOX-06',NULL,'EMPTY_BOX',N'DEMO - Box 6',65000,6,1000,1),
        (11,N'DEMO-BOX-08',NULL,'EMPTY_BOX',N'DEMO - Box 8',85000,8,500,1),
        (12,N'DEMO-GIFT-04',NULL,'GIFT_SET',N'DEMO - Gift 4 READY',325000,4,0,1),
        (13,N'DEMO-GIFT-06',NULL,'GIFT_SET',N'DEMO - Gift 6 READY',485000,6,0,1),
        (14,N'DEMO-GIFT-08',NULL,'GIFT_SET',N'DEMO - Gift 8 DRAFT',650000,8,0,0);
    IF (SELECT COUNT(*) FROM @Catalog d JOIN dbo.PRODUCT_VARIANT v ON v.sku=d.source_sku) <> 8
        THROW 51106, 'Expected V3 source cakes are missing.', 1;
    DECLARE @n int=1, @ProductId bigint, @VariantId bigint, @BoxId bigint, @ConfigId bigint;
    WHILE @n<=14
    BEGIN
        INSERT dbo.PRODUCT(category_id,slug,product_type,product_name,short_description,
            detailed_description,main_ingredients,shelf_life_info,storage_guidance,is_active)
        SELECT cat.category_id,LOWER(d.sku),d.kind,COALESCE(d.title,N'DEMO - '+p.product_name),
            N'LOCAL DEMO: sample prices and illustrative image; not a commercial listing.',
            p.detailed_description,p.main_ingredients,p.shelf_life_info,p.storage_guidance,d.active
        FROM @Catalog d
        JOIN dbo.CATEGORY cat ON cat.slug=CASE d.kind WHEN 'MOONCAKE' THEN N'demo-seed-v1'
            WHEN 'EMPTY_BOX' THEN N'demo-boxes' ELSE N'demo-gifts' END
        LEFT JOIN dbo.PRODUCT_VARIANT src ON src.sku=d.source_sku
        LEFT JOIN dbo.PRODUCT p ON p.product_id=src.product_id WHERE d.n=@n;
        SET @ProductId=SCOPE_IDENTITY();
        INSERT dbo.PRODUCT_VARIANT(product_id,sku,variant_name,weight_g,filling_label,unit_price,is_active)
        SELECT @ProductId,d.sku,COALESCE(d.title,src.variant_name),src.weight_g,src.filling_label,d.price,d.active
        FROM @Catalog d LEFT JOIN dbo.PRODUCT_VARIANT src ON src.sku=d.source_sku WHERE d.n=@n;
        SET @VariantId=SCOPE_IDENTITY();
        UPDATE @Catalog SET variant_id=@VariantId WHERE n=@n;
        INSERT dbo.PRODUCT_IMAGE(variant_id,image_url,alt_text,is_primary)
        VALUES(@VariantId,N'/images/branding/mooncake_luxury_emblem_v4.svg',N'DEMO illustration',1);
        INSERT dbo.INVENTORY_BALANCE(variant_id,available_quantity,low_stock_threshold,stock_version,updated_at)
        SELECT @VariantId,initial_stock,10,CASE WHEN initial_stock>0 THEN 1 ELSE 0 END,@Start FROM @Catalog WHERE n=@n;
        INSERT dbo.STOCK_MOVEMENT(variant_id,movement_type,quantity_delta,reason,operation_key,created_at)
        SELECT @VariantId,'RECEIVE',initial_stock,N'DEMO initial receipt',CONCAT('DEMO:RECEIVE:',sku),@Start
        FROM @Catalog WHERE n=@n AND initial_stock>0;
        IF @n BETWEEN 9 AND 11
        BEGIN
            INSERT dbo.BOX(empty_box_variant_id,capacity_cakes,box_notes)
            SELECT @VariantId,capacity,N'LOCAL DEMO packaging' FROM @Catalog WHERE n=@n;
            SET @BoxId=SCOPE_IDENTITY();
            UPDATE @Catalog SET box_id=@BoxId WHERE n=@n;
        END;
        SET @n+=1;
    END;
    -- Two complete immutable recipes and one intentionally incomplete editable DRAFT.
    SET @n=12;
    WHILE @n<=14
    BEGIN
        SELECT @BoxId=box_id FROM @Catalog WHERE n=@n-3;
        INSERT dbo.BOX_CONFIGURATION(box_id,preset_variant_id,configuration_type,configuration_status,created_at,updated_at)
        SELECT @BoxId,variant_id,'PRESET',CASE WHEN @n=14 THEN 'DRAFT' ELSE 'READY' END,@Start,@Start
        FROM @Catalog WHERE n=@n;
        SET @ConfigId=SCOPE_IDENTITY();
        UPDATE @Catalog SET config_id=@ConfigId,box_id=@BoxId WHERE n=@n;
        INSERT dbo.BOX_CONFIGURATION_ITEM(configuration_id,mooncake_variant_id,quantity_per_box)
        SELECT @ConfigId,variant_id,CASE WHEN @n=13 AND n<=2 THEN 2 ELSE 1 END
        FROM @Catalog WHERE n BETWEEN 1 AND CASE WHEN @n=14 THEN 2 ELSE 4 END;
        SET @n+=1;
    END;
    DECLARE @Pack table(variant_id bigint,delta int,group_key nvarchar(150));
    INSERT @Pack SELECT variant_id,CASE n WHEN 12 THEN 160 ELSE 100 END,CONCAT('DEMO:PACK:',sku)
    FROM @Catalog WHERE n IN(12,13);
    INSERT @Pack SELECT b.empty_box_variant_id,-CASE d.n WHEN 12 THEN 160 ELSE 100 END,CONCAT('DEMO:PACK:',d.sku)
    FROM @Catalog d JOIN dbo.BOX b ON b.box_id=d.box_id WHERE d.n IN(12,13);
    INSERT @Pack SELECT i.mooncake_variant_id,-i.quantity_per_box*CASE d.n WHEN 12 THEN 160 ELSE 100 END,
        CONCAT('DEMO:PACK:',d.sku) FROM @Catalog d
    JOIN dbo.BOX_CONFIGURATION_ITEM i ON i.configuration_id=d.config_id WHERE d.n IN(12,13);
    INSERT dbo.STOCK_MOVEMENT(variant_id,movement_type,quantity_delta,reason,operation_key,operation_group_key,created_at)
    SELECT variant_id,'PACK',delta,N'DEMO pack finished gift sets',CONCAT(group_key,':',variant_id),group_key,
        DATEADD(day,1,@Start) FROM @Pack;
    UPDATE b SET available_quantity=b.available_quantity+p.delta,stock_version=stock_version+1,
        updated_at=DATEADD(day,1,@Start)
    FROM dbo.INVENTORY_BALANCE b JOIN (SELECT variant_id,SUM(delta) delta FROM @Pack GROUP BY variant_id)p ON p.variant_id=b.variant_id;

    INSERT dbo.BUSINESS(business_name,contact_name,contact_phone,contact_email) VALUES
        (N'DEMO Business A',N'Demo purchasing A','0000000000',N'purchasing-a@example.test'),
        (N'DEMO Business B',N'Demo purchasing B','0000000000',N'purchasing-b@example.test');
    DECLARE @BusinessB bigint=SCOPE_IDENTITY(), @BusinessA bigint;
    SELECT @BusinessA=business_id FROM dbo.BUSINESS WHERE contact_email=N'purchasing-a@example.test';
    DECLARE @Orders table(n int PRIMARY KEY,status varchar(16),business_id bigint,order_id bigint);
    INSERT @Orders(n,status,business_id) VALUES
        (1,'PLACED',NULL),(2,'PLACED',NULL),(3,'PLACED',@BusinessA),
        (4,'PROCESSING',NULL),(5,'PROCESSING',@BusinessB),(6,'PROCESSING',NULL),
        (7,'SHIPPING',NULL),(8,'SHIPPING',@BusinessA),
        (9,'COMPLETED',NULL),(10,'COMPLETED',@BusinessB),
        (11,'CANCELLED',NULL),(12,'CANCELLED',NULL);
    -- target 0 = CUSTOM box 4 with two cakes of each of flavors 1 and 2.
    DECLARE @Lines table(order_n int,target int,qty int);
    INSERT @Lines VALUES (1,1,4),(1,2,2),(1,9,1),(2,12,2),(2,0,1),
        (3,12,50),(3,1,50),(4,0,3),(4,3,4),(5,0,100),(5,2,100),
        (6,13,3),(6,11,2),(7,12,4),(7,5,6),(8,13,50),
        (9,0,2),(9,4,4),(10,12,50),(10,1,100),(11,12,2),(11,1,5),(12,0,4);
    DECLARE @OrderId bigint,@CartId bigint,@ItemId bigint,@Status varchar(16),@At datetime2(3),
        @Target int,@Qty int,@CustomId bigint,@RecipeId bigint,@Price decimal(18,0),@Group varchar(12),
        @TierId bigint,@Pct decimal(5,2),@Discount decimal(18,0),@GroupQty int,@Ship decimal(18,0);
    SET @n=1;
    WHILE @n<=12
    BEGIN
        SET @At=DATEADD(day,@n+3,@Start);
        SELECT @Status=status FROM @Orders WHERE n=@n;
        INSERT dbo.CART(customer_id,cart_status,created_at,updated_at) VALUES(@CustomerId,'CHECKED_OUT',@At,@At);
        SET @CartId=SCOPE_IDENTITY();
        SET @CustomId=NULL;
        IF EXISTS(SELECT 1 FROM @Lines WHERE order_n=@n AND target=0)
        BEGIN
            SELECT @BoxId=box_id FROM @Catalog WHERE n=9;
            INSERT dbo.BOX_CONFIGURATION(box_id,cart_id,configuration_type,configuration_status,created_at,updated_at)
            VALUES(@BoxId,@CartId,'CUSTOM','READY',@At,@At);
            SET @CustomId=SCOPE_IDENTITY();
            INSERT dbo.BOX_CONFIGURATION_ITEM(configuration_id,mooncake_variant_id,quantity_per_box)
            SELECT @CustomId,variant_id,2 FROM @Catalog WHERE n IN(1,2);
        END;
        SELECT @Ship=CASE WHEN SUM(CASE WHEN target=0 OR target IN(12,13) THEN qty ELSE 0 END)>=50
            THEN 150000 ELSE 30000 END FROM @Lines WHERE order_n=@n;
        INSERT dbo.SALES_ORDER(order_code,customer_id,business_id,order_type,buyer_name_snapshot,buyer_phone_snapshot,
            buyer_email_snapshot,recipient_name,recipient_phone,shipping_address_snapshot,order_note,
            subtotal_amount,discount_amount,shipping_fee,total_amount,order_status,created_at,updated_at)
        SELECT CONCAT('DEMO-ORDER-',RIGHT('00'+CAST(@n AS varchar(2)),2)),@CustomerId,business_id,
            CASE WHEN business_id IS NULL THEN 'RETAIL' ELSE 'BUSINESS' END,N'Demo buyer','0000000000',@CustomerEmail,
            CONCAT(N'Demo recipient ',@n),'0000000000',N'DEMO address - not for delivery',
            N'LOCAL DEMO. Synthetic order for Admin testing; do not dispatch.',0,0,@Ship,@Ship,@Status,@At,DATEADD(hour,2,@At)
        FROM @Orders WHERE n=@n;
        SET @OrderId=SCOPE_IDENTITY();
        UPDATE @Orders SET order_id=@OrderId WHERE n=@n;
        DECLARE line_cursor CURSOR LOCAL FAST_FORWARD FOR SELECT target,qty FROM @Lines WHERE order_n=@n ORDER BY target;
        OPEN line_cursor;
        FETCH NEXT FROM line_cursor INTO @Target,@Qty;
        WHILE @@FETCH_STATUS=0
        BEGIN
            SELECT @VariantId=NULL,@RecipeId=NULL,@BoxId=NULL,@Group=NULL,@TierId=NULL,@Pct=0;
            IF @Target=0
            BEGIN
                SET @RecipeId=@CustomId;
                SET @Group='GIFT_BOX';
                SELECT @Price=SUM(price*CASE WHEN n=9 THEN 1 ELSE 2 END) FROM @Catalog WHERE n IN(1,2,9);
            END
            ELSE
                SELECT @VariantId=variant_id,@RecipeId=config_id,@Price=price,
                    @Group=CASE kind WHEN 'MOONCAKE' THEN 'MOONCAKE' WHEN 'GIFT_SET' THEN 'GIFT_BOX' END
                FROM @Catalog WHERE n=@Target;
            SELECT @GroupQty=SUM(l.qty) FROM @Lines l LEFT JOIN @Catalog d ON d.n=l.target
            WHERE l.order_n=@n AND ((@Group='GIFT_BOX' AND (l.target=0 OR d.kind='GIFT_SET'))
                OR (@Group='MOONCAKE' AND d.kind='MOONCAKE'));
            SELECT TOP(1) @TierId=volume_discount_tier_id,@Pct=discount_percent
            FROM dbo.VOLUME_DISCOUNT_TIER WHERE is_active=1 AND discount_group=@Group
                AND min_quantity<=@GroupQty AND (max_quantity IS NULL OR max_quantity>=@GroupQty)
                AND (valid_from IS NULL OR valid_from<=@At) AND (valid_to IS NULL OR valid_to>@At)
            ORDER BY min_quantity DESC;
            SET @Discount=ROUND(@Price*@Pct/100,0);
            INSERT dbo.CART_ITEM(cart_id,variant_id,configuration_id,quantity,added_at)
            VALUES(@CartId,@VariantId,CASE WHEN @Target=0 THEN @CustomId END,@Qty,@At);
            INSERT dbo.ORDER_ITEM(order_id,variant_id,configuration_id,volume_discount_tier_id,item_kind,
                inventory_source_snapshot,product_name_snapshot,sku_snapshot,discount_group_snapshot,
                box_name_snapshot,box_sku_snapshot,quantity,unit_base_price_snapshot,discount_pct_snapshot,
                unit_discount_snapshot,unit_final_price_snapshot,line_total)
            SELECT @OrderId,@VariantId,CASE WHEN @Target=0 THEN @CustomId END,@TierId,
                CASE WHEN @Target=0 THEN 'CUSTOM' ELSE 'STANDARD' END,
                CASE WHEN @Target=0 THEN 'COMPONENTS' ELSE 'STOCKED_SKU' END,
                COALESCE(p.product_name,N'DEMO - Custom box 4'),v.sku,@Group,bp.product_name,bv.sku,
                @Qty,@Price,@Pct,@Discount,@Price-@Discount,@Qty*(@Price-@Discount)
            FROM (VALUES(1)) dummy(n)
            LEFT JOIN dbo.PRODUCT_VARIANT v ON v.variant_id=@VariantId
            LEFT JOIN dbo.PRODUCT p ON p.product_id=v.product_id
            LEFT JOIN dbo.BOX_CONFIGURATION cfg ON cfg.configuration_id=@RecipeId
            LEFT JOIN dbo.BOX b ON b.box_id=cfg.box_id
            LEFT JOIN dbo.PRODUCT_VARIANT bv ON bv.variant_id=b.empty_box_variant_id
            LEFT JOIN dbo.PRODUCT bp ON bp.product_id=bv.product_id;
            SET @ItemId=SCOPE_IDENTITY();
            IF @RecipeId IS NOT NULL
            BEGIN
                INSERT dbo.ORDER_ITEM_COMPONENT(order_item_id,component_variant_id,component_role,
                    component_name_snapshot,quantity_per_set,unit_price_snapshot)
                SELECT @ItemId,v.variant_id,'CAKE',p.product_name,i.quantity_per_box,v.unit_price
                FROM dbo.BOX_CONFIGURATION_ITEM i JOIN dbo.PRODUCT_VARIANT v ON v.variant_id=i.mooncake_variant_id
                JOIN dbo.PRODUCT p ON p.product_id=v.product_id WHERE i.configuration_id=@RecipeId
                UNION ALL
                SELECT @ItemId,v.variant_id,'BOX',p.product_name,1,v.unit_price
                FROM dbo.BOX_CONFIGURATION cfg JOIN dbo.BOX b ON b.box_id=cfg.box_id
                JOIN dbo.PRODUCT_VARIANT v ON v.variant_id=b.empty_box_variant_id
                JOIN dbo.PRODUCT p ON p.product_id=v.product_id WHERE cfg.configuration_id=@RecipeId;
            END;
            FETCH NEXT FROM line_cursor INTO @Target,@Qty;
        END;
        CLOSE line_cursor;
        DEALLOCATE line_cursor;
        UPDATE o SET subtotal_amount=t.subtotal,discount_amount=t.discount,
            total_amount=t.subtotal-t.discount+o.shipping_fee
        FROM dbo.SALES_ORDER o CROSS APPLY(SELECT SUM(quantity*unit_base_price_snapshot) subtotal,
            SUM(quantity*unit_discount_snapshot) discount FROM dbo.ORDER_ITEM WHERE order_id=@OrderId)t
        WHERE o.order_id=@OrderId;
        INSERT dbo.PAYMENT(order_id,payment_method,payment_status,amount,paid_at,created_at,updated_at)
        SELECT @OrderId,'COD',CASE WHEN @Status='COMPLETED' THEN 'PAID' ELSE 'UNPAID' END,total_amount,
            CASE WHEN @Status='COMPLETED' THEN DATEADD(hour,2,@At) END,@At,DATEADD(hour,2,@At)
        FROM dbo.SALES_ORDER WHERE order_id=@OrderId;
        INSERT dbo.STOCK_MOVEMENT(variant_id,order_id,movement_type,quantity_delta,reason,operation_key,created_at)
        SELECT variant_id,@OrderId,'SALE',-SUM(qty),N'DEMO checkout',CONCAT('SALE:',@OrderId,':',variant_id),@At
        FROM (
            SELECT variant_id,quantity qty FROM dbo.ORDER_ITEM WHERE order_id=@OrderId AND inventory_source_snapshot='STOCKED_SKU'
            UNION ALL
            SELECT c.component_variant_id,c.quantity_per_set*i.quantity FROM dbo.ORDER_ITEM i
            JOIN dbo.ORDER_ITEM_COMPONENT c ON c.order_item_id=i.order_item_id
            WHERE i.order_id=@OrderId AND i.inventory_source_snapshot='COMPONENTS'
        ) demands GROUP BY variant_id;
        UPDATE b SET available_quantity=b.available_quantity+m.quantity_delta,stock_version=stock_version+1,updated_at=@At
        FROM dbo.INVENTORY_BALANCE b JOIN dbo.STOCK_MOVEMENT m ON m.variant_id=b.variant_id
        WHERE m.order_id=@OrderId AND m.movement_type='SALE';
        IF @Status='CANCELLED'
        BEGIN
            INSERT dbo.STOCK_MOVEMENT(variant_id,order_id,movement_type,quantity_delta,reason,operation_key,created_at)
            SELECT variant_id,@OrderId,'CANCEL_RETURN',-quantity_delta,N'DEMO cancellation',
                CONCAT('CANCEL_RETURN:',@OrderId,':',variant_id),DATEADD(hour,2,@At)
            FROM dbo.STOCK_MOVEMENT WHERE order_id=@OrderId AND movement_type='SALE';
            UPDATE b SET available_quantity=b.available_quantity+m.quantity_delta,stock_version=stock_version+1,
                updated_at=DATEADD(hour,2,@At)
            FROM dbo.INVENTORY_BALANCE b JOIN dbo.STOCK_MOVEMENT m ON m.variant_id=b.variant_id
            WHERE m.order_id=@OrderId AND m.movement_type='CANCEL_RETURN';
        END;
        SET @n+=1;
    END;
    -- Abort the whole seed if financial, recipe or inventory invariants are broken.
    IF EXISTS(SELECT 1 FROM @Catalog d JOIN dbo.INVENTORY_BALANCE b ON b.variant_id=d.variant_id
        OUTER APPLY(SELECT COALESCE(SUM(quantity_delta),0) qty FROM dbo.STOCK_MOVEMENT WHERE variant_id=d.variant_id)m
        WHERE b.available_quantity<>m.qty)
        THROW 51107, 'Inventory ledger mismatch.', 1;
    IF EXISTS(SELECT 1 FROM @Orders d JOIN dbo.SALES_ORDER o ON o.order_id=d.order_id
        CROSS APPLY(SELECT SUM(line_total) total FROM dbo.ORDER_ITEM WHERE order_id=o.order_id)i
        JOIN dbo.PAYMENT p ON p.order_id=o.order_id WHERE o.total_amount<>i.total+o.shipping_fee OR p.amount<>o.total_amount)
        THROW 51108, 'Order totals mismatch.', 1;
    IF EXISTS(SELECT 1 FROM dbo.BOX_CONFIGURATION c JOIN dbo.BOX b ON b.box_id=c.box_id
        JOIN @Catalog d ON d.box_id=b.box_id AND d.kind='EMPTY_BOX'
        CROSS APPLY(SELECT SUM(quantity_per_box) qty FROM dbo.BOX_CONFIGURATION_ITEM WHERE configuration_id=c.configuration_id)i
        WHERE c.configuration_status='READY' AND (i.qty IS NULL OR i.qty<>b.capacity_cakes))
        THROW 51109, 'READY recipe capacity mismatch.', 1;
    COMMIT;
    SELECT v.sku,b.available_quantity,b.low_stock_threshold FROM @Catalog d
        JOIN dbo.PRODUCT_VARIANT v ON v.variant_id=d.variant_id
        JOIN dbo.INVENTORY_BALANCE b ON b.variant_id=d.variant_id ORDER BY d.n;
    SELECT o.order_code,o.order_status,o.subtotal_amount,o.discount_amount,o.shipping_fee,o.total_amount,p.payment_status
        FROM @Orders d JOIN dbo.SALES_ORDER o ON o.order_id=d.order_id
        JOIN dbo.PAYMENT p ON p.order_id=o.order_id ORDER BY d.n;
    PRINT 'Created 14 SKUs, 3 PRESET recipes, 2 businesses, 12 orders and their COD/inventory history.';
END TRY
BEGIN CATCH
    IF XACT_STATE()<>0 ROLLBACK;
    THROW;
END CATCH;
