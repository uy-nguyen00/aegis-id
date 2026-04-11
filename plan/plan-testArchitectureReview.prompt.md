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

## Phase 2: Testcontainers Migration (Integration Tests)

### Step 5 — Add Testcontainers dependency (_depends on step 2_)

Add to `pom.xml`:

- `org.testcontainers:postgresql` (test scope)
- `org.testcontainers:junit-jupiter` (test scope)
- `org.springframework.boot:spring-boot-testcontainers` (test scope)

### Step 6 — Create shared Testcontainers config (_depends on step 5_)

Create `src/test/java/.../testsupport/PostgresTestContainerConfig.java`:

- Use `@TestConfiguration` with `@ServiceConnection` for auto-configured PostgreSQL container
- Single shared container across all integration tests (singleton pattern) for performance
- Remove H2 from `pom.xml` test dependency

### Step 7 — Update integration test configs (_depends on step 6_)

- Remove H2 datasource config from `application-dev.yml` and `application-prod.yml` in `src/test/resources`
- Testcontainers + `@ServiceConnection` auto-configures datasource
- Keep `hibernate.ddl-auto: create-drop` for schema setup

### Step 8 — Update existing integration tests (_depends on step 6_)

- Add `@Import(PostgresTestContainerConfig.class)` to each integration test, or create a shared `@SpringBootTest` base annotation

---

## Phase 3: Coverage Gap Fixes

### Step 9 — Add `UserMapper` unit test (_parallel with phase 1-2_)

Create `src/test/java/.../user/UserMapperTest.java`:

- **UserMapper** has **0% coverage** (44 missed instructions)
- Test all mapping methods (DTO → Entity, Entity → DTO)
- Test null input handling
- Test partial field mappings

### Step 10 — Add `ApplicationExceptionHandler` test (_parallel with phase 1-2_)

Create `src/test/java/.../handler/ApplicationExceptionHandlerTest.java`:

- **ApplicationExceptionHandler** has **34% missed** (61/179 instructions)
- Currently only tested indirectly through integration controller tests
- Test each `@ExceptionHandler` method directly with mocked inputs:
    - `BusinessException` handling → verify correct ErrorResponse and HTTP status from ErrorCode
    - `MethodArgumentNotValidException` handling → verify validation error formatting
    - `HttpMessageNotReadableException` → malformed JSON body
    - `ConstraintViolationException` → path-variable/query-param validation
    - Any other `@ExceptionHandler` annotated methods with uncovered paths

### Step 11 — Improve `ApplicationAuditorAware` test (_parallel_)

Create `src/test/java/.../config/ApplicationAuditorAwareTest.java`:

- **ApplicationAuditorAware** has **58% missed** (14/24 instructions, 5/6 branches missed)
- Test when SecurityContext has authenticated user → returns user ID
- Test when SecurityContext is empty → returns empty Optional
- Test when authentication is null → returns empty Optional
- Test when principal is not a User instance → returns empty Optional

### Step 12 — Add `UserMapper` to JaCoCo exclusions (_if mapper is trivial_)

Decision: After reviewing UserMapper, if it's a simple field-copying mapper, consider excluding from coverage instead of testing. Otherwise, write the test (step 9).

---

## Phase 4: JaCoCo Exclusion Refinements

### Step 13 — Update JaCoCo and Sonar exclusions

Add config classes to exclusions in `pom.xml`:

- `**/config/OpenApiConfig.java` — pure Spring config bean, 0% coverage, not worth testing
- `**/config/JpaConfig.java` — single annotation-driven config
- `**/config/BeansConfig.java` — pure bean wiring (already 100% via integration, but shouldn't inflate coverage)

Update both `<jacoco.excludes>` and `<sonar.coverage.exclusions>` properties.

Also consider excluding:

- `**/exception/ErrorCode.java` — enum with static data (100% via side effects, but not testing logic)
- Entity/DTO Lombok-generated code — User entity shows 71% missed, mostly Lombok constructors/getters

### Step 14 — Add Lombok JaCoCo exclusion via `lombok.config`

Create/update `lombok.config` in project root:

```
lombok.addLombokGeneratedAnnotation = true
```

This makes JaCoCo automatically skip Lombok-generated code (getters, setters, constructors, builders, equals/hashCode). This will fix the User entity's 71% missed coverage being a false negative.

---

## Phase 5: Test Quality Improvements

### Step 15 — Consolidate `AegisIdApplicationIT` and `SmokeIT`

Both are `@SpringBootTest` tests that verify context loading. `SmokeIT` checks conditional beans, `AegisIdApplicationIT` just verifies context loads. Consider merging into one file to reduce integration test startup overhead.

### Step 16 — Clean up phantom test report

`RedirectUriValidatorTest` appears in surefire reports but source file doesn't exist. Clean up stale test reports from `target/surefire-reports/`.

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

- `src/test/java/com/uynguyen/aegis_id/AegisIdApplicationIT.java`
- `src/test/java/com/uynguyen/aegis_id/SmokeIT.java`
- `src/test/java/com/uynguyen/aegis_id/auth/AuthenticationControllerIT.java`
- `src/test/java/com/uynguyen/aegis_id/security/SecurityConfigDevProfileIT.java`
- `src/test/java/com/uynguyen/aegis_id/security/SecurityConfigProdProfileIT.java`
- `src/test/java/com/uynguyen/aegis_id/user/UserControllerIT.java`

**New tests to create:**

- `src/test/java/.../user/UserMapperTest.java`
- `src/test/java/.../handler/ApplicationExceptionHandlerTest.java`
- `src/test/java/.../config/ApplicationAuditorAwareTest.java`

**New integration support:**

- `src/test/java/.../testsupport/PostgresTestContainerConfig.java`

**Production code for reference (no changes):**

- `src/main/java/com/uynguyen/aegis_id/user/UserMapper.java` — 0% coverage
- `src/main/java/com/uynguyen/aegis_id/handler/ApplicationExceptionHandler.java` — 34% missed
- `src/main/java/com/uynguyen/aegis_id/config/ApplicationAuditorAware.java` — 58% missed

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

---

## Decisions

- **Convention-based separation** over custom source roots — standard Maven behavior with lower maintenance overhead
- **Config classes excluded from coverage** — OpenApiConfig, JpaConfig, BeansConfig are pure wiring; testing them adds noise, not safety
- **Testcontainers** replaces H2 — catches PostgreSQL-specific SQL issues; H2 `MODE=PostgreSQL` is leaky abstraction
- **Lombok config** to exclude generated code — standard approach, avoids false negatives on entities
- **Scope boundary**: This plan does NOT add new feature tests (e.g., repository integration tests) — it restructures existing tests and fills clear gaps
