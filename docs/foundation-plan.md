# MoonCake MVP — Foundation Implementation Plan

**Version:** 1.2

**Date:** 20/10/2026
**Status:** Updated plan; execute only after the team approves the documentation baseline

## 1. Foundation objective

Foundation creates one compileable, reviewed starting point for four parallel feature streams. It establishes shared domain mappings and contracts without implementing full Product, Cart, Checkout, Inventory, Admin, or Security features.

Foundation must not change the approved MVP, add tables, add demo business data, or rewrite Flyway V1/V2.

## 2. Preconditions

Before Foundation begins:

- `business-rules.md` is approved.
- Flyway V1/V2 is merged and runs successfully on an empty `MoonCakeDB`.
- `team-work-plan.md` and `architecture-conventions.md` are approved.
- Every team member knows their feature ownership.
- The team agrees that shared entities/contracts require cross-module review.

## 3. Foundation deliverables

Foundation includes:

- Feature-first package skeleton where real files require it.
- Database enums with exact SQL values.
- All 19 JPA entities mapped to the existing schema.
- Reviewed relationship ownership and fetch choices.
- Shared exception/error conventions.
- Initial public service contracts and boundary DTOs needed for parallel development.
- Shared `ShopperIdentity` and `CurrentCustomerService` contracts only. Spring Security dependency/configuration and login/logout implementation belong to Member 4, not Foundation.
- A separate SQL Server integration-test profile targeting `MoonCakeTestDB`, never the development `MoonCakeDB`.
- Safe `TEST_DB_*` placeholders in `.env.example`; no real test credentials are committed.
- Tests proving the application context and mappings work with the Flyway-managed schema.
- Removal of no existing UI behavior unless its database-backed replacement is ready.

Feature repositories, business service implementations, Spring Security configuration, controllers, and pages remain in later feature PRs. Foundation may use `EntityManager` in mapping smoke tests, but it does not create general feature repositories merely to test entity mappings.

## 4. Step 1 — Freeze names and package ownership

Create/confirm these top-level packages:

- `common`
- `config`
- `domain.entity`
- `domain.enums`
- `product`
- `cart`
- `order`
- `inventory`
- `security`

Record any requested deviation in `architecture-conventions.md` before creating competing structures.

Before creating `domain.entity.Product`, rename the existing demo `model.Product` to `CatalogProductView` and update only its imports/usages. This is a behavior-preserving collision cleanup, not the database-backed catalog implementation.

## 5. Step 2 — Create exact database enums

Use these exact class names and persisted strings from Flyway constraints:

- `ProductType`: `MOONCAKE`, `EMPTY_BOX`, `GIFT_SET`
- `AccountRole`: `ADMIN`, `CUSTOMER`
- `AccountStatus`: `ACTIVE`, `DISABLED`
- `CartStatus`: `ACTIVE`, `CHECKED_OUT`, `ABANDONED`
- `ConfigurationType`: `PRESET`, `CUSTOM`
- `ConfigurationStatus`: `DRAFT`, `READY`
- `OrderType`: `RETAIL`, `BUSINESS`
- `OrderStatus`: `PLACED`, `PROCESSING`, `SHIPPING`, `COMPLETED`, `CANCELLED`
- `DiscountGroup`: `GIFT_BOX`, `MOONCAKE`
- `OrderItemKind`: `STANDARD`, `CUSTOM`
- `InventorySource`: `STOCKED_SKU`, `COMPONENTS`
- `ComponentRole`: `BOX`, `CAKE`
- `PaymentMethod`: `COD`
- `PaymentStatus`: `UNPAID`, `PAID`
- `StockMovementType`: `RECEIVE`, `SALE`, `CANCEL_RETURN`, `ADJUST`, `PACK`

Do not add convenience enum values that the database cannot store.

## 6. Step 3 — Map all 19 entities

Create exactly one entity for each table:

### Catalog and gift box

- `Category`
- `Product`
- `ProductVariant`
- `ProductImage`
- `Box`
- `BoxConfiguration`
- `BoxConfigurationItem`

### Identity, customer, and cart

- `Account`
- `Customer`
- `Cart`
- `CartItem`

### Order and pricing

- `Business`
- `SalesOrder`
- `VolumeDiscountTier`
- `OrderItem`
- `OrderItemComponent`
- `Payment`

### Inventory

- `InventoryBalance`
- `StockMovement`

Mapping rules:

- Match table/column names, nullability, length, precision, and identity behavior in V1.
- Use `BigDecimal` for all price/amount/percentage fields.
- Use `LocalDateTime` for every `DATETIME2(3)` column and interpret it as UTC through the shared UTC `Clock`.
- Map enums as strings and never rely on ordinal values.
- Treat historical/order snapshots as normal stored columns, not computed catalog views.
- Use lazy associations by default and avoid broad cascading deletes.
- Preserve the `InventoryBalance` shared PK/FK to `ProductVariant` without inventing a new identity column.
- Map nullable target relationships carefully for CART_ITEM and ORDER_ITEM XOR constraints.
- Review the composite same-cart FK between CART_ITEM and BOX_CONFIGURATION so JPA does not create duplicate writable column mappings.
- Do not add a universal base entity if it implies columns that some tables do not have.

## 7. Step 4 — Review entity boundaries together

Before merge, the team performs one mapping review focused on:

- Whether an association is actually needed in both directions.
- Lazy-loading behavior in Thymeleaf/service mappings.
- FK ownership and nullable relationships.
- Cascade and orphan-removal safety.
- Equality/string methods avoiding recursive graphs.
- PRESET/CUSTOM and STANDARD/CUSTOM XOR representation.
- Historical rows remaining reachable without destructive deletes.

No member creates an alternative entity for the same table in a feature package.

## 8. Step 5 — Define shared service contracts

Agree on responsibilities and boundary data before implementation. Foundation may create minimal interfaces/DTOs for:

- Product lookup by active SKU and product type.
- PRESET recipe lookup without mutation access.
- Cart lookup/claim and cart summary.
- Checkout pricing result containing group totals, per-line snapshots, discount, shipping, and total.
- Inventory demand and atomic deduction/restoration result.
- Current customer or guest identity resolution.

The exact Foundation contract names and ownership are:

- `product.service.ProductQueryService`: returns immutable sellable-variant snapshots by variant ID.
- `product.service.PresetConfigurationQueryService`: returns an immutable READY PRESET recipe by GIFT_SET variant ID.
- `cart.service.CartService`: returns an owned ACTIVE cart snapshot and conditionally claims it for checkout.
- `cart.service.PricingService`: returns an immutable checkout quote containing per-line and grouped totals.
- `inventory.service.InventoryService`: initializes/verifies balances and performs atomic sale deduction/cancellation restoration from aggregated demand commands.
- `security.service.CurrentCustomerService`: resolves an immutable `ShopperIdentity` containing exactly one of customer ID or guest token.

Foundation creates the `CurrentCustomerService` interface and `ShopperIdentity` only. Member 4 later provides their implementation together with Spring Security; Foundation must not create `SecurityConfig`, `UserDetailsService`, `AuthenticationServiceImpl`, login/logout MVC, or a `PasswordEncoder` bean.

Freeze these cross-module method signatures before parallel branches start:

```java
// product.service.ProductQueryService
SellableVariantSnapshot requireSellableVariant(long variantId);

// product.service.PresetConfigurationQueryService
PresetRecipeSnapshot requireReadyPreset(long giftSetVariantId);

// cart.service.CartService
CartSnapshot requireActiveCart(long cartId, ShopperIdentity shopper);
CheckoutCartSnapshot claimForCheckout(long cartId, ShopperIdentity shopper);

// cart.service.PricingService
CheckoutQuote calculateCheckoutQuote(CheckoutCartSnapshot cart);

// inventory.service.InventoryService
void initializeBalance(long variantId);
void requireBalance(long variantId);
void deductForSale(SaleInventoryCommand command);
void restoreForCancellation(CancellationInventoryCommand command);

// security.service.CurrentCustomerService
ShopperIdentity resolveCurrentShopper();
```

`claimForCheckout` performs the conditional cart state update and returns the immutable checkout input captured in the same transaction. Inventory commands contain an operation/order identifier and aggregated demand by variant; they never accept duplicate variant rows. Foundation defines only the DTO fields required by these responsibilities. Feature-only mutation methods stay in their owning feature PR. Any signature change must first update this list and identify affected consumers.

Required boundary data is intentionally minimal:

- `ShopperIdentity`: exactly one of `customerId` or `guestToken`.
- `SellableVariantSnapshot`: variant/product IDs, SKU, product type, display name, current unit price, and active state.
- `PresetRecipeSnapshot`: configuration/GIFT_SET IDs, box variant ID, status, and immutable component variant/quantity entries.
- `CartSnapshot`: cart ID/owner plus immutable target and quantity entries for display/mutation responses.
- `CheckoutCartSnapshot`: claimed cart ID/owner plus immutable target and quantity entries used only by checkout.
- `CheckoutQuote`: per-line price/discount/component snapshots, group totals, subtotal, discount, shipping fee, and total.
- `SaleInventoryCommand` and `CancellationInventoryCommand`: order/operation identity plus one positive aggregated demand entry per variant.

Contract rules:

- Do not expose repositories.
- Do not return mutable managed entities across modules.
- Include enough information to avoid a consuming module reimplementing the provider's rules.
- Keep Foundation contracts minimal; add methods later through reviewed PRs when a real use case requires them.

## 9. Step 6 — Establish shared error semantics

Define a small common vocabulary for:

- Resource not found.
- Invalid business state or validation conflict.
- Access denied/ownership mismatch.
- Concurrent modification or already-processed operation.
- Insufficient stock.

Do not implement every final error page in Foundation. Establish types and expected MVC mapping so feature modules do not invent incompatible responses.

## 10. Step 7 — Foundation verification

Required checks before merge:

- Maven compile succeeds with Java 21 compatibility.
- Default unit tests run without SQL Server credentials.
- Rename the current `MoonCakeApplicationTests` to `MoonCakeApplicationIT`; run it only through the Maven `integration` profile/Failsafe with Spring profile `integration`.
- `application-integration.properties` loads the Spring Boot context against the dedicated `MoonCakeTestDB`, not `MoonCakeDB`.
- Integration-test configuration uses `TEST_DB_HOST`, `TEST_DB_PORT`, `TEST_DB_NAME`, `TEST_DB_USERNAME`, and `TEST_DB_PASSWORD`; `TEST_DB_NAME` defaults to `MoonCakeTestDB`.
- `.env.example` documents those `TEST_DB_*` names with placeholders only; `.env` remains local and ignored.
- A test safety guard verifies the connected database name is exactly `MoonCakeTestDB` before any mutating integration test runs.
- Flyway validates V1/V2 and reports schema version 2 with no pending migration.
- Default application configuration remains `spring.jpa.hibernate.ddl-auto=none`; the integration profile uses `spring.jpa.hibernate.ddl-auto=validate` after Flyway migration and never creates/updates schema.
- Every entity maps to exactly one approved table.
- Enum values match CHECK constraints.
- A minimal persistence smoke test can read seeded volume tiers without adding permanent demo data.
- No Entity, Repository, Service, or Controller duplicates an existing class/name.
- `.env` remains ignored and no secret appears in the diff.
- The Maven Wrapper version command is verified on a teammate machine or its reproducible failure is resolved in a separate tooling commit.

## 11. Foundation PR structure

Keep one coherent Foundation PR but organize it into reviewable commits:

1. Behavior-preserving legacy `Product` view-model rename plus package skeleton.
2. Exact enums and shared UTC clock.
3. Catalog and identity entities.
4. Cart/configuration entities and repository ownership boundaries.
5. Order/payment entities.
6. Inventory entities.
7. Shared contracts/errors, including `ShopperIdentity` and the unimplemented `CurrentCustomerService` contract.
8. Isolated SQL Server integration profile, mapping/context tests, and documentation corrections.

Do not combine Foundation with feature repositories, service implementations, Admin CRUD, Spring Security configuration, authentication screens, cart UI, or checkout implementation.

## 12. Foundation Definition of Done

Foundation is complete when:

- All members reviewed the domain mappings that affect them.
- The project compiles and tests pass against the Flyway-managed database.
- There are exactly 19 entity mappings and no schema generation.
- Shared contract ownership is recorded in `team-work-plan.md`.
- No unresolved schema/business contradiction remains hidden in code.
- `main` is stable enough for the four feature branches to update and begin parallel work.

After Foundation is merged, changes to entities, enums, shared contracts, or migrations are treated as integration changes and require affected-owner review.

