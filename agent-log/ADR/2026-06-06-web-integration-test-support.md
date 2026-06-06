# ADR: Shared Web Integration Test Support

## Context

Controller integration tests in `auth` and `friends` were duplicating the same Spring Boot, Security, MockMvc, and RestDocs setup.
The previous `AuthIntegrationTestSupport` name implied an auth-only helper, but it was already being reused by non-auth controller integration tests.

## Decision

Introduce `com.common.testsupport.WebIntegrationTestSupport` as the shared base class for all web integration tests.
Keep persistence and infrastructure setup in `PersistenceTestSupport`, and let the web base extend it to reuse Redis, RabbitMQ, and mock bean wiring.

## Rationale

- One place for `MockMvc`, `RestDocs`, `springSecurity()`, UTF-8 filter, and `JsonMapper`.
- Removes the misleading `Auth`-scoped naming from a cross-cutting test harness.
- Reduces repeated annotations and setup code across controller integration tests.

## Consequences

- Existing controller integration tests now inherit from the shared web base.
- Future web integration tests should use `WebIntegrationTestSupport` instead of adding their own `@SpringBootTest` / MockMvc setup.
- The old commented `ControllerTestSupport` and the `AuthIntegrationTestSupport` shim are no longer needed.

## Related

- `src/test/kotlin/com/common/testsupport/PersistenceTestSupport.kt`
- `src/test/kotlin/com/common/testsupport/WebIntegrationTestSupport.kt`
- `agent-log/2026-06-06.md`
