# MoonCake MVP — Architecture and Coding Conventions

**Version:** 1.6

**Date:** 08/11/2026

**Status:** Approved baseline; v1.6 adds the instructor's mandatory n-layer rules (graded) and the payment Strategy design for COD/VNPay
**Applies to:** Java, Spring Boot MVC, Thymeleaf, JPA, SQL Server, and Flyway code in `MoonCake/`

## 1. Architecture style

Use feature-first packages with shared database domain classes. This keeps each member's feature work together while ensuring every table mapping has one definition.

### Mandatory n-layer rules (graded by the instructor)

The course instructor grades the project 1-on-1 and deducts points when these rules are broken. They override any looser rule elsewhere in this document.

| Layer | Package | Rule |
|---|---|---|
| Controller | `feature.controller` | Receives the request, validates forms, calls **services only**, returns a view. Never injects a repository, `EntityManager`, gateway, or other infrastructure class. |
| Service | `feature.service` | **Always an interface** that declares the use cases. |
| Service implementation | `feature.service.impl` | `XxxServiceImpl implements XxxService`. Holds business logic and `@Transactional`. |
| Repository | `feature.repository` | Interface. Prefer Spring Data JPA (`extends JpaRepository`/`Repository`); put hand-written queries in `repository.impl` behind the interface. |
| DTO | `feature.dto` | Forms going in and view models going out. **Views receive DTOs only** — no entities, no `Object[]`, no `Map` rows. Prefer Java `record` for read-only views. |
| Entity | `entity` | Every table has a JPA entity. Do not reach a table only through native SQL. |

Payment methods follow the Strategy pattern in section 7 (Payment). Business code must not branch on `if (method == VNPAY)`.

```text
com.se201411.project.mooncake
├── common
│   ├── exception
│   ├── validation
│   └── util
├── config
├── entity
├── enums
├── product
│   ├── controller
│   ├── dto
│   ├── repository
│   └── service
│       └── impl
├── cart
│   ├── controller
│   ├── dto
│   ├── repository
│   └── service
│       └── impl
├── order
│   ├── controller
│   ├── dto
│   ├── repository
│   └── service
│       └── impl
├── inventory
│   ├── controller
│   ├── dto
│   ├── repository
│   └── service
│       └── impl
└── security
    ├── config
    ├── controller
    ├── dto
    ├── repository
    └── service
        └── impl
```

Create only packages that contain real code. Do not add empty DTO subpackages just to mirror the diagram.

## 2. Dependency direction

The normal direction is:

```text
Thymeleaf/Form -> Controller -> Service contract -> Service implementation -> Repository -> Entity/Database
```

Rules:

- Controllers never contain pricing, inventory, order-state, or authorization business logic.
- Controllers never call repositories directly.
- Repositories do not call services and do not implement business workflows.
- A feature may call another feature through its public service contract only.
- A feature must not inject another feature's repository.
- Shared `entity` and `enums` types never depend on controller, DTO, service, or repository packages.
- Avoid circular service dependencies; choose one orchestration owner instead.

## 3. Java and naming baseline

- Target Java 21 language/API compatibility, even if a developer machine has a newer JDK.
- Packages use lowercase singular feature names.
- Classes use descriptive names such as `ProductController`, `CartService`, `CheckoutServiceImpl`, and `OrderRepository`.
- Method names describe business intent, for example `claimActiveCart`, `calculateCheckoutPrice`, and `deductAvailableStock`.
- Do not introduce Lombok or another code-generation dependency without team approval.
- Prefer constructor injection; do not use field injection.
- Keep methods focused and extract domain calculations that require independent tests.
- Prefer straightforward, line-by-line code that every member can explain in a review. Use descriptive intermediate variables and named helper methods.
- Prefer ordinary `for`, `if`, and `switch` statements over Java Stream/lambda chains when the loop is easier to follow.
- In JavaScript, prefer named functions and ordinary loops over arrow functions and chained `map`/`filter`/`some` expressions. Keep unavoidable callbacks short.
- Avoid clever one-liners and nested ternary expressions for business behavior; readability for the student team takes priority over brevity.
- One statement per line. Never compress a Java class, Thymeleaf template, CSS, or JavaScript file into one or a few very long lines; format with the IDE before committing.
- Use `import` statements; do not write fully-qualified class names such as `com.se201411.project.mooncake.enums.PaymentMethod` inside code.
- Read configuration through a small `@ConfigurationProperties` class instead of calling `Environment.getProperty` throughout the code.
- Simple is not an excuse for incorrect: keep ownership checks, conditional state transitions, transactions, and tests.
- The former `com.se201411.project.mooncake.model.Product` is a legacy UI model, not a JPA entity. Keep its renamed form as `product.dto.CatalogProductView` while adding `entity.Product`, preserving existing rendered pages during the transition.

## 4. Entity and enum conventions

- All table mappings live under `entity`. The original 19 were created in the Foundation PR; tables added by V9–V12 (`VNPAY_ATTEMPT`, `FEEDBACK`, `PRODUCT_VIDEO`, `STOCK_BATCH`, `BATCH_ALLOCATION`) must also get entities.
- Database enums live under `enums` and use exact SQL values with `EnumType.STRING`.
- Use these exact enum class names: `ProductType`, `AccountRole`, `AccountStatus`, `CartStatus`, `ConfigurationType`, `ConfigurationStatus`, `OrderType`, `OrderStatus`, `DiscountGroup`, `OrderItemKind`, `InventorySource`, `ComponentRole`, `PaymentMethod`, `PaymentStatus`, and `StockMovementType`.
- IDs use `Long`; SQL `IDENTITY` keys use identity generation.
- VND monetary values use `BigDecimal` with zero fractional digits. Never use `float` or `double` for money.
- Map SQL `DATETIME2(3)` to `LocalDateTime`, interpreted strictly as UTC because `DATETIME2` stores no offset. Obtain current values through an injected UTC `Clock`; do not call the host machine's local time directly.
- Associations default to lazy loading unless an explicit query use case proves otherwise.
- Do not add broad cascade or orphan removal to historical/order relationships.
- Do not expose every relationship bidirectionally. Add the minimum navigation required by approved use cases.
- Avoid entity `toString`, equality, or serialization implementations that recursively traverse associations.
- Never add a field that is not present in Flyway/Physical DDL.
- Shared entity changes require review by all affected module owners.

The database retains final responsibility for PK, FK, unique, filtered unique, and CHECK constraints. Service validation should provide friendly errors but must not replace database protection.

## 5. DTO, form, and view-model conventions

- Web inputs use dedicated form/request classes with Bean Validation. Do not read many loose `@RequestParam` values for a form; bind one form DTO.
- Thymeleaf outputs always use DTOs/view models. Never pass an entity, `Object[]`, or raw query row to a template.
- Map entities to DTOs inside the service implementation (or a small mapper it owns), never in a controller.
- Do not bind complex JPA entities directly to create/update forms.
- Cross-module service contracts exchange IDs and immutable DTOs/results rather than managed entities.
- Name types by purpose, such as `CreateProductForm`, `ProductDetailView`, `CartSummary`, and `CheckoutCommand`.
- Keep a DTO in the feature that owns its meaning; do not create one global dumping-ground DTO package.

## 6. Service and implementation conventions

- Put public cross-module contracts in `feature.service`.
- Put implementations in `feature.service.impl`.
- Every service is an interface in `feature.service` with exactly one `XxxServiceImpl` in `feature.service.impl` (instructor rule). The only exceptions are Spring configuration classes and pure static helpers in `common.util`.
- Depend on the interface type in constructors, never on the implementation class.
- Put `@Transactional` on public service-implementation use cases, not on controllers.
- Use read-only transactions for multi-query read use cases when useful.
- Convert persistence/concurrency failures into meaningful application exceptions at the service boundary.

## 7. Required transaction boundaries

### Checkout

One `CheckoutService` transaction must:

1. Conditionally claim the cart from `ACTIVE` to `CHECKED_OUT` and require one affected row.
2. Revalidate SKU/configuration status and calculate server-side prices.
3. Create order, item, and component snapshots plus one `UNPAID` payment for the chosen method, then let that method's `PaymentProcessor` start payment (VNPay creates its attempt and redirect URL inside the same transaction).
4. Aggregate inventory demand and call `InventoryService` atomic deductions.
5. Write idempotent SALE movements.
6. Commit everything or roll everything back.

All called services participate in the same transaction with default `REQUIRED` propagation. Do not use `REQUIRES_NEW` for cart claim, pricing persistence, inventory, payment, or movement writes.

### Cart and CUSTOM configuration mutation

One mutation transaction must lock the owning CART row for update, verify `cart_status = ACTIVE` and ownership, then modify `CART_ITEM` or its CUSTOM configuration. Locking the cart row serializes add/update/delete with checkout. Under that lock, adding an existing target updates its quantity; inserting a new target remains protected by the filtered unique indexes.

If a uniqueness/concurrency exception still occurs, roll back the whole transaction and translate or retry from a new transaction; never continue using a transaction marked rollback-only.

### Cancellation

Use one shared `OrderCancellationService` transaction coordinator for every cancellation entry point; Customer and Admin controllers must not reproduce state-change or stock-restoration logic. Expose separate intent-specific Customer and Admin service methods instead of accepting a role or permission flag from a web form.

- Customer self-cancellation verifies ownership from the authenticated `CUSTOMER` principal and accepts only `PLACED -> CANCELLED`.
- Admin cancellation accepts `PLACED -> CANCELLED` or `PROCESSING -> CANCELLED`.
- The transaction conditionally changes the expected current state to `CANCELLED` and requires one affected row before restoring stock.
- It restores the original inventory source once and writes idempotent `CANCEL_RETURN` movements in the same transaction.
- Before the conditional transition, the coordinator calls the order's `PaymentProcessor.prepareCancellation`; VNPay may reject or reconcile through `querydr` there. Customers may cancel only `UNPAID` orders.
- A cancelled unpaid payment remains `UNPAID` with null `paid_at`; a cancelled paid VNPay payment stays `PAID` for manual refund. `SALES_ORDER.CANCELLED` records the cancellation outcome.
- Competing transitions such as `PLACED -> PROCESSING` and `PROCESSING -> SHIPPING` must also be conditional on the expected current state. Repeated or losing requests return an already-processed/conflict result without inventory effects.

### Customer delivery updates

- A Customer may update recipient name, recipient phone, shipping address, and order note only for an owned `PLACED` order.
- Resolve ownership from the authenticated `CUSTOMER` principal. Never accept a submitted customer/account ID as proof of ownership.
- The write must be a conditional update that includes the expected `PLACED` status and requires exactly one affected row so it cannot race successfully with Admin processing.
- Delivery updates must not rewrite buyer, item, price, discount, shipping-fee, total, payment, or inventory snapshots.

### Delivery and COD collection

One transaction conditionally changes `SHIPPING -> COMPLETED` and requires one affected row, then asks the order's `PaymentProcessor` to finish delivery: COD conditionally changes `UNPAID -> PAID` and writes `paid_at`; VNPay changes nothing because it is already `PAID`. Both updates commit or roll back together. Repeated confirmation must return already-processed/conflict without creating or rewriting payment data.

### Payment (Strategy pattern)

Each payment method is one class behind a shared interface, so checkout, cancellation, and order management never branch on the method. Adding a new gateway means adding one new implementation class.

```text
order.service
├── PaymentProcessor              (interface — one per payment method)
├── PaymentProcessorRegistry      (interface — finds the processor for a PaymentMethod)
└── impl
    ├── CodPaymentProcessor       implements PaymentProcessor
    ├── VnpayPaymentProcessor     implements PaymentProcessor
    └── PaymentProcessorRegistryImpl
```

`PaymentProcessor` declares the decisions that differ by method (names may be refined during implementation):

- `PaymentMethod method()` — which enum value it handles.
- `start(...)` — after checkout: COD returns no redirect; VNPay creates an attempt and returns the gateway URL.
- `prepareCancellation(...)` — COD always allows; VNPay applies the reconciliation rule from Business Rules 6.3.
- `canStartProcessing(...)` — COD always true; VNPay only when `PAID`.
- `completeDelivery(...)` — COD collects cash; VNPay does nothing.

Rules:

- The registry builds a `Map<PaymentMethod, PaymentProcessor>` from all Spring beans, so services ask `registry.get(payment.getMethod())`.
- VNPay HTTP/signing details stay in a gateway client used only by `VnpayPaymentProcessor` and the VNPay service implementation; controllers call services, not the gateway.
- IPN and `querydr` results lock the order row, then update the attempt and payment conditionally on `PENDING`/`UNPAID`, so duplicate callbacks change nothing.
- Payment processors run inside the caller's transaction (`REQUIRED`); they do not open their own.

### PACK

One packing transaction deducts loose box/cake components, increases the finished GIFT_SET SKU, and writes grouped PACK movements.

### PRESET definition

One PRESET mutation transaction locks the configuration row. Composition changes are allowed only while it is `DRAFT`, its GIFT_SET SKU has not been opened for sale, it has no finished stock, and it has no historical reference. Transition to `READY` validates box capacity and component product types; READY never transitions back to DRAFT. A changed READY recipe uses a new SKU/configuration.

Do not use asynchronous events for these MVP consistency boundaries.

## 8. Repository conventions

- Repositories belong to the feature that owns the aggregate/use case.
- Repositories are interfaces. Start with Spring Data derived queries for simple reads; return entities or projection DTOs, never `Object[]`.
- Use explicit JPQL/native queries for conditional updates, locking, aggregation, or performance-sensitive projections.
- Keep atomic inventory/cart claim updates in clearly named repository methods that return affected-row counts.
- Do not hide a complete business workflow inside a repository default method.
- Prevent N+1 query problems with deliberate fetch joins/entity graphs/projections, not global eager loading.
- Product owns `PresetConfigurationRepository`/`PresetConfigurationItemRepository`; Cart owns `CustomBoxConfigurationRepository`/`CustomBoxConfigurationItemRepository`. They may target the same shared entities but expose role-specific queries only.
- Do not create a general-purpose configuration repository or use one repository to mutate both PRESET and CUSTOM types. Prefer restricted Spring Data `Repository` interfaces/custom implementations over exposing broad CRUD methods for these shared tables.
- Product Admin must call `InventoryService` to initialize/verify stock balance; it must not inject an Inventory repository.

## 9. MVC and Thymeleaf conventions

- Customer/Admin pages use Spring MVC controllers that return view names and handle form POST/redirect flows.
- Do not create REST APIs for every screen unless a real JavaScript interaction requires one.
- Separate customer and Admin routes/controllers when authorization or layout differs.
- Use Post/Redirect/Get after successful form mutations.
- Customer self-cancellation is a CSRF-protected POST followed by redirect; never implement cancellation as a GET link.
- Display validation errors without trusting prices, roles, IDs, or ownership claims from hidden frontend fields.
- Keep reusable header/footer fragments and existing CSS/JS assets unless the feature requires a reviewed change.

## 10. Validation, errors, and security

- Bean Validation checks shape and required input; services enforce ownership and cross-table business rules.
- Use a small application exception hierarchy for not-found, validation/business conflict, authorization, concurrency, and insufficient-stock cases.
- Provide a shared MVC exception handler/error mapping rather than repeated try/catch blocks in controllers.
- Resolve the current `ACCOUNT` and `CUSTOMER` from the authenticated Spring Security principal. Never accept a submitted account ID, customer ID, cart ID, or configuration ID as proof of ownership.
- Catalog and product-detail pages remain public. Adding to cart, building a CUSTOM box, viewing or changing a cart, checkout, customer order history/detail, customer delivery updates, and customer self-cancellation require an authenticated ACTIVE account with role `CUSTOMER`.
- An unauthenticated purchase action redirects to `/login` with a validated local return URL. Do not mutate a cart and do not automatically replay the original POST after login; return to a safe GET page so the customer can confirm the action again.
- Protect Admin routes and service operations, not only Admin navigation links.
- Use one application `SecurityConfig`. The Customer Sales owner owns the Spring Security baseline, login/logout, customer registration, and `CurrentCustomerService`; the Admin owner consumes that configuration and must not create a second filter chain or authentication implementation without review.
- Public endpoints are limited to catalog/product reads, `/login`, `/register`, required static assets, shared error pages, and the VNPay callbacks `/payments/vnpay/ipn` and `/payments/vnpay/return` (trusted only after signature verification). `/admin/**` requires role `ADMIN`; customer purchase routes require role `CUSTOMER`.
- Keep CSRF enabled for state-changing SSR requests. Use Spring Security form login and server-side session authentication.
- Customer registration creates one `ACCOUNT` with forced role `CUSTOMER` and one linked `CUSTOMER` in the same transaction. Use the normalized email as the login name; trim it and lowercase it with `Locale.ROOT`. Reject an existing email, hash the password through `PasswordEncoder`, and never bind role/status from the registration form.
- Registration accepts a password of 8–64 characters plus a matching confirmation. On success, redirect to the login page instead of automatically authenticating the new account.
- Normalize surrounding whitespace from registration email before Bean Validation. A registration phone is exactly 10 digits starting with `0`; do not silently strip spaces or separators from it.
- Because the shared encoder is BCrypt, reject registration passwords longer than 72 UTF-8 bytes with a field validation error before calling `PasswordEncoder.encode`.
- A successfully registered account is `ACTIVE` immediately because email verification and account approval are outside this MVP.
- Password reset, social login, MFA, email verification, and account approval remain outside scope.
- Store only a strong one-way password hash supported by Spring Security; never store or log raw passwords.
- The shared V1 schema retains nullable guest-related columns for migration-checksum compatibility, but the current application does not create or operate guest carts. New MVP carts use `customer_id` and leave `guest_token` null.

### Initial Admin bootstrap

- The Admin owner provides a one-time `bootstrap-admin` profile/command that reads `BOOTSTRAP_ADMIN_EMAIL` and `BOOTSTRAP_ADMIN_PASSWORD` from environment variables and uses the shared `PasswordEncoder`.
- The command hashes the password through the application's `PasswordEncoder`, creates an ACTIVE ADMIN only when no Admin exists, and never prints the raw password or encoded hash.
- The bootstrap path is disabled during normal startup. If an Admin already exists, it exits without changing credentials. After successful creation, remove the environment variables and do not commit them.
- Do not seed an Admin password/hash through Flyway, `.env.example`, source code, or test fixtures intended for shared environments.

## 11. Database and configuration rules

- Flyway is the only schema owner; keep `spring.jpa.hibernate.ddl-auto=none`.
- Do not edit an applied/shared migration. Add a new reviewed version.
- Never run the reference Physical SQL against a Flyway-managed database.
- Keep database credentials in `.env`/environment variables and commit only `.env.example`.
- Do not log passwords, connection strings containing credentials, personal data, or payment-sensitive values.
- Use UTC consistently for stored timestamps.

## 12. Testing conventions

- Unit-test money, discount, state-machine, component aggregation, and inventory-demand calculations.
- Repository/integration tests cover conditional updates, constraints, and important queries against SQL Server-compatible behavior.
- Service tests cover transaction outcomes and failure rollback.
- MVC tests cover validation, authorization, view names, and redirects for important routes.
- Critical integration scenarios include registration with a duplicate email, unauthenticated purchase redirects, cross-customer ownership rejection, duplicate cart additions, concurrent checkout, insufficient stock, Customer cancellation of an owned `PLACED` order, rejection for another customer's or `PROCESSING` order, repeated/concurrent cancellation, Admin cancellation of `PROCESSING`, repeated delivery confirmation, and immutable PRESET composition.
- Every PR reports the exact tests run and does not claim database verification when only mocks were used.

### Test database isolation

- Default unit and MVC slice tests must not require SQL Server credentials.
- SQL Server repository/transaction tests use the `integration` Spring profile (`application-integration.properties`) and a dedicated local database named `MoonCakeTestDB`; they must never point at `MoonCakeDB` or another shared/production-like database.
- The integration profile reads `TEST_DB_HOST`, `TEST_DB_PORT`, `TEST_DB_NAME`, `TEST_DB_USERNAME`, and `TEST_DB_PASSWORD`; a safety assertion must confirm the connected database is exactly `MoonCakeTestDB` before tests may mutate data.
- Keep `spring.jpa.hibernate.ddl-auto=none` in the default application configuration. Override it with `spring.jpa.hibernate.ddl-auto=validate` only in the integration profile, after Flyway has migrated the test schema. Validation complements but does not replace explicit tests for CHECK, FK, filtered indexes, and concurrency.
- Keep test credentials in `.env`/OS environment variables such as `TEST_DB_USERNAME` and `TEST_DB_PASSWORD`; do not commit them.
- Name SQL Server integration tests `*IT`, annotate them with `@ActiveProfiles("integration")`, and run them separately with the Maven `integration` profile/Failsafe. Rename the existing `MoonCakeApplicationTests` context test to `MoonCakeApplicationIT` and move it into this path before feature development begins.
- Do not substitute H2 for SQL Server constraint/concurrency tests because filtered indexes, locking, and SQL semantics differ.
- Test cleanup may affect only data created inside `MoonCakeTestDB`. Never reset or drop an existing developer database as part of a test.
- Verify `mvnw.cmd -v` on each development machine. If the checked-in wrapper is reproducibly broken, repair/regenerate it in a separate tooling commit before declaring it the required PR command.

## 13. Change discipline

- Avoid unrelated formatting/refactoring in a feature PR.
- Do not rename approved database concepts such as `GIFT_SET` versus discount group `GIFT_BOX`.
- Update architecture/work-plan documents only when a team decision changes.
- If an implementation requires behavior not covered by Business Rules, stop and request a decision before coding it.

