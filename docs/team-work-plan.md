# MoonCake MVP — Team Work Plan

**Version:** 1.2

**Date:** 20/10/2026

**Status:** Updated baseline; ready for team review before the Foundation PR
**Scope:** Four-member HSF302 team, Spring Boot MVC + Thymeleaf SSR, SQL Server, COD-only MVP

## 1. Purpose

This document defines feature ownership, collaboration boundaries, integration order, and the Git workflow. It prevents two members or their AI assistants from implementing the same responsibility differently.

Read it together with:

1. `business-rules.md` for approved behavior.
2. `architecture-conventions.md` for code structure.
3. `foundation-plan.md` for the shared starting implementation.
4. `MoonCake_Physical_ERD_Design_Notes.md`, the approved Physical SQL, and Flyway V1/V2 for database structure.

Module ownership means responsibility for design, implementation, tests, and review. It does not authorize a member to change shared entities, migrations, or another module's public contract without agreement.

## 2. Shared delivery model

- Develop vertically by feature: persistence, service, MVC controller, Thymeleaf view, and tests.
- Reuse the existing Thymeleaf/CSS/JS screens; replace demo/hardcoded data incrementally.
- Do not wait for all frontend work to finish before backend work starts.
- Merge the Foundation PR before parallel feature implementation.
- Prioritize the end-to-end customer flow: browse product -> cart -> COD checkout -> inventory deduction.
- Complete secondary Admin screens and UI polish after the critical flow works.

## 3. Member 1 — Product, Catalog, and PRESET Definition

### Primary scope

- Customer product catalog, search/filter, product detail, SKU display, and product images.
- Admin CRUD for categories, products, variants, images, and empty-box definitions.
- Define PRESET gift-box recipes before they become READY.
- Enforce the rule that a READY/stocked/referenced PRESET cannot have its composition changed.

### Primary database area

- `CATEGORY`
- `PRODUCT`
- `PRODUCT_VARIANT`
- `PRODUCT_IMAGE`
- `BOX`
- PRESET behavior over shared `BOX_CONFIGURATION` and `BOX_CONFIGURATION_ITEM`

### Expected output

- Customer catalog reads real SQL Server data instead of `PageController` hardcoded lists.
- Admin can manage the approved catalog fields.
- A changed PRESET recipe produces a new GIFT_SET SKU/configuration rather than rewriting historical composition.

### Public contracts owned

- `ProductQueryService`
- `ProductAdminService`
- `PresetConfigurationQueryService`
- `PresetConfigurationAdminService`

### Persistence ownership

- Own catalog repositories plus the narrowly scoped `PresetConfigurationRepository` and `PresetConfigurationItemRepository`.
- The PRESET repositories may read/write only PRESET configurations through Product services; they must not mutate CUSTOM configurations.
- SKU creation/activation calls the public Inventory contract to initialize or verify `INVENTORY_BALANCE`; Product code must not write the Inventory repository directly.

## 4. Member 2 — Cart, CUSTOM Gift Box, and Pricing

### Primary scope

- Guest/customer cart lifecycle and cart-line operations.
- Add the same target by increasing quantity, never inserting a duplicate line.
- Create and edit CUSTOM box configurations owned by an ACTIVE cart.
- Validate exact box capacity before READY/checkout.
- Calculate provisional cart prices and volume-discount groups.

### Primary database area

- `CART`
- `CART_ITEM`
- CUSTOM behavior over `BOX_CONFIGURATION`
- CUSTOM behavior over `BOX_CONFIGURATION_ITEM`
- `VOLUME_DISCOUNT_TIER`

### Expected output

- Customers can add variants, build a CUSTOM gift box, update quantities, and see server-calculated provisional totals.
- Cart and CUSTOM configuration ownership is protected.
- Pricing follows separate `GIFT_BOX` and `MOONCAKE` quantity groups.

### Public contracts owned

- `CartService`
- `CustomBoxService`
- `PricingService`

### Persistence ownership

- Own `CartRepository`, `CartItemRepository`, `CustomBoxConfigurationRepository`, and `CustomBoxConfigurationItemRepository`.
- The CUSTOM repositories may read/write only configurations owned by an ACTIVE cart; they must not mutate PRESET configurations.
- All cart/configuration mutations serialize on the owning cart row so they cannot race checkout.

## 5. Member 3 — Checkout, Order, and COD Payment

### Primary scope

- Coordinate the complete checkout transaction.
- Create immutable order, order-item, and component snapshots.
- Create COD payment as `UNPAID` at checkout.
- Implement order history/detail, Admin order processing, valid state transitions, cancellation, and delivery/COD collection.
- Keep cancellation inventory restoration and delivery/payment completion atomic.

### Primary database area

- `SALES_ORDER`
- `ORDER_ITEM`
- `ORDER_ITEM_COMPONENT`
- `PAYMENT`
- `BUSINESS`

### Expected output

- Exactly one successful order can be created from an ACTIVE cart.
- Concurrent/repeated checkout cannot create duplicate orders or deduct stock twice.
- Order, pricing, recipient, component, and payment snapshots remain stable after catalog changes.

### Public contracts owned

- `CheckoutService`
- `OrderQueryService`
- `OrderManagementService`

### Transaction coordinator rule

Member 3 owns the checkout and cancellation orchestration. The implementation calls Member 2 pricing/cart contracts and Member 4 inventory contracts. It must not reproduce their repository logic inside the order module.

## 6. Member 4 — Inventory, PACK, Identity, and Security

### Primary scope

- Inventory balances, receiving, adjustment, low-stock views, and movement history.
- Atomic stock deduction/restoration operations used by checkout and cancellation.
- PACK operation: consume loose box/cake components and increase finished GIFT_SET stock in one transaction.
- Account/customer authentication, authorization, current-customer resolution, and Admin route protection.
- MVP authentication is limited to login/logout for existing accounts and role protection. Customer self-registration, password reset, social login, MFA, and account verification are outside scope until separately approved.
- Secure backend-generated guest identity cookie and ownership checks for guest carts/configurations.
- One-time initial Admin bootstrap from environment variables; never seed or hardcode an Admin password.

### Primary database area

- `INVENTORY_BALANCE`
- `STOCK_MOVEMENT`
- `ACCOUNT`
- `CUSTOMER`

### Expected output

- The application prevents overselling with conditional database updates.
- SALE, CANCEL_RETURN, and PACK movement keys remain idempotent.
- Admin/customer access is protected without preventing guest checkout.
- Guest tokens are unguessable, server-managed, absent from URLs/logs/local storage, and never accepted as caller-selected ownership proof.
- The first Admin can be created through the disabled-by-default `bootstrap-admin` profile without storing credentials in Git or Flyway.

### Public contracts owned

- `InventoryService`
- `PackingService`
- `AuthenticationService`
- `CurrentCustomerService`

### Security ownership boundary

- Foundation creates only `ShopperIdentity`, the `CurrentCustomerService` interface, and shared authorization/error types.
- Member 4 exclusively adds the Spring Security dependency and owns `SecurityConfig`, `UserDetailsService`, `PasswordEncoder`, `AuthenticationService` implementation, `CurrentCustomerService` implementation, login/logout controller/view, guest-cookie handling, and Admin bootstrap.
- Other members do not create alternate security configuration or authentication services. Member 4's initial security PR must merge before Admin feature controllers rely on route protection.

## 7. Shared ownership and change control

### Foundation-owned artifacts

- All 19 JPA entities and database enums.
- Cross-module relationship mappings.
- Global exception/error conventions.
- Public service contracts agreed before parallel work.
- Spring/Flyway/database configuration.
- The dedicated SQL Server integration-test profile and its non-production database contract.
- Shared security identity contracts only; concrete Spring Security configuration is explicitly excluded from Foundation ownership.

Changing these after Foundation merge requires review from every affected module owner.

### Database changes

- Flyway V1/V2 are immutable after sharing.
- A schema change requires a written reason, comparison with Business Rules/ERD, team approval, and a new migration version.
- Only one active PR should introduce a particular migration version.
- Never let Hibernate generate or update schema.

### Legacy `PageController`

- Do not let all members edit it independently.
- Member 1 replaces product routes first; other routes move to feature controllers through small PRs.
- Delete hardcoded demo data only after the corresponding database-backed page works.

## 8. Cross-module dependency rules

- Product contracts are consumed by Cart, Checkout, and Inventory/PACK.
- Inventory contracts are consumed by Product Admin when a managed-stock SKU is created or activated.
- Cart and Pricing contracts are consumed by Checkout.
- Inventory contracts are consumed by Checkout/Order and Admin inventory screens.
- Current-customer/security contracts are consumed by all web modules.
- Order contracts may be consumed by Admin views, but Inventory must not change order state itself.
- A module may call another module's service interface; it may not call that module's repository.
- Cross-module contracts exchange IDs and DTOs, not mutable JPA entities.

## 9. Implementation phases

### Phase 0 — Documentation baseline

- Approve Business Rules, this work plan, architecture conventions, and Foundation plan.
- Confirm the already-merged Flyway/database baseline, then merge the reviewed documentation and AI guidance.

### Phase 1 — Foundation

- Implement the package skeleton, enums, all entities, mapping tests, shared errors, and agreed service contracts.
- Merge and tag a stable starting point for all feature branches.

### Phase 2 — Core feature development

- Members implement their vertical feature slices in parallel.
- Use agreed interfaces or small fakes while a dependency is not merged.
- Avoid creating an alternative interface privately inside a feature branch.

### Phase 3 — Integration

- Integrate Product -> Cart -> Checkout -> Inventory first.
- Then integrate cancellation/refund-to-stock, PACK, authentication, and Admin workflows.

### Phase 4 — Testing and polish

- Test concurrency, idempotency, state transitions, authorization, validation, and critical SSR flows.
- Remove remaining demo data and finish UI consistency.

## 10. Git and PR workflow

Suggested short-lived branch prefixes are:

- `feature/product-catalog`
- `feature/cart-custom-box`
- `feature/checkout-orders`
- `feature/inventory-admin-auth`

Branches are ownership labels, not permission to accumulate a very large PR. Each member should open small reviewable PRs, regularly update from `main`, and integrate early.

Every PR must state:

- Scope and module owner.
- Files and behavior changed.
- Tests executed and their result.
- Database impact (`none` or migration version).
- Public contract changes and affected members.
- Remaining assumptions or follow-up work.

## 11. Definition of Done for a feature

A feature is complete only when:

- It follows Business Rules and the approved schema.
- Controller, service, repository, DTO/form/view model, and Thymeleaf behavior are integrated as needed.
- Server-side validation and authorization are present.
- Critical business behavior has automated tests proportional to risk.
- No credentials, demo secrets, or unrelated schema changes are included.
- The owner has tested the relevant page or transaction against SQL Server.
- Documentation is updated if a public contract or team agreement changed.

## 12. Conflict resolution

When requirements, schema, or module contracts conflict:

1. Stop the affected implementation.
2. Record the conflicting sources and affected modules.
3. Prefer confirmed Business Rules for behavior and approved Flyway/Physical DDL for structure.
4. Get agreement before modifying a shared contract or adding a migration.
5. Document the decision so human members and AI assistants receive the same instruction.

