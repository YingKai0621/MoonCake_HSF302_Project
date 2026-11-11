# MoonCake Project Instructions

These instructions apply to the entire repository.

## Read before changing code

Read these sources in order:

1. `docs/business-rules.md` for approved MVP behavior.
2. The Flyway migrations for executable database structure.
3. `docs/MoonCake_Physical_Schema_SQLServer.sql` and `docs/MoonCake_Physical_ERD_Design_Notes.md` for reviewed database design context.
4. `docs/architecture-conventions.md` for package, concurrency, security, and testing rules.
5. `docs/team-work-plan.md` for module/repository ownership.
6. `docs/foundation-plan.md` when working on Foundation.
7. `docs/instructor-code-rules.md` for the instructor-graded code checklist (copy it into every PR description).

If the sources conflict, stop and report the conflict. Do not invent a business decision.

## Non-negotiable scope

- Spring Boot MVC + Thymeleaf SSR and Microsoft SQL Server.
- Payment methods are COD and VNPay Sandbox (Business Rules v1.9, section 6). Do not add payOS, VietQR, another gateway, automatic refunds, reservation, payment approval, or unrelated features without a new Business Rules version.
- Visitors may browse the catalog, but registration/login is required before cart, CUSTOM-box, checkout, or customer-order operations. Customer self-registration is in scope.
- The schema has 25 tables after Flyway V19. Add tables only through a reviewed new migration.
- Never edit an applied/shared migration (`V1__...` through `V19__...`). Use a new version after team approval.
- Never commit `.env`, passwords, tokens, or real connection strings.

## Instructor-graded code rules (mandatory)

The project is graded 1-on-1 by the course instructor, who deducts points when these are broken. Check every change against them.

- Layers: `controller` -> `service` (interface) -> `service.impl` (`XxxServiceImpl`) -> `repository` (interface) -> `entity`; data to and from views goes through `dto`.
- A controller calls services only. Never inject a repository, `EntityManager`, gateway, or `Environment` into a controller.
- Every service is an interface with one implementation in `service.impl`; constructors depend on the interface.
- Views receive DTOs only: no entities, no `Object[]`, no raw query rows. Forms bind to a form DTO.
- Every table has a JPA entity. Prefer Spring Data JPA repositories over native SQL.
- Payment methods use the Strategy pattern (`PaymentProcessor` interface with `CodPaymentProcessor`, `VnpayPaymentProcessor`). Never branch on `if (method == VNPAY)` in checkout, cancellation, or order management.
- Write simple, readable code that the owner can explain: one statement per line, `import` instead of fully-qualified names, no compressed one-line Java/Thymeleaf/CSS/JS files. Simple must still be correct — keep ownership checks, conditional transitions, transactions, and tests.

## Architecture

- Use feature-first packages with shared database mappings in top-level `entity` and `enums` packages.
- Dependency direction is Controller -> Service -> Repository.
- A module may call another module's public service contract, not its repository.
- Use DTOs/forms/view models at web and module boundaries; do not bind Thymeleaf forms directly to complex entities.
- Keep the legacy catalog data as `product.dto.CatalogProductView`; never treat it as the database-backed `entity.Product`.
- Put transaction boundaries in service implementations, never controllers.
- Do not let Hibernate create or modify schema; Flyway remains the schema owner.
- Foundation defines the shared customer-identity contract and common authentication errors. The Customer Sales owner owns the single Spring Security configuration, customer registration/login/logout, and current-customer implementation; the Admin owner consumes it and owns only Admin access flows and initial-Admin bootstrap.

## Critical behavior

- Checkout first claims `CART` with conditional `ACTIVE -> CHECKED_OUT`; order creation, the `UNPAID` payment for the chosen method, inventory deduction, and movements commit in one transaction.
- The payment method is fixed at checkout. VNPay becomes `PAID` only from a verified IPN or `querydr` result, never from the Return URL. Reconcile an expired pending VNPay attempt before cancelling or retrying.
- Adding the same cart target increases quantity instead of inserting a duplicate row.
- A READY/stocked/referenced PRESET recipe is immutable; changed contents require a new SKU and configuration.
- PRESET sales deduct finished GIFT_SET stock; CUSTOM sales deduct box and cake component stock.
- Compute prices, tier discounts, shipping, snapshots, and totals on the server.
- Serialize cart/CUSTOM mutations on the ACTIVE cart row; use conditional state transitions for checkout, cancellation, shipping, and COD completion.
- Resolve cart/configuration/order ownership from the authenticated CUSTOMER principal; never trust caller-supplied account, customer, cart, or configuration IDs as proof of ownership.
- Keep catalog reads public. Unauthenticated purchase actions go through login and must not mutate state or automatically replay a POST.

## Collaboration and verification

- Respect the ownership in `docs/team-work-plan.md`; coordinate before editing another module's public contract or entities.
- Keep PRs small and do not mix unrelated refactors with feature work.
- Add proportional tests for changed behavior and run the relevant Maven tests before handoff.
- Keep the default JPA mode at `ddl-auto=none`; use `ddl-auto=validate` only in the dedicated integration-test profile after Flyway has migrated the schema.
- SQL Server integration tests run separately and use only the dedicated `MoonCakeTestDB`; never reset, clean, or write test data to `MoonCakeDB`.
- Report files changed, tests run, schema impact, and any unresolved assumptions.

