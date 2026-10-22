# Admin flow — implementation and verification

Date: 22/10/2026. Status: implemented in the working tree; SQL Server acceptance remains pending.

## Context reviewed

The implementation was checked against the current approved sources: Business Rules 1.4, architecture conventions 1.5, team ownership 1.4, Foundation 1.4, Physical SQL, Physical ERD notes, and the Conceptual, Logical V5 and Physical draw.io diagrams. All applied Flyway migrations V1–V7 were also inspected.

The Conceptual diagram retains the earlier `WHOLESALE_TIER` name and exploratory payment wording. Its own note identifies it as a proposal. The approved Business Rules and Logical/Physical models establish `VOLUME_DISCOUNT_TIER` and COD-only behavior; this implementation follows those current sources. Foundation's version-2 verification is historical: V3–V7 are already present and remain unchanged.

The existing application already implements customer authentication, database catalog queries, customer cart/CUSTOM boxes, pricing, atomic COD checkout, inventory sale/restoration contracts, and customer order history. Before this change there were no Admin controllers, pages, catalog mutation services, packing orchestration, order transition service, or bootstrap command. Existing unit/MVC tests passed before the Admin work.

## Implemented flows

| Area | Entry point | Behavior |
|---|---|---|
| Navigation | `/admin` | Opens order management; shared storefront header links Admin accounts to this area |
| Catalog | `/admin/catalog` | Create/edit category, product, SKU, empty-box specification and image; deactivate sellable data; remove an image |
| PRESET | `/admin/presets` | Create DRAFT, edit box/components, validate exact capacity, activate READY |
| Inventory | `/admin/inventory` | Balance/low-stock view, receive, signed adjustment, PACK, paged movement history |
| Orders | `/admin/orders` | Status filter, paged list, immutable detail snapshots, processing/shipping/cancellation/delivery actions |
| Bootstrap | `bootstrap-admin` profile | Create the first ACTIVE ADMIN from environment variables, then close the application |

### First-time setup

1. Configure the normal `DB_*` environment variables for the intended developer database. Flyway owns the schema.
2. Set `BOOTSTRAP_ADMIN_EMAIL` and `BOOTSTRAP_ADMIN_PASSWORD` in the local process environment or the local `MoonCake/.env` imported by Spring. Run with `MoonCake/` as the working directory so the `.env` import resolves. Do not commit credentials or include passwords in command arguments.
3. From `MoonCake/`, run:

   ```powershell
   .\mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=bootstrap-admin'
   ```

4. The profile uses a temporary local server port and exits after bootstrap. Any existing ADMIN (including DISABLED) causes a no-op; it never resets credentials or promotes a CUSTOMER. Email collision fails safely.
5. Remove the two bootstrap environment variables. Start normally with `.\mvnw.cmd spring-boot:run`, then visit `/admin` and sign in through the shared login page.

### Catalog and gift sets

1. Create a category and product, then its SKU. A balance is initialized through the existing `InventoryService` in the SKU transaction.
2. A GIFT_SET SKU starts inactive. Create an EMPTY_BOX SKU and its box specification if needed.
3. Create a DRAFT PRESET for the GIFT_SET; select cake SKUs and quantities. Setting a component to zero removes it from the draft.
4. Activate READY only when the total cake quantity equals box capacity. READY is permanent; stocked/referenced/active recipes cannot be edited.
5. Receive cake and empty-box stock, then PACK the desired quantity. Receive/adjust of a GIFT_SET also requires a READY recipe.
6. Open the GIFT_SET SKU for sale in Catalog. Public catalog/checkout continue using the existing queries and pricing rules.

Changing recipe contents requires a new GIFT_SET SKU and PRESET. Existing SKU codes/product associations cannot be reassigned. Product type cannot change after variants exist. A box already referenced by a configuration cannot change capacity or underlying SKU. Product/category/SKU/box deactivation retains historical data. Food information is entered by the operator.

### Order processing

```text
PLACED → PROCESSING → SHIPPING → COMPLETED + COD PAID
   └────────┴──────→ CANCELLED + stock restoration
```

Cancellation is allowed only before SHIPPING. Restoration aggregates the stored order inventory source: STANDARD/PRESET restores only its SKU; CUSTOM restores BOX/CAKE component snapshots multiplied by set quantity. It does not consult the current recipe. A conditional order update claims the transition before restoration. Delivery conditionally claims SHIPPING and updates the single existing COD payment from UNPAID to PAID, including `paid_at`, in one transaction. Wrong/missing payment causes rollback. Shipping/delivery never deduct stock again.

### Concurrency and authorization

- Existing shared `SecurityConfig` protects `/admin/**`; it and `PasswordEncoder` are reused.
- Every Admin service read/mutation checks the authenticated principal and current database account role/status, including previously logged-in accounts subsequently disabled.
- POST forms carry CSRF tokens. User inputs are validated in MVC and at catalog/inventory service boundaries.
- PRESET mutations lock the configuration row; catalog changes lock affected rows.
- Inventory changes use conditional nonnegative/overflow-safe SQL and transactional movement writes. PACK aggregates and processes SKUs in ascending ID order.
- Receive, adjustment and PACK forms carry operation UUIDs. SQL Server transaction-scoped `sp_getapplock` serializes the operation group; stored movement keys reject replay and protect the ledger. This also serializes two concurrent submissions that reuse the same UUID with different payloads.
- Bootstrap serializes the initial Admin check/creation with a transaction-scoped account-table lock.

## Ownership and schema impact

New code stays in `product`, `inventory`, `order`, `security` and shared validation/error packages. Controllers call services. Product mutations initialize balances through `InventoryService`; packing reads PRESET through `PresetConfigurationQueryService`; every cancellation entry point delegates to the shared `OrderCancellationService`, which performs the conditional transition and invokes `InventoryService.restoreForCancellation` in one transaction.

The shared `OrderCancellationService` contract was added to enforce the ownership rule already approved in the team plan. No entity, enum, security filter chain, applied migration, or 19-table schema was changed. Admin-only contracts remain confined to the Admin scope, and the shared header hides the customer cart control from Admin accounts.

Admin order reads and non-cancellation transitions use `OrderManagementService`; Admin cancellation calls the shared `OrderCancellationService`. No competing customer `OrderQueryService` was introduced. Volume-discount editing remains with the Customer Sales ownership established by the team plan.

## Verification

- `mvnw.cmd -v`: Maven wrapper works on JDK 21.
- `mvn test`: 106 unit/MVC tests passed after merging current `main` into the Admin branch. This includes focused tests for shared cancellation routing, conditional cancellation, snapshot-based restoration, service authorization, bootstrap, PRESET immutability/capacity, PACK aggregation/replay, order state transitions, real Thymeleaf rendering, form binding and CSRF/role checks.
- `mvnw.cmd -DskipTests test-compile`: compile gate for SQL Server integration tests.
- `AdminFlowIT` covers real SQL Server catalog/balance creation, DRAFT→READY immutability, PACK/operation locks, failed PACK rollback, concurrent COD completion, and completion rollback for missing payment. These tests are **not yet executed** because this workspace has no `TEST_DB_*` credentials or configured IDE database connection.

Run the database suite only after configuring the dedicated `MoonCakeTestDB`:

```powershell
.\mvnw.cmd -Pintegration verify
```

The suite inherits the existing database-name guard and additionally verifies `SELECT DB_NAME()` before its writes. It must never target `MoonCakeDB`. Mock tests do not establish SQL Server locking or rollback behavior. Database acceptance and a manual browser walkthrough with real data remain necessary before marking Phase 3 fully accepted under the team's Definition of Done.
