# Plan: Test Architecture Review & Restructuring

## TL;DR

Restructure tests using Maven naming conventions in a single default test root (`*Test`/`*Tests` for unit, `*IT` for integration), migrate integration tests to Testcontainers (PostgreSQL), fix coverage gaps (UserMapper 0%, ApplicationExceptionHandler 34% missed, ApplicationAuditorAware 58% missed), and refine JaCoCo exclusions.

---

## Phase 1: Convention-Based Unit/Integration Separation (Completed)

### Step 1 — Use Maven default test roots

Use default Maven roots only:

- `src/test/java/` — all tests
- `src/test/resources/` — shared test resources

Avoid additional source roots unless there is a hard requirement for multi-module separation.

### Step 2 — Configure Maven by naming convention

- Remove `build-helper-maven-plugin` custom source/resource registration
- Keep `maven-surefire-plugin` with default class-name discovery (`*Test`, `*Tests`)
- Keep `maven-failsafe-plugin` bound to `integration-test` + `verify` with default discovery (`*IT`)
- This gives `mvn test` = unit only, `mvn verify` = unit + integration

### Step 3 — Keep unit tests in default root

Unit tests remain as `*Test` / `*Tests` under `src/test/java/`:

- `auth/impl/AuthenticationServiceImplTest.java`
- `auth/request/AuthenticationRequestTest.java`
- `auth/request/RegistrationRequestTest.java`
- `auth/request/RefreshTokenRequestTest.java`
- `security/JwtFilterTest.java`
- `security/JwtServiceTest.java`
- `security/SecurityConfigUnitTest.java`
- `user/impl/UserServiceImplTest.java`

### Step 4 — Rename integration tests to `*IT`

- `AegisIdApplicationTests.java` -> `AegisIdApplicationIT.java`
- `SmokeTest.java` -> `SmokeIT.java`
- `auth/AuthenticationControllerTest.java` -> `auth/AuthenticationControllerIT.java`
- `security/SecurityConfigDevProfileTest.java` -> `security/SecurityConfigDevProfileIT.java`
- `security/SecurityConfigProdProfileTest.java` -> `security/SecurityConfigProdProfileIT.java`
- `user/UserControllerTest.java` -> `user/UserControllerIT.java`

### Step 5 — Consolidate test resources in single root

- Keep profile overrides in `src/test/resources/application-dev.yml` and `src/test/resources/application-prod.yml`
- Avoid duplicate file names (e.g., two `application-dev.yml` files in different active roots)

### Phase 1 Completion Status

- Completed in branch `chore/test-architecture-phase1`
- Validation passed:
    - `./mvnw clean test` (unit only)
    - `./mvnw clean verify` (unit + integration)

### Problems Encountered and How They Were Solved

1. Problem: Custom plugin setup added maintenance overhead (build-helper + class-by-class include/exclude lists).
    - Solution: Removed custom source-root plugin wiring and adopted Maven naming conventions (`*Test` and `*IT`).

2. Problem: Surefire still discovered nested integration classes when only top-level class excludes were configured.
    - Solution: Eliminated fragile excludes entirely by renaming integration suites to `*IT`, so Surefire naturally ignores them.

3. Problem: Duplicate `application-dev.yml` in different test resource roots caused classpath shadowing and DB misconfiguration.
    - Solution: Consolidated resources back to a single root (`src/test/resources`) to guarantee deterministic profile loading.

---

## Phase 2: Testcontainers Migration (Integration Tests) (Completed)

### Step 6 — Add Testcontainers dependency (_depends on step 2_)

Add to `pom.xml`:

- `org.testcontainers:postgresql` (test scope)
- `org.testcontainers:junit-jupiter` (test scope)
- `org.springframework.boot:spring-boot-testcontainers` (test scope)

### Step 7 — Create shared Testcontainers config (_depends on step 6_)

Create `src/test/java/.../testsupport/PostgresTestContainerConfig.java`:

- Use `@TestConfiguration` with `@ServiceConnection` for auto-configured PostgreSQL container
- Single shared container across all integration tests (singleton pattern) for performance
- Remove H2 from `pom.xml` test dependency

### Step 8 — Update integration test configs (_depends on step 7_)

- Remove H2 datasource config from `application-dev.yml` and `application-prod.yml` in `src/test/resources`
- Testcontainers + `@ServiceConnection` auto-configures datasource
- Keep `hibernate.ddl-auto: create-drop` for schema setup

### Step 9 — Update existing integration tests (_depends on step 7_)

- Add `@Import(PostgresTestContainerConfig.class)` to each integration test, or create a shared `@SpringBootTest` base annotation

### Phase 2 Completion Status

- Completed in branch `chore/test-architecture-phase2`
- Validation passed:
    - `./mvnw clean test` (unit only)
    - `./mvnw clean verify` (unit + integration on Testcontainers PostgreSQL)

### Problems Encountered and How They Were Solved

1. Problem: Testcontainers dependencies were added without project-level version management, causing unresolved dependency versions in Maven.
    - Solution: Imported `org.testcontainers:testcontainers-bom` in `dependencyManagement` and pinned `testcontainers.version`.

2. Problem: Eager static container startup in `PostgresTestContainerConfig` caused unstable Docker environment detection during context bootstrap.
    - Solution: Kept a singleton `PostgreSQLContainer` instance but removed manual `start()` so Spring Boot Testcontainers lifecycle starts and manages it via `@ServiceConnection`.

---

## Phase 3: Coverage Gap Fixes (Completed)

### Step 10 — Add `UserMapper` unit test (_parallel with phase 1-2_)

Create `src/test/java/.../user/UserMapperTest.java`:

- **UserMapper** has **0% coverage** (44 missed instructions)
- Test all mapping methods (DTO → Entity, Entity → DTO)
- Test null input handling
- Test partial field mappings

### Step 11 — Add `ApplicationExceptionHandler` test (_parallel with phase 1-2_)

Create `src/test/java/.../handler/ApplicationExceptionHandlerTest.java`:

- **ApplicationExceptionHandler** has **34% missed** (61/179 instructions)
- Currently only tested indirectly through integration controller tests
- Test each `@ExceptionHandler` method directly with mocked inputs:
    - `BusinessException` handling → verify correct ErrorResponse and HTTP status from ErrorCode
    - `MethodArgumentNotValidException` handling → verify validation error formatting
    - `HttpMessageNotReadableException` → malformed JSON body
    - `ConstraintViolationException` → path-variable/query-param validation
    - Any other `@ExceptionHandler` annotated methods with uncovered paths

### Step 12 — Improve `ApplicationAuditorAware` test (_parallel_)

Create `src/test/java/.../config/ApplicationAuditorAwareTest.java`:

- **ApplicationAuditorAware** has **58% missed** (14/24 instructions, 5/6 branches missed)
- Test when SecurityContext has authenticated user → returns user ID
- Test when SecurityContext is empty → returns empty Optional
- Test when authentication is null → returns empty Optional
- Test when principal is not a User instance → returns empty Optional

### Step 13 — Add `UserMapper` to JaCoCo exclusions (_if mapper is trivial_)

Decision: After reviewing UserMapper, if it's a simple field-copying mapper, consider excluding from coverage instead of testing. Otherwise, write the test (step 10).

### Phase 3 Completion Status

- Completed in branch `chore/test-architecture-phase3`
- Implemented:
    - `src/test/java/com/uynguyen/aegis_id/user/UserMapperTest.java`
    - `src/test/java/com/uynguyen/aegis_id/handler/ApplicationExceptionHandlerTest.java`
    - `src/test/java/com/uynguyen/aegis_id/config/ApplicationAuditorAwareTest.java`
- Step 13 decision: `UserMapper` was **not excluded** from JaCoCo; we kept it in scope and added direct unit coverage.

### Problems Encountered and How They Were Solved

1. Problem: `ApplicationAuditorAware` assumed `Authentication#getPrincipal()` was always a `User`, causing `ClassCastException` for authenticated non-`User` principals in direct unit tests.
    - Solution: Updated `ApplicationAuditorAware#getCurrentAuditor()` to guard principal type and return `Optional.empty()` when principal is not a `User`.

2. Problem: Sonar/static analysis flagged `assertThrows` lambdas in new tests for having multiple possible runtime invocations.
    - Solution: Refactored test setup to construct objects outside the lambda and keep only one invocation inside each `assertThrows` block.

3. Problem: The plan expected malformed JSON and constraint-violation coverage paths; current handler implementation has no dedicated `@ExceptionHandler` methods for these and routes them through the generic `Exception` handler.
    - Solution: Added direct unit tests that validate current fallback behavior for `HttpMessageNotReadableException` and `ConstraintViolationException`.

---

## Phase 4: JaCoCo Exclusion Refinements (Completed)

### Step 14 — Update JaCoCo and Sonar exclusions

Add config classes to exclusions in `pom.xml`:

- `**/config/OpenApiConfig.java` — pure Spring config bean, 0% coverage, not worth testing
- `**/config/JpaConfig.java` — single annotation-driven config
- `**/config/BeansConfig.java` — pure bean wiring (already 100% via integration, but shouldn't inflate coverage)

Update both `<jacoco.excludes>` and `<sonar.coverage.exclusions>` properties.

Also consider excluding:

- `**/exception/ErrorCode.java` — enum with static data (100% via side effects, but not testing logic)
- Entity/DTO Lombok-generated code — User entity shows 71% missed, mostly Lombok constructors/getters

### Step 15 — Add Lombok JaCoCo exclusion via `lombok.config`

Create/update `lombok.config` in project root:

```
lombok.addLombokGeneratedAnnotation = true
```

This makes JaCoCo automatically skip Lombok-generated code (getters, setters, constructors, builders, equals/hashCode). This will fix the User entity's 71% missed coverage being a false negative.

### Phase 4 Completion Status

- Implementation completed in branch `chore/test-architecture-phase4`.
- Updated `pom.xml` to add `<jacoco.excludes>` and expanded `<sonar.coverage.exclusions>` with:
    - `**/*Application`
    - `**/JwtConfigEndpoint`
    - `**/config/OpenApiConfig`
    - `**/config/JpaConfig`
    - `**/config/BeansConfig`
    - `**/exception/ErrorCode`
- Updated JaCoCo plugin report excludes in `pom.xml` to class patterns (`.class`).
- Added root `lombok.config`:
    - `config.stopBubbling = true`
    - `lombok.addLombokGeneratedAnnotation = true`

### Phase 4 Validation Status

- `./mvnw -Pcoverage -DskipITs clean verify` → **BUILD SUCCESS**.
- First attempt: `./mvnw -Pcoverage verify` → **BUILD FAILURE** (Docker unavailable for Testcontainers).
- Retry after enabling Docker: `./mvnw -Pcoverage verify` → **BUILD SUCCESS**.
- Surefire (unit) summary from successful full verify run:
    - Tests run: 151
    - Failures: 0
    - Errors: 0
    - Skipped: 0
- Failsafe (integration) summary from successful full verify run:
    - Tests run: 31
    - Failures: 0
    - Errors: 0
    - Skipped: 0
- JaCoCo report generated successfully (`target/site/jacoco/jacoco.xml`) with excluded classes applied.

### Problems Encountered and How They Were Solved

1. Problem: JaCoCo excludes were first written as `*.java` patterns, which JaCoCo does not use for bytecode filtering.
    - Solution: Switched JaCoCo exclusions to `*.class` in both `<jacoco.excludes>` and plugin `<excludes>` while keeping Sonar exclusions as source-file (`*.java`) patterns.

2. Problem: Initial full coverage verify failed because Testcontainers could not find a Docker environment (`Previous attempts to find a Docker environment failed. Will not retry.`), causing ApplicationContext startup errors across integration tests.
    - Solution: Enabled Docker and reran full coverage verification. Integration tests then passed and full coverage build completed successfully.

---

## Phase 5: Test Quality Improvements (Completed)

### Step 16 — Consolidate `AegisIdApplicationIT` and `SmokeIT`

Both are `@SpringBootTest` tests that verify context loading. `SmokeIT` checks conditional beans, `AegisIdApplicationIT` just verifies context loads. Consider merging into one file to reduce integration test startup overhead.

### Step 17 — Clean up phantom test report

`RedirectUriValidatorTest` appears in surefire reports but source file doesn't exist. Clean up stale test reports from `target/surefire-reports/`.

### Phase 5 Completion Status

- Completed in branch `chore/test-architecture-phase5`.
- Implemented:
    - Removed `src/test/java/com/uynguyen/aegis_id/AegisIdApplicationIT.java` and consolidated context-load coverage into `SmokeIT`.
    - Regenerated test reports via `./mvnw clean verify` so stale report artifacts are removed before report inspection.
    - Verified no `RedirectUriValidatorTest` artifact exists in `target/surefire-reports/`.
- Validation passed:
    - `./mvnw clean verify` → **BUILD SUCCESS**.
    - Surefire (unit) summary: 151 tests, 0 failures, 0 errors.
    - Failsafe (integration) summary: 30 tests, 0 failures, 0 errors.

### Problems Encountered and How They Were Solved

1. Problem: Removing `AegisIdApplicationIT` could accidentally drop basic context bootstrap coverage.
    - Solution: Kept `SmokeIT#contextLoads()` as the consolidated context-load suite; it still asserts critical bean wiring while validating context startup.

2. Problem: The `RedirectUriValidatorTest` phantom could not be reproduced from source because no matching test class exists.
    - Solution: Treated this as stale report state, ran `clean verify` to rebuild reports from scratch, and confirmed the phantom entry does not appear in regenerated surefire outputs.

---

## Relevant Files

**Build config:**

- `pom.xml` — surefire/failsafe/JaCoCo plugin configuration, new dependencies
- `lombok.config` — create with `addLombokGeneratedAnnotation = true`

**Current unit tests (`*Test`/`*Tests` in `src/test/java/`):**

- `src/test/java/com/uynguyen/aegis_id/auth/impl/AuthenticationServiceImplTest.java`
- `src/test/java/com/uynguyen/aegis_id/auth/request/AuthenticationRequestTest.java`
- `src/test/java/com/uynguyen/aegis_id/auth/request/RegistrationRequestTest.java`
- `src/test/java/com/uynguyen/aegis_id/auth/request/RefreshTokenRequestTest.java`
- `src/test/java/com/uynguyen/aegis_id/security/JwtFilterTest.java`
- `src/test/java/com/uynguyen/aegis_id/security/JwtServiceTest.java`
- `src/test/java/com/uynguyen/aegis_id/security/SecurityConfigUnitTest.java`
- `src/test/java/com/uynguyen/aegis_id/user/impl/UserServiceImplTest.java`
- `src/test/java/com/uynguyen/aegis_id/testsupport/ValidationTestSupport.java`

**Current integration tests (`*IT` in `src/test/java/`):**

- `src/test/java/com/uynguyen/aegis_id/SmokeIT.java`
- `src/test/java/com/uynguyen/aegis_id/auth/AuthenticationControllerIT.java`
- `src/test/java/com/uynguyen/aegis_id/security/SecurityConfigDevProfileIT.java`
- `src/test/java/com/uynguyen/aegis_id/security/SecurityConfigProdProfileIT.java`
- `src/test/java/com/uynguyen/aegis_id/user/UserControllerIT.java`

**New tests added in Phase 3:**

- `src/test/java/com/uynguyen/aegis_id/user/UserMapperTest.java`
- `src/test/java/com/uynguyen/aegis_id/handler/ApplicationExceptionHandlerTest.java`
- `src/test/java/com/uynguyen/aegis_id/config/ApplicationAuditorAwareTest.java`

**New integration support:**

- `src/test/java/.../testsupport/PostgresTestContainerConfig.java`

**Production code covered in Phase 3 (with one targeted change):**

- `src/main/java/com/uynguyen/aegis_id/user/UserMapper.java` — now directly unit tested
- `src/main/java/com/uynguyen/aegis_id/handler/ApplicationExceptionHandler.java` — now directly unit tested
- `src/main/java/com/uynguyen/aegis_id/config/ApplicationAuditorAware.java` — directly unit tested + principal-type guard added

---

## Verification

1. **`mvn test`** — runs only unit tests (`*Test` / `*Tests`), fast, no DB
2. **`mvn verify`** — runs unit + integration tests (`*IT` via failsafe)
3. **`mvn verify -Pcoverage`** — generates JaCoCo report with updated exclusions
4. Verify JaCoCo report shows Lombok-generated code excluded (User entity gap resolved)
5. Verify `UserMapper` coverage is now >0%
6. Verify `ApplicationExceptionHandler` coverage gap reduced
7. Verify Testcontainers PostgreSQL starts correctly for integration tests
8. Verify `mvn test` does NOT start any Spring context or Docker container
9. ✅ Phase 3 validation executed:
    - `./mvnw -Dtest=UserMapperTest,ApplicationExceptionHandlerTest,ApplicationAuditorAwareTest test` → 23 tests, 0 failures
    - `./mvnw test` → 151 tests, 0 failures
10. ✅ Phase 5 validation executed:
    - `./mvnw clean verify` → BUILD SUCCESS
    - Failsafe summary: completed 30, failures 0, errors 0
    - `target/surefire-reports/` contains no `RedirectUriValidatorTest` report artifact

---

## Decisions

- **Convention-based separation** over custom source roots — standard Maven behavior with lower maintenance overhead
- **Config classes excluded from coverage** — OpenApiConfig, JpaConfig, BeansConfig are pure wiring; testing them adds noise, not safety
- **Testcontainers** replaces H2 — catches PostgreSQL-specific SQL issues; H2 `MODE=PostgreSQL` is leaky abstraction
- **Lombok config** to exclude generated code — standard approach, avoids false negatives on entities
- **Scope boundary**: This plan does NOT add new feature tests (e.g., repository integration tests) — it restructures existing tests and fills clear gaps
