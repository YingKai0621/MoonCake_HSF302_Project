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

If the sources conflict, stop and report the conflict. Do not invent a business decision.

## Non-negotiable scope

- Spring Boot MVC + Thymeleaf SSR and Microsoft SQL Server.
- MVP payment is COD only. Do not add payOS, VietQR, reservation, payment approval, or unrelated features.
- Visitors may browse the catalog, but registration/login is required before cart, CUSTOM-box, checkout, or customer-order operations. Customer self-registration is in scope.
- Keep the approved 19-table design unless a reviewed migration is explicitly required.
- Never edit an applied/shared `V1__...`, `V2__...`, or `V3__...` migration. Use a new version after team approval.
- Never commit `.env`, passwords, tokens, or real connection strings.

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

- Checkout first claims `CART` with conditional `ACTIVE -> CHECKED_OUT`; order creation, COD payment, inventory deduction, and movements commit in one transaction.
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

