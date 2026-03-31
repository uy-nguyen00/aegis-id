# Plan: Decouple User from Role

## TL;DR

Decouple User and Role so registration doesn't require roles, seed roles from environment variables on startup (auto-prefixed with `ROLE_`), make JWT roles claims toggleable via env var, provide admin endpoints for user-role assignment, and skip creating a RoleController for role CRUD (security risk with no current benefit).

**Phase order** — prioritized by isolation and risk (safest first):

1. **JWT roles claim toggle** — zero runtime impact (JwtFilter already loads roles from DB, not JWT)
2. **Decouple registration** — removes hard `ROLE_USER` requirement
3. **Role seeding from env vars** — new feature, no dependency on 1-2
4. **Admin endpoints** — depends on roles being seeded

Each phase includes its own tests.

---

## Phase 1: Make JWT Roles Claim Toggleable ✅ DONE

> **Why first?** `JwtFilter` already ignores JWT roles claims — it loads authorities from the database. The `roles` claim in the JWT is dead weight. Toggling it off changes nothing about how requests are authorized. This is the safest, most isolated change.

**Implementation summary:**

- Added `app.security.jwt.include-roles-claim` property (default: `false`, env var: `JWT_INCLUDE_ROLES_CLAIM`)
- `JwtService` uses `AtomicBoolean` for thread-safe runtime toggling via getter/setter
- `generateAccessToken()` / `generateRefreshToken()` conditionally include `roles` claim
- `extractRolesFromToken()` returns empty list when claim is absent
- `refreshAccessToken()` handles null roles gracefully
- Custom Actuator endpoint `JwtConfigEndpoint` at `/actuator/jwtconfig` (read + write) for runtime toggle
- Management server runs on separate port (`MANAGEMENT_PORT`, default: `8081`) — not exposed through main API, secured at network level
- Dockerfile updated: health check now targets management port `8081`
- 5 new tests in `JwtServiceTest` (`RolesClaimToggleTests` nested class)
- All 146 tests pass

**Files changed:**

- `src/main/java/com/uynguyen/aegis_id/security/JwtService.java`
- `src/main/java/com/uynguyen/aegis_id/security/JwtConfigEndpoint.java` — **NEW**
- `src/main/resources/application.yml`
- `src/test/resources/application-dev.yml`
- `src/test/java/com/uynguyen/aegis_id/security/JwtServiceTest.java`
- `Dockerfile`

---

## Phase 2: Decouple Registration from Role

### Step 2.1 — Remove hard-coded role assignment from registration

- In `AuthenticationServiceImpl.register()`:
    - Remove the `roleRepository.findByName("ROLE_USER")` lookup
    - Remove the role list creation and `user.setRoles(roles)`
    - Let user be saved with an empty roles list (or null → empty)
    - Remove the `RoleRepository` dependency from `AuthenticationServiceImpl` if no longer needed

### Step 2.2 — Verify User.getAuthorities() handles empty roles

- Already confirmed: `User.getAuthorities()` returns `List.of()` when roles is empty — no change needed

### Step 2.3 — Tests

- Remove test that expects `EntityNotFoundException` when `ROLE_USER` doesn't exist
- Update `register_successfully` test to not mock `roleRepository.findByName()`
- Verify user is saved without roles

### Step 2.4 — Verification

1. Register a user → saved with empty roles
2. Login with that user → succeeds (JWT issued, no roles in token if toggle is off)
3. All existing tests pass

---

## Phase 3: Role Seeding from Environment Variables

### Step 3.1 — Add configuration properties

- In `application.yml`, add:
    - `app.security.roles` — comma-separated list of role names (e.g., `USER,ADMIN` or `CREATOR,MEMBER`)
- Maps to env var `APP_ROLES`

### Step 3.2 — Add unique constraint to Role.name

- In `Role.java`, add `unique = true` to the `@Column(name = "NAME")` annotation to prevent duplicate roles at DB level

### Step 3.3 — Create RoleInitializer component

- New class: `src/main/java/com/uynguyen/aegis_id/role/RoleInitializer.java`
- Implements `ApplicationRunner`
- Reads `app.security.roles` property (list of strings)
- For each role name:
    - Auto-prefix with `ROLE_` (e.g., input `ADMIN` → stored as `ROLE_ADMIN`)
    - Check if role exists via `RoleRepository.findByName()`
    - If not, create and save it
- Log each created/skipped role at INFO level
- If `app.security.roles` is empty/missing, do nothing (no-op)

### Step 3.4 — Role lifecycle: adding and removing roles

**Adding a role:**

- Add the new name to `APP_ROLES` env var and redeploy
- `RoleInitializer` creates the new role; existing roles are untouched (idempotent)
- No downtime required — the new role is available immediately after startup

**Removing a role:**

- `RoleInitializer` is **additive-only** — it never deletes roles from the DB
- This is intentional: deleting a role would cascade to `USERS_ROLES`, silently stripping permissions from users
- To decommission a role:
    1. Remove it from `APP_ROLES` (prevents re-creation on future deploys)
    2. Use the admin endpoint (Phase 4) to unassign it from affected users
    3. Optionally delete the orphaned role row via a DB migration or manual operation
- **Why not auto-sync/delete?** A typo in `APP_ROLES` (e.g., accidentally omitting `ADMIN`) would wipe that role from all users on next deploy. Additive-only is the safe default.

### Step 3.5 — Bootstrap: first admin user

- Add `app.security.admin-emails` config property (maps to env var `APP_ADMIN_EMAILS`)
- Comma-separated list of email addresses that should receive the admin role on startup
- `RoleInitializer` (after creating roles) looks up these users by email:
    - If the user exists and doesn't already have the admin role → assign it
    - If the user doesn't exist yet → skip (they'll need to be assigned later via admin endpoint)
    - Admin role name is determined by convention: uses the first role in `APP_ROLES` that contains `ADMIN`, or a separate `app.security.admin-role` property if explicit control is needed
- This solves the **chicken-and-egg problem**: admin endpoints (Phase 4) require an admin, but no admin exists without this bootstrap
- Log assignments at INFO level

### Step 3.6 — Tests

- Test: roles are created on startup when `app.security.roles` is configured
- Test: existing roles are not duplicated
- Test: no-op when property is empty/missing
- Test: ROLE\_ prefix is applied
- Test: admin bootstrap assigns admin role to configured emails
- Test: admin bootstrap skips non-existent users gracefully

### Step 3.7 — Verification

1. Start app with `APP_ROLES=USER,ADMIN` → verify `ROLE_USER` and `ROLE_ADMIN` appear in ROLES table
2. Start app with no `APP_ROLES` → no roles created, no errors
3. Add `CREATOR` to `APP_ROLES` and restart → `ROLE_CREATOR` created, existing roles untouched
4. Remove `USER` from `APP_ROLES` and restart → `ROLE_USER` still exists in DB (additive-only)
5. Set `APP_ADMIN_EMAILS=admin@example.com` with `APP_ROLES=ADMIN` → register that user, restart, verify admin role assigned

---

## Phase 4: Admin Endpoints for User-Role Assignment

### Step 4.1 — Add `RoleService`

- New class: `src/main/java/com/uynguyen/aegis_id/role/RoleService.java` (interface) + `impl/RoleServiceImpl.java`
- Methods:
    - `List<Role> findAllByNames(List<String> names)` — bulk lookup, throws if any name is invalid
    - `List<Role> findAll()` — for listing available roles
- Used by the admin endpoint and `RoleInitializer`

### Step 4.2 — Add role assignment endpoints

- Add to `UserController` (or a new `AdminController` if separation is preferred):
    - **`PUT /api/v1/users/{userId}/roles`** — Replace all roles for a user. Request body: `{ "roles": ["ROLE_ADMIN", "ROLE_USER"] }`. Validates each role exists in the DB before assigning.
    - **`GET /api/v1/users/{userId}/roles`** — List roles assigned to a user.
- All endpoints protected with `@PreAuthorize("hasRole('ADMIN')")`
- Returns `404` if user or role not found
- Returns `403` if caller is not an admin

### Step 4.3 — Self-demotion guard

- Prevent an admin from removing their own admin role via this endpoint
- If `userId` matches the authenticated user and the request removes the admin role → reject with `400 Bad Request`
- This prevents accidental lockout (last admin removes their own access)

### Step 4.4 — Update SecurityConfig

- `@EnableMethodSecurity` is already present
- No URL-level role checks needed — `@PreAuthorize` on the controller methods is sufficient
- Ensure `JwtFilter` / `User.getAuthorities()` correctly populates `ROLE_ADMIN` so `hasRole('ADMIN')` works

### Step 4.5 — Tests

- Test: admin can assign roles to a user
- Test: admin can list a user's roles
- Test: non-admin gets `403`
- Test: assigning a non-existent role returns `400`
- Test: self-demotion guard rejects removing own admin role
- Test: assigning roles to non-existent user returns `404`

### Step 4.6 — Verification

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

- `src/main/java/com/uynguyen/aegis_id/security/JwtService.java` — toggle roles claim on/off ✅
- `src/main/java/com/uynguyen/aegis_id/security/JwtConfigEndpoint.java` — **NEW** ✅: Actuator endpoint for runtime toggle
- `src/main/java/com/uynguyen/aegis_id/auth/impl/AuthenticationServiceImpl.java` — remove ROLE_USER hard-coding from `register()`
- `src/main/java/com/uynguyen/aegis_id/role/Role.java` — add unique constraint on `name`
- `src/main/java/com/uynguyen/aegis_id/role/RoleRepository.java` — may add `existsByName()`, `findAllByNameIn()` queries
- `src/main/java/com/uynguyen/aegis_id/role/RoleInitializer.java` — **NEW**: ApplicationRunner to seed roles + bootstrap admin
- `src/main/java/com/uynguyen/aegis_id/role/RoleService.java` — **NEW**: interface for role operations
- `src/main/java/com/uynguyen/aegis_id/role/impl/RoleServiceImpl.java` — **NEW**: role service implementation
- `src/main/java/com/uynguyen/aegis_id/user/UserController.java` — add admin endpoints for user-role assignment (or new `AdminController`)
- `src/main/resources/application.yml` — add `app.security.jwt.include-roles-claim` ✅, `app.security.roles`, `app.security.admin-emails`
- `src/test/java/com/uynguyen/aegis_id/security/JwtServiceTest.java` — add toggle tests ✅
- `src/test/java/com/uynguyen/aegis_id/auth/impl/AuthenticationServiceImplTest.java` — update registration tests
- `src/test/java/com/uynguyen/aegis_id/role/RoleInitializerTest.java` — **NEW**: tests for role seeding + admin bootstrap
- `src/test/java/com/uynguyen/aegis_id/user/UserControllerTest.java` — add tests for admin role-assignment endpoints

## Decisions

- **No RoleController** — roles are managed via env vars, not API
- **No roles on registration** — users start with zero roles
- **ROLE\_ auto-prefix** — input `ADMIN` → stored as `ROLE_ADMIN`
- **JWT roles claim defaults to OFF** — opt-in via `JWT_INCLUDE_ROLES_CLAIM=true` env var; runtime-toggleable via Actuator endpoint on management port
- **Toggle lives in JwtService** — `AtomicBoolean` for thread-safe runtime switching; AuthenticationServiceImpl always passes roles; JwtService decides whether to embed them
- **Management port separation** — Actuator endpoints served on port 8081, not exposed through main API or JWT auth
- **Additive-only role seeding** — RoleInitializer never deletes roles; removal is a manual/migration operation
- **Admin bootstrap via env var** — `APP_ADMIN_EMAILS` assigns admin role to specified users on startup
- **Admin endpoints for user-role assignment** — `PUT /api/v1/users/{userId}/roles` protected by `@PreAuthorize("hasRole('ADMIN')")`
- **Self-demotion guard** — admins cannot remove their own admin role via API
