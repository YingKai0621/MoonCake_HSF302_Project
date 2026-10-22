# MoonCake MVP — Team Work Plan

**Version:** 1.4

**Date:** 21/10/2026

**Status:** Approved current ownership after Foundation; shared cancellation ownership clarified
**Scope:** Four-member HSF302 team, Spring Boot MVC + Thymeleaf SSR, SQL Server, COD-only MVP

## 1. Purpose

This document defines current feature ownership, collaboration boundaries, integration order, and the Git workflow. It prevents team members or their AI assistants from implementing the same responsibility differently.

Read it together with:

1. `business-rules.md` for approved behavior.
2. `architecture-conventions.md` for code structure.
3. `foundation-plan.md` for the shared implementation already merged.
4. `MoonCake_Physical_ERD_Design_Notes.md`, the approved Physical SQL, and all applied Flyway migrations for database structure.

Ownership means responsibility for design, implementation, tests, and review. It does not authorize a person to change shared entities, migrations, or another flow's public contract without agreement.

## 2. Current delivery model

The project currently has two primary ownership streams:

- **Customer Sales owner:** implements the complete customer-facing selling flow with Codex support.
- **Admin owner:** implements Admin pages and Admin-only operations.

The remaining team members may support UI, testing, or review through an explicitly assigned PR. They must not create parallel controllers, services, repositories, or security configuration for an already-owned use case.

Develop vertically by feature: persistence, service, MVC controller, Thymeleaf view, and tests. Reuse existing Thymeleaf/CSS/JS screens and replace demo data incrementally. Keep PRs small enough to review and integrate the end-to-end customer path before secondary Admin polish.

## 3. Customer Sales ownership

### Primary scope

- Public catalog, product search/filter, product detail, variants, images, and PRESET recipe display.
- Customer registration, login, logout, session security, and current-customer resolution.
- The application's single `SecurityConfig` and shared `PasswordEncoder`.
- Authenticated customer cart and cart-line operations.
- CUSTOM gift-box creation/editing, exact-capacity validation, and ownership checks.
- Server-side pricing and volume discounts.
- COD checkout, immutable order/payment/component snapshots, and atomic sale inventory deduction.
- Customer order history/detail and self-cancellation while an owned order is `PLACED`.
- Customer-facing controllers, forms/DTOs, templates, authorization checks, and tests for those flows.

### Authentication behavior

- Visitors may browse catalog and product pages without signing in.
- Add-to-cart, buy-now, CUSTOM-box, cart, checkout, and customer-order operations require an ACTIVE account with role `CUSTOMER`.
- Customer registration uses normalized email as the login name and creates `ACCOUNT` plus linked `CUSTOMER` in one transaction; role/status are decided by the backend, never by form input.
- An unauthenticated purchase action redirects to login with a validated internal return URL. It does not mutate data or automatically replay a POST.
- The schema's guest fields remain for applied-migration compatibility but are not used by the current application.

### Database and service area

- Catalog reads over `CATEGORY`, `PRODUCT`, `PRODUCT_VARIANT`, `PRODUCT_IMAGE`, `BOX`, and READY PRESET configurations.
- `ACCOUNT` and `CUSTOMER` for customer registration/authentication.
- `CART`, `CART_ITEM`, CUSTOM `BOX_CONFIGURATION`, and `BOX_CONFIGURATION_ITEM`.
- `VOLUME_DISCOUNT_TIER`.
- `SALES_ORDER`, `ORDER_ITEM`, `ORDER_ITEM_COMPONENT`, and COD `PAYMENT` for checkout and customer reads.
- Sale-side operations over `INVENTORY_BALANCE` and `STOCK_MOVEMENT`, through Inventory service contracts.

### Public contracts owned

- `ProductQueryService`
- `PresetConfigurationQueryService`
- `AuthenticationService`
- `CurrentCustomerService`
- `CartService`
- `CustomBoxService`
- `PricingService`
- `CheckoutService`
- Customer-facing methods of `OrderQueryService`
- Shared `OrderCancellationService` and its single transaction implementation
- Sale-deduction methods of `InventoryService`

### Required behavior

- Add the same cart target by increasing quantity, never inserting a duplicate line.
- Serialize cart/CUSTOM mutations on the owning ACTIVE cart row.
- Derive ownership from the authenticated CUSTOMER principal.
- Claim `CART` from `ACTIVE` to `CHECKED_OUT` before creating an order.
- Create the order, COD `UNPAID` payment, inventory deductions, and SALE movements in one transaction.
- PRESET sales deduct finished GIFT_SET stock; CUSTOM sales deduct the empty box and cake components.
- Customer self-cancellation verifies ownership from the authenticated principal, accepts only `PLACED`, and delegates state change plus exact stock restoration to the shared cancellation transaction.

## 4. Admin ownership

### Primary scope

- `/admin/**` controllers, forms/DTOs, templates, navigation, and authorization tests.
- Category, product, variant, image, and empty-box management.
- PRESET recipe creation while DRAFT and activation to READY.
- Inventory receiving, manual adjustment, low-stock views, movement history, and PACK operations.
- Admin order views and valid processing, shipping, cancellation, and delivery/COD-completion actions. Admin cancellation reuses the shared cancellation service.
- One-time initial-Admin bootstrap from environment variables.

### Database and service area

- Admin mutations over catalog and PRESET tables.
- `INVENTORY_BALANCE` and `STOCK_MOVEMENT` for receive, adjust, PACK, and valid restoration operations.
- Admin state transitions over `SALES_ORDER` and `PAYMENT` through order service contracts.
- `ACCOUNT` only for Admin bootstrap/status checks; customer registration belongs to Customer Sales.

### Public contracts owned

- `ProductAdminService`
- `PresetConfigurationAdminService`
- `InventoryAdminService` or clearly Admin-scoped methods of `InventoryService`
- `PackingService`
- Admin-facing methods of `OrderQueryService`
- `OrderManagementService` for Admin-owned processing, shipping, and delivery/COD-completion transitions
- `AdminBootstrapService`

### Required behavior

- A READY, stocked, or historically referenced PRESET composition is immutable; changed contents require a new SKU/configuration.
- Receive/adjust/PACK operations update balance and movement history atomically.
- Admin may cancel `PLACED` or `PROCESSING` through the shared `OrderCancellationService`; it must not implement a second restoration path.
- Delivery changes `SHIPPING -> COMPLETED` and COD `UNPAID -> PAID` atomically.
- Admin code uses the shared `SecurityConfig`, `PasswordEncoder`, and account-loading mechanism. It must not create another filter chain or authentication service.

## 5. Shared boundaries and file ownership

- Customer Sales owns security baseline files. Admin may request a route/role change but coordinates before editing them.
- Product query and Product Admin implementations use separate service methods. Admin mutations must not leak into public query controllers.
- Customer Sales owns checkout orchestration. Admin state transitions call order/inventory contracts and do not reproduce checkout logic.
- Sale inventory deduction and Admin inventory operations may share repositories, but each operation has one service owner and one transaction coordinator. Any public `InventoryService` signature change requires both owners to review it.
- Customer Sales owns customer order history/detail and the shared `OrderCancellationService` transaction implementation. The service exposes separate intent-specific Customer/Admin methods, derives Customer ownership from server-side identity, performs one conditional transition, and restores inventory once; it does not accept a role/permission flag from form input.
- Admin owns Admin order views and the remaining order-management transitions. Its cancellation controller calls the shared `OrderCancellationService`; it must not copy cancellation queries, transaction logic, or inventory restoration.
- Shared repository queries should be added through the owning service, not copied into another module. Any change to the shared cancellation contract requires both Customer Sales and Admin owners to review it.
- Product owns PRESET repositories; Cart owns CUSTOM configuration repositories. Do not create a general repository that freely mutates both types.
- Controllers call services, never another module's repository.
- Cross-module contracts exchange IDs and immutable DTOs, not managed entities.

## 6. Foundation and database change control

Foundation-owned artifacts are:

- All 19 JPA entities and database enums.
- Cross-module relationship mappings.
- Global exception/error conventions.
- Shared public contracts created before feature work.
- Spring/Flyway/database and SQL Server integration-test configuration.

Changing a shared entity, enum, contract, or configuration after Foundation requires review from every affected owner.

Applied Flyway V1/V2/V3/V4/V5/V6/V7 files are immutable. A genuine schema change requires a written Business Rules/ERD comparison, team approval, and a new migration version. Never let Hibernate create or update schema.

The authenticated-customer decision does not require a migration: new carts use `customer_id`, leave `guest_token` null, and new retail orders have `customer_id` at the application boundary.

## 7. Implementation order

### Phase 0 — Documentation and shared-contract alignment

- Approve Business Rules v1.4 and this ownership plan.
- Narrow `ShopperIdentity`/`CurrentCustomerService` to authenticated customers.
- Confirm no applied migration was changed.

### Phase 1 — Customer authentication baseline

- Add Spring Security dependency and the single `SecurityConfig`.
- Implement registration, login/logout, ACTIVE CUSTOMER resolution, CSRF-safe SSR forms, and safe post-login return behavior.
- Test public catalog access, protected purchase routes, duplicate email, disabled account, and cross-customer access.

### Phase 2 — Customer selling flow

- Integrate database-backed catalog.
- Implement cart and CUSTOM gift boxes.
- Implement pricing, COD checkout, inventory sale deduction, customer order history/detail, and Customer self-cancellation for owned `PLACED` orders.
- Test duplicate additions, insufficient stock, repeated/concurrent checkout, cancellation ownership/state/idempotency, and transaction rollback.

### Phase 3 — Admin flow

- Implement catalog/PRESET management, inventory receive/adjust/PACK, Admin order transitions, cancellation through the shared service, COD completion, and Admin bootstrap.
- Integrate against the already-merged security and service contracts.

### Phase 4 — Integration and polish

- Test authorization, concurrency, idempotency, state transitions, SQL Server constraints, and critical SSR journeys.
- Remove remaining demo data only after its database-backed replacement works.
- Finish UI consistency without mixing unrelated refactors into business PRs.

## 8. Git and PR workflow

Suggested short-lived branches are:

- `feature/customer-auth`
- `feature/customer-cart-custom-box`
- `feature/customer-checkout-orders`
- `feature/customer-order-cancellation`
- `feature/admin-catalog-preset`
- `feature/admin-inventory-orders`

Every PR must state:

- Scope and owner.
- Files and behavior changed.
- Tests executed and results.
- Database impact (`none` or migration version).
- Public contract changes and affected owners.
- Remaining assumptions or follow-up work.

Regularly update from `main`, coordinate before editing a shared contract, and do not combine an unrelated refactor with a feature PR.

## 9. Definition of Done

A feature is complete only when:

- It follows Business Rules and the approved schema.
- Controller, service, repository, DTO/form/view model, and Thymeleaf behavior are integrated as needed.
- Server-side validation, ownership checks, and route/service authorization are present.
- Critical behavior has automated tests proportional to risk.
- No credentials, demo secrets, or unrelated schema changes are included.
- The owner tested the relevant page or transaction against the correct environment.
- Documentation is updated if a public contract or team agreement changed.

## 10. Conflict resolution

When requirements, schema, or module contracts conflict:

1. Stop the affected implementation.
2. Record the conflicting sources and affected owners.
3. Prefer confirmed Business Rules for behavior and approved Flyway/Physical DDL for structure.
4. Get agreement before modifying a shared contract or adding a migration.
5. Document the decision so human members and AI assistants receive the same instruction.

## 11. Admin implementation checkpoint — 22/10/2026

Admin implementation is now present in the working tree: catalog/PRESET management, inventory receive/adjust/PACK, order management with cancellation and COD completion, shared-security authorization, and one-time Admin bootstrap. Ownership and existing shared contracts are unchanged.

See [admin-flow.md](admin-flow.md) for the source-context review, operator workflow, implementation boundaries, exact verification commands and remaining acceptance checks. Unit/MVC verification is complete at this checkpoint; SQL Server integration and the manual browser journey against real data remain pending. Phase 3 must not be reported as fully accepted until those Definition-of-Done checks pass.
