# Plan: Decouple User from Role

## TL;DR

Decouple User and Role so registration doesn't require roles, inject user full name and email into JWT tokens, introduce `TokenUserInfo` to eliminate parameter explosion in token APIs, seed roles from environment variables on startup (auto-prefixed with `ROLE_`), make JWT roles claims toggleable via env var, provide admin endpoints for user-role assignment, and skip creating a RoleController for role CRUD (security risk with no current benefit).

**Phase order** — prioritized by isolation and risk (safest first):

1. **JWT roles claim toggle** — zero runtime impact (JwtFilter already loads roles from DB, not JWT)
2. **Decouple registration** — removes hard `ROLE_USER` requirement
3. **Inject full name claim into JWT** — claim-only change with no authorization impact
4. **Introduce `TokenUserInfo` and add email claim** — structural refactor + new claim, no authorization impact
5. **Role seeding from env vars** — new feature, no dependency on 1-4
6. **Admin endpoints** — depends on roles being seeded

Each phase includes its own tests.

---

## Phase 1: Make JWT Roles Claim Toggleable ✅ DONE

> **Why first?** `JwtFilter` already ignores JWT roles claims — it loads authorities from the database. The `roles` claim in the JWT is dead weight. Toggling it off changes nothing about how requests are authorized. This is the safest, most isolated change.

### 1.1 — Configuration property

- Added `app.security.jwt.include-roles-claim` in `application.yml`, bound to env var `JWT_INCLUDE_ROLES_CLAIM` (default: `false`)
- Added `management.server.port` bound to env var `MANAGEMENT_PORT` (default: `8081`)
- Exposed actuator endpoints: `health`, `jwtconfig` via `management.endpoints.web.exposure.include`
- Registered the new property in `META-INF/additional-spring-configuration-metadata.json`
- Test profile (`application-dev.yml`) sets `include-roles-claim: true` to keep existing test behavior

### 1.2 — JwtService toggle

- Added `AtomicBoolean includeRolesClaim` field — thread-safe for runtime switching
- Constructor takes a third parameter `boolean includeRolesClaim` (injected from config)
- `generateAccessToken()` — only adds `claims.put(ROLES_CLAIM, roles)` when `includeRolesClaim.get()` is `true`
- `generateRefreshToken()` — same conditional logic
- `extractRolesFromToken()` — returns `Collections.emptyList()` when claim is `null` (absent)
- `refreshAccessToken()` — extracts roles from refresh token with null guard (`roles != null ? roles : Collections.emptyList()`), then delegates to `generateAccessToken()` which re-applies the toggle
- Added `isIncludeRolesClaim()` / `setIncludeRolesClaim(boolean)` for runtime control

### 1.3 — Custom Actuator endpoint

- New class `JwtConfigEndpoint` — `@Endpoint(id = "jwtconfig")` component
- `@ReadOperation` (GET `/actuator/jwtconfig`) — returns `{"includeRolesClaim": true/false}`
- `@WriteOperation` (POST `/actuator/jwtconfig`) — accepts `boolean includeRolesClaim`, updates toggle, returns new state
- Excluded from JaCoCo coverage in `pom.xml` (pure delegation, no branching logic)

### 1.4 — Management port separation and security

- Actuator endpoints served on port `8081` (configurable via `MANAGEMENT_PORT`), not exposed through main API port `8080`
- Added dedicated `SecurityFilterChain` (`@Order(1)`) in `SecurityConfig`:
    - `securityMatcher` uses `request.getLocalPort() == managementPort` to scope to management port only
    - `/actuator/**` → `permitAll()`
    - Everything else on port 8081 → `denyAll()` (prevents Swagger, login form, etc. from being served on management port)
    - CSRF disabled (stateless, no cookies — SonarQube S4502 hotspot reviewed as safe)
    - Stateless session policy
- Main filter chain (`@Order(2)`) unchanged in behavior
- Static resource URLs (`/`, `/index.html`, `/css/**`, `/js/**`) moved from `BASE_PUBLIC_URLS` to `DEV_PUBLIC_URLS` — now only accessible in dev profile on port 8080

### 1.5 — Infrastructure updates

- `Dockerfile` — updated `EXPOSE 8080 8081`, health check targets `http://localhost:8081/actuator/health`
- `docker-compose.yml` — added port mapping `8081:8081`, added `JWT_INCLUDE_ROLES_CLAIM` and `MANAGEMENT_PORT` env vars
- `.env.example` — added `JWT_INCLUDE_ROLES_CLAIM` and `MANAGEMENT_PORT` entries
- `README.md` — documented management port, env vars, and actuator endpoint table

### 1.6 — Tests

- Updated all `new JwtService(...)` calls in `JwtServiceTest` to 3-arg constructor with `true`
- Added `@Nested` class `RolesClaimToggleTests` with 5 tests:
    1. `shouldIncludeRolesClaimWhenToggleOn` — verifies roles present in JWT when toggle is on
    2. `shouldExcludeRolesClaimWhenToggleOff` — verifies roles absent when toggle is off
    3. `shouldExcludeRolesFromRefreshTokenWhenToggleOff` — verifies refresh token also excludes roles
    4. `shouldRefreshAccessTokenWithoutRolesClaim` — verifies refresh flow works when roles claim is absent
    5. `shouldToggleAtRuntime` — verifies `setIncludeRolesClaim()` switches behavior without restart
- Added test for actuator filter chain error wrapping in `SecurityConfigUnitTest`
- All 147 tests pass

### Files changed

- `src/main/java/com/uynguyen/aegis_id/security/JwtService.java` — toggle logic
- `src/main/java/com/uynguyen/aegis_id/security/JwtConfigEndpoint.java` — **NEW**: Actuator endpoint
- `src/main/java/com/uynguyen/aegis_id/security/SecurityConfig.java` — actuator filter chain, dev-only static URLs
- `src/main/resources/application.yml` — new properties
- `src/main/resources/META-INF/additional-spring-configuration-metadata.json` — new property metadata
- `src/test/resources/application-dev.yml` — `include-roles-claim: true`
- `src/test/java/com/uynguyen/aegis_id/security/JwtServiceTest.java` — 5 new toggle tests
- `src/test/java/com/uynguyen/aegis_id/security/SecurityConfigUnitTest.java` — actuator chain test
- `Dockerfile` — health check port, expose 8081
- `docker-compose.yml` — port mapping, env vars
- `.env.example` — new env vars
- `README.md` — management endpoints documentation
- `pom.xml` — JaCoCo exclusion for `JwtConfigEndpoint`

---

## Phase 2: Decouple Registration from Role ✅ DONE

> **Why second?** It removes the hard `ROLE_USER` dependency while keeping authentication behavior stable. Users can exist with zero roles and still sign in.

### 2.1 — Registration flow decoupled

- Removed `RoleRepository` from `AuthenticationServiceImpl`
- Removed `ROLE_USER` lookup and role assignment from `register()`
- Registration now saves the mapped user directly
- `User.roles` defaults to empty via `@Builder.Default`, so new users start without roles

### 2.2 — Authorities behavior with empty roles

- Confirmed unchanged: `User.getAuthorities()` returns `List.of()` when roles is empty

### 2.3 — Tests

- Removed the register test case expecting failure when `ROLE_USER` is missing
- Updated register-success test to remove role repository stubbing
- Verified saved user has initialized, empty roles
- Targeted suite passed: `AuthenticationServiceImplTest` (8 tests, 0 failures, 0 errors)

### 2.4 — Verification

1. Register user → persisted with empty roles
2. Login still succeeds for user with zero roles
3. Full suite passed: 146 tests, 0 failures, 0 errors

### Files changed

- `src/main/java/com/uynguyen/aegis_id/auth/impl/AuthenticationServiceImpl.java` — removed role lookup/assignment from `register()`
- `src/test/java/com/uynguyen/aegis_id/auth/impl/AuthenticationServiceImplTest.java` — updated registration tests for role-free registration
- `src/main/java/com/uynguyen/aegis_id/user/User.java` — ensured roles are initialized by default for builder-created users

---

## Phase 3: Inject User Full Name into JWT Tokens ✅ DONE

> **Why third?** This is a claim-only enhancement and does not affect authorization decisions (`JwtFilter` still loads authorities from DB). It is isolated from role seeding and admin endpoint work.

### 3.1 — Token claim contract implemented

- Added JWT claim key `full_name` in `JwtService`
- Implemented full-name normalization:
    - trim each name part
    - collapse internal whitespace (`\s+` → single space)
    - combine `firstName` + `lastName` with one space when both exist
    - when only one part exists, use that part
    - when both parts are empty/null, omit `full_name` claim
- Added `extractFullNameFromToken()` for explicit claim assertions in tests

### 3.2 — Token generation updated (breaking API by design)

- `generateAccessToken(...)` now takes four arguments: `(userId, roles, firstName, lastName)`
- `generateRefreshToken(...)` now takes four arguments: `(userId, roles, firstName, lastName)`
- Removed backward-compatible 2-arg overloads intentionally (no compatibility layer)
- Full-name claim remains independent from `include-roles-claim` toggle

### 3.3 — Authentication and refresh flow wiring

- `AuthenticationServiceImpl.login()` now passes `user.getFirstName()` and `user.getLastName()` to both token generation calls
- `JwtService.refreshAccessToken()` now reads `full_name` from refresh token and preserves it when minting a new access token

### 3.4 — Tests

- Added `FullNameClaimTests` in `JwtServiceTest` with 4 cases:
    1. include claim when both names are present
    2. normalize/handle missing first or last name
    3. omit claim when both names are empty/null
    4. preserve claim across refresh-token → access-token flow
- Updated `JwtServiceTest` existing token-generation calls to 4-arg signature
- Updated `AuthenticationServiceImplTest` to:
    - provide first/last name in login principal fixture
    - verify `JwtService` is called with first/last name arguments

### 3.5 — Verification

1. Targeted validation passed: `JwtServiceTest` + `AuthenticationServiceImplTest` (25 tests, 0 failures)
2. Full suite passed after signature change: 150 tests, 0 failures, 0 errors

---

## Phase 4: Introduce `TokenUserInfo` and Add Email Claim

> **Why now?** Adding email to the token would be the third time we extend `generateAccessToken` / `generateRefreshToken` signatures. Each new claim currently requires changes to method signatures, all callers, the refresh flow, and every test. Introducing a `TokenUserInfo` record first consolidates user data into a single object, making this change — and all future claim additions — a localized edit.

### The structural problem

Current state: `generateAccessToken(userId, roles, firstName, lastName)` — 4 scalar parameters. Adding email makes it 5. Each addition touches:

- `JwtService.generateAccessToken()` and `generateRefreshToken()` signatures
- `JwtService.refreshAccessToken()` claim extraction
- `AuthenticationServiceImpl.login()` call site
- Every test that constructs or mocks token generation

### Step 4.1 — Introduce `TokenUserInfo` record

- New record: `src/main/java/com/uynguyen/aegis_id/security/TokenUserInfo.java`
- Fields: `String userId`, `List<String> roles`, `String firstName`, `String lastName`, `String email`
- Pure data carrier — no business logic, no Spring annotations
- Future claims (e.g., avatar URL, locale) only require adding a field here + claim-building logic in `JwtService`

### Step 4.2 — Refactor `JwtService` to use `TokenUserInfo`

- Replace `generateAccessToken(String userId, List<String> roles, String firstName, String lastName)` → `generateAccessToken(TokenUserInfo userInfo)`
- Replace `generateRefreshToken(String userId, List<String> roles, String firstName, String lastName)` → `generateRefreshToken(TokenUserInfo userInfo)`
- Extract a private `buildClaims(TokenUserInfo userInfo, String tokenType)` method that centralizes all claim construction:
    - `token_type` — always added
    - `roles` — conditional on `includeRolesClaim` toggle
    - `full_name` — computed from `userInfo.firstName()` + `userInfo.lastName()` (existing logic)
    - `email` — always added when non-null/non-blank
- New constant: `EMAIL_CLAIM = "email"`
- Future claims: add one `claims.put(...)` line in `buildClaims()` — no signature changes anywhere

### Step 4.3 — Update refresh flow

- `refreshAccessToken()` reconstructs `TokenUserInfo` from token claims:
    - `userId` ← `claims.getSubject()`
    - `roles` ← `claims.get(ROLES_CLAIM)` with null guard
    - `firstName` ← `claims.get(FULL_NAME_CLAIM)` (already-normalized full name, passed as firstName — `buildFullName` returns it as-is since lastName is null)
    - `lastName` ← `null`
    - `email` ← `claims.get(EMAIL_CLAIM)`
- Then delegates to `generateAccessToken(userInfo)` as before
- **Net effect**: adding a new claim to refresh requires only adding one `claims.get(...)` line + one record field — no signature cascading

### Step 4.4 — Add `extractEmailFromToken()` accessor

- New public method in `JwtService` for explicit claim extraction in tests
- Pattern matches existing `extractFullNameFromToken()` and `extractRolesFromToken()`

### Step 4.5 — Update `AuthenticationServiceImpl`

- `login()` builds a `TokenUserInfo` from the `User` principal:
    ```
    TokenUserInfo userInfo = new TokenUserInfo(
        user.getUserId(), roles, user.getFirstName(), user.getLastName(), user.getEmail()
    );
    ```
- Passes `userInfo` to both `generateAccessToken()` and `generateRefreshToken()`
- **Future additions**: just add a field from `User` to the record constructor — one line

### Step 4.6 — Tests

- Update all `JwtServiceTest` token-generation calls from 4-arg to `TokenUserInfo`-based
- Add `@Nested` class `EmailClaimTests` with cases:
    1. email included in access token when present
    2. email included in refresh token when present
    3. email omitted when null/blank
    4. email preserved across refresh-token → access-token flow
- Update `AuthenticationServiceImplTest` to:
    - verify `JwtService` is called with `TokenUserInfo` containing the expected email
    - use `ArgumentCaptor<TokenUserInfo>` instead of positional `eq()/any()` matchers (cleaner assertions)
- Update `JwtFilterTest` if it mocks token generation

### Step 4.7 — Verification

1. Targeted validation: `JwtServiceTest` + `AuthenticationServiceImplTest` pass
2. Full suite passes with no regressions
3. Generated tokens contain `email` claim when user has email
4. Refreshed tokens preserve `email` claim

### Why `TokenUserInfo` and not a `Map<String, Object>`?

- **Type safety** — compiler catches missing/wrong fields; a map silently accepts anything
- **Discoverability** — record fields document exactly what data token generation needs
- **Refactor-friendly** — renaming a field is a compile error, not a silent runtime bug
- **Testability** — `ArgumentCaptor<TokenUserInfo>` gives structured assertions vs. inspecting map keys

### Future claim checklist (after this phase)

To add a new claim to JWT tokens:

1. Add field to `TokenUserInfo` record
2. In `JwtService.buildClaims()`, add `claims.put(NEW_CLAIM, userInfo.newField())`
3. In `JwtService.refreshAccessToken()`, extract the claim and pass to `TokenUserInfo` constructor
4. In `AuthenticationServiceImpl.login()`, pass the data from `User` to `TokenUserInfo`
5. (Optional) Add `extractNewFieldFromToken()` accessor in `JwtService`
6. Add tests

No method signature changes. No test rewrites. ~5 lines of production code per claim.

---

## Phase 5: Role Seeding from Environment Variables

### Step 5.1 — Add configuration properties

- In `application.yml`, add:
    - `app.security.roles` — comma-separated list of role names (e.g., `USER,ADMIN` or `CREATOR,MEMBER`)
- Maps to env var `APP_ROLES`

### Step 5.2 — Add unique constraint to Role.name

- In `Role.java`, add `unique = true` to the `@Column(name = "NAME")` annotation to prevent duplicate roles at DB level

### Step 5.3 — Create RoleInitializer component

- New class: `src/main/java/com/uynguyen/aegis_id/role/RoleInitializer.java`
- Implements `ApplicationRunner`
- Reads `app.security.roles` property (list of strings)
- For each role name:
    - Auto-prefix with `ROLE_` (e.g., input `ADMIN` → stored as `ROLE_ADMIN`)
    - Check if role exists via `RoleRepository.findByName()`
    - If not, create and save it
- Log each created/skipped role at INFO level
- If `app.security.roles` is empty/missing, do nothing (no-op)

### Step 5.4 — Role lifecycle: adding and removing roles

**Adding a role:**

- Add the new name to `APP_ROLES` env var and redeploy
- `RoleInitializer` creates the new role; existing roles are untouched (idempotent)
- No downtime required — the new role is available immediately after startup

**Removing a role:**

- `RoleInitializer` is **additive-only** — it never deletes roles from the DB
- This is intentional: deleting a role would cascade to `USERS_ROLES`, silently stripping permissions from users
- To decommission a role:
    1. Remove it from `APP_ROLES` (prevents re-creation on future deploys)
    2. Use the admin endpoint (Phase 5) to unassign it from affected users
    3. Optionally delete the orphaned role row via a DB migration or manual operation
- **Why not auto-sync/delete?** A typo in `APP_ROLES` (e.g., accidentally omitting `ADMIN`) would wipe that role from all users on next deploy. Additive-only is the safe default.

### Step 5.5 — Bootstrap: first admin user

- Add `app.security.admin-emails` config property (maps to env var `APP_ADMIN_EMAILS`)
- Comma-separated list of email addresses that should receive the admin role on startup
- `RoleInitializer` (after creating roles) looks up these users by email:
    - If the user exists and doesn't already have the admin role → assign it
    - If the user doesn't exist yet → skip (they'll need to be assigned later via admin endpoint)
    - Admin role name is determined by convention: uses the first role in `APP_ROLES` that contains `ADMIN`, or a separate `app.security.admin-role` property if explicit control is needed
- This solves the **chicken-and-egg problem**: admin endpoints (Phase 6) require an admin, but no admin exists without this bootstrap
- Log assignments at INFO level

### Step 5.6 — Tests

- Test: roles are created on startup when `app.security.roles` is configured
- Test: existing roles are not duplicated
- Test: no-op when property is empty/missing
- Test: ROLE\_ prefix is applied
- Test: admin bootstrap assigns admin role to configured emails
- Test: admin bootstrap skips non-existent users gracefully

### Step 5.7 — Verification

1. Start app with `APP_ROLES=USER,ADMIN` → verify `ROLE_USER` and `ROLE_ADMIN` appear in ROLES table
2. Start app with no `APP_ROLES` → no roles created, no errors
3. Add `CREATOR` to `APP_ROLES` and restart → `ROLE_CREATOR` created, existing roles untouched
4. Remove `USER` from `APP_ROLES` and restart → `ROLE_USER` still exists in DB (additive-only)
5. Set `APP_ADMIN_EMAILS=admin@example.com` with `APP_ROLES=ADMIN` → register that user, restart, verify admin role assigned

---

## Phase 6: Admin Endpoints for User-Role Assignment

### Step 6.1 — Add `RoleService`

- New class: `src/main/java/com/uynguyen/aegis_id/role/RoleService.java` (interface) + `impl/RoleServiceImpl.java`
- Methods:
    - `List<Role> findAllByNames(List<String> names)` — bulk lookup, throws if any name is invalid
    - `List<Role> findAll()` — for listing available roles
- Used by the admin endpoint and `RoleInitializer`

### Step 6.2 — Add role assignment endpoints

- Add to `UserController` (or a new `AdminController` if separation is preferred):
    - **`PUT /api/v1/users/{userId}/roles`** — Replace all roles for a user. Request body: `{ "roles": ["ROLE_ADMIN", "ROLE_USER"] }`. Validates each role exists in the DB before assigning.
    - **`GET /api/v1/users/{userId}/roles`** — List roles assigned to a user.
- All endpoints protected with `@PreAuthorize("hasRole('ADMIN')")`
- Returns `404` if user or role not found
- Returns `403` if caller is not an admin

### Step 6.3 — Self-demotion guard

- Prevent an admin from removing their own admin role via this endpoint
- If `userId` matches the authenticated user and the request removes the admin role → reject with `400 Bad Request`
- This prevents accidental lockout (last admin removes their own access)

### Step 6.4 — Update SecurityConfig

- `@EnableMethodSecurity` is already present
- No URL-level role checks needed — `@PreAuthorize` on the controller methods is sufficient
- Ensure `JwtFilter` / `User.getAuthorities()` correctly populates `ROLE_ADMIN` so `hasRole('ADMIN')` works

### Step 6.5 — Tests

- Test: admin can assign roles to a user
- Test: admin can list a user's roles
- Test: non-admin gets `403`
- Test: assigning a non-existent role returns `400`
- Test: self-demotion guard rejects removing own admin role
- Test: assigning roles to non-existent user returns `404`

### Step 6.6 — Verification

1. As admin, `PUT /api/v1/users/{userId}/roles` → roles are updated
2. As non-admin, attempt same endpoint → `403`
3. As admin, attempt to remove own admin role → `400` rejection

---

## RoleController Decision: DO NOT CREATE

**Rationale:**

1. **No current consumer** — No `@PreAuthorize`, `@Secured`, or `@RolesAllowed` annotations exist; SecurityConfig only checks `authenticated()` vs `permitAll()`. Roles aren't enforced anywhere yet.
2. **Attack surface** — A RoleController with DELETE/PUT exposes:
    - Role deletion that cascades to user-role mappings (users silently lose permissions)
    - Role creation that could be exploited if authorization is misconfigured
    - Role modification that could elevate privileges
3. **Roles are infrastructure, not user data** — They define what the system recognizes. Env-var seeding is the right approach: roles change when the deployment config changes, not via API calls.
4. **If needed later** — Create a read-only `GET /api/v1/roles` endpoint (admin-only) for UI consumption. Never expose write operations for roles via API in an identity service.

---

## Relevant Files

- `src/main/java/com/uynguyen/aegis_id/security/JwtService.java` — toggle roles claim on/off ✅, refactor to `TokenUserInfo`-based API + email claim
- `src/main/java/com/uynguyen/aegis_id/security/TokenUserInfo.java` — **NEW**: record grouping all user data for token generation
- `src/main/java/com/uynguyen/aegis_id/security/JwtConfigEndpoint.java` — **NEW** ✅: Actuator endpoint for runtime toggle
- `src/main/java/com/uynguyen/aegis_id/auth/impl/AuthenticationServiceImpl.java` — remove ROLE_USER hard-coding from `register()` and pass full name into token generation
- `src/main/java/com/uynguyen/aegis_id/role/Role.java` — add unique constraint on `name`
- `src/main/java/com/uynguyen/aegis_id/role/RoleRepository.java` — may add `existsByName()`, `findAllByNameIn()` queries
- `src/main/java/com/uynguyen/aegis_id/role/RoleInitializer.java` — **NEW**: ApplicationRunner to seed roles + bootstrap admin
- `src/main/java/com/uynguyen/aegis_id/role/RoleService.java` — **NEW**: interface for role operations
- `src/main/java/com/uynguyen/aegis_id/role/impl/RoleServiceImpl.java` — **NEW**: role service implementation
- `src/main/java/com/uynguyen/aegis_id/user/UserController.java` — add admin endpoints for user-role assignment (or new `AdminController`)
- `src/main/resources/application.yml` — add `app.security.jwt.include-roles-claim` ✅, `app.security.roles`, `app.security.admin-emails`
- `src/test/java/com/uynguyen/aegis_id/security/JwtServiceTest.java` — add toggle tests ✅, full-name-claim tests, migrate to `TokenUserInfo`-based API, add email claim tests
- `src/test/java/com/uynguyen/aegis_id/auth/impl/AuthenticationServiceImplTest.java` — update registration tests, verify `TokenUserInfo` arguments with `ArgumentCaptor`
- `src/test/java/com/uynguyen/aegis_id/role/RoleInitializerTest.java` — **NEW**: tests for role seeding + admin bootstrap
- `src/test/java/com/uynguyen/aegis_id/user/UserControllerTest.java` — add tests for admin role-assignment endpoints

## Decisions

- **No RoleController** — roles are managed via env vars, not API
- **No roles on registration** — users start with zero roles
- **ROLE\_ auto-prefix** — input `ADMIN` → stored as `ROLE_ADMIN`
- **JWT roles claim defaults to OFF** — opt-in via `JWT_INCLUDE_ROLES_CLAIM=true` env var; runtime-toggleable via Actuator endpoint on management port
- **Full-name claim in JWT** — include normalized `full_name` in access and refresh tokens for client display convenience
- **No backward-compat JWT API layer** — removed 2-arg `generateAccessToken` / `generateRefreshToken` overloads; `TokenUserInfo`-based signature is now the single contract
- **`TokenUserInfo` record for token generation** — eliminates parameter explosion; adding a new claim is ~5 lines of production code with no signature cascading
- **Email claim in JWT** — always included when non-null (not toggleable — email is a core identity field)
- **Toggle lives in JwtService** — `AtomicBoolean` for thread-safe runtime switching; AuthenticationServiceImpl always passes roles; JwtService decides whether to embed them
- **Management port separation** — Actuator endpoints served on port 8081, not exposed through main API or JWT auth
- **Additive-only role seeding** — RoleInitializer never deletes roles; removal is a manual/migration operation
- **Admin bootstrap via env var** — `APP_ADMIN_EMAILS` assigns admin role to specified users on startup
- **Admin endpoints for user-role assignment** — `PUT /api/v1/users/{userId}/roles` protected by `@PreAuthorize("hasRole('ADMIN')")`
- **Self-demotion guard** — admins cannot remove their own admin role via API
