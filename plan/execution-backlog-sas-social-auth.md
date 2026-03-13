# Execution Backlog: SAS-First Multi-Client Auth + Social Readiness

Scope: Greenfield execution with no backward-compatibility constraints.
Principles: Single auth stack (SAS only), immutable userId subject, explicit-consent linking, provider feature flags default OFF.

## PR-01: Lock Platform Baseline

Goal: Establish compatible Spring Boot + Spring Authorization Server baseline and bootstrap minimal authorization server wiring.

Changes:

- Update dependency management in pom.xml.
- Add SAS core dependencies and required security modules.
- Add initial AuthorizationServerConfig class.
- Add minimal issuer and auth-server settings in application.yml.
- Add startup smoke test that validates context loads with SAS configuration.

Acceptance criteria:

- Application starts with SAS config enabled.
- Well-known metadata and JWKS endpoints are reachable.
- Dependency versions are pinned and documented in README.

Test focus:

- Context load test.
- Endpoint smoke checks for discovery/JWKS.

## PR-02: Identity Foundation Schema

Goal: Introduce ExternalIdentity and harden user schema for social readiness.

Changes:

- Create ExternalIdentity entity and repository.
- Add provider enum with LOCAL, GOOGLE, MICROSOFT, APPLE, GITHUB.
- Add unique constraint on provider + providerUserId.
- Update User entity for nullable password and phone number.
- Add migration scripts for new table and column nullability changes.

Acceptance criteria:

- User records can exist without password/phone.
- ExternalIdentity uniqueness is enforced at DB level.
- A LOCAL identity can be attached to a user.

Test focus:

- JPA repository tests for uniqueness and relations.
- Schema migration test in CI.

## PR-03: Principal Contract Hard Cutover

Goal: Make userId the canonical principal and token subject.

Changes:

- Update UserDetails principal behavior so principal name maps to immutable userId.
- Refactor user-loading paths to support id-centric principal resolution.
- Ensure local login may still accept email as credential input identifier.
- Add token customization so sub is always userId.
- Keep email as convenience claim only.

Acceptance criteria:

- All new access tokens have sub=userId.
- Principal.getName() equals userId in authenticated requests.
- Email is never used as canonical subject.

Test focus:

- Unit tests for principal mapping.
- Integration tests decoding issued token claims.

## PR-04: Decommission Custom JWT Stack

Goal: Remove dual-stack auth risk by deleting custom JWT/token pipeline.

Changes:

- Remove JwtService runtime usage.
- Remove JwtFilter runtime usage and registration.
- Remove custom password-style token endpoint (/api/v1/auth/login).
- Remove or refactor dependent service/controller code that issued custom tokens.
- Update SecurityConfig to rely on SAS + resource server JWT processing only.

Acceptance criteria:

- No request path uses custom JWT classes.
- OAuth2/OIDC flows use SAS endpoints only.
- Legacy auth endpoint is removed and tests updated.

Test focus:

- Security integration tests for SAS token validation.
- Negative test confirming legacy login endpoint is absent.

## PR-05: Client Registry + Scope Governance

Goal: Register first-party clients and enforce scope contracts centrally.

Changes:

- Add registered clients for web, mobile, and service-to-service use cases.
- Configure grant types and PKCE requirement for public clients.
- Configure client_credentials for confidential service clients.
- Define canonical scopes and enforce allowed-scopes policy.

Acceptance criteria:

- Public clients require PKCE.
- Service clients can use client_credentials only.
- Tokens contain expected client and scope claims.

Test focus:

- Authorization code + PKCE flow tests.
- Client credentials flow tests.
- Scope enforcement tests.

## PR-06: Token State + Revocation

Goal: Add deterministic refresh token lifecycle control.

Changes:

- Create refresh token state table (jti hash, principal, client, expiresAt, revokedAt).
- Add revocation service and client-scoped revocation behavior.
- Add logout/revocation endpoints where needed by product flows.
- Enforce rejection for revoked or cross-client token reuse.

Acceptance criteria:

- Revoked refresh tokens cannot mint new access tokens.
- Cross-client token replay is rejected.
- Revocation events are auditable.

Test focus:

- Integration tests for revoke-then-refresh failure.
- Cross-client replay negative tests.

## PR-07: Account Linking Security Layer

Goal: Implement explicit-consent and step-up linking primitives.

Changes:

- Add link/unlink domain services and endpoints.
- Require active authenticated session plus fresh step-up authentication before link.
- Add anti-CSRF/state validation for linking initiation.
- Persist consent record with timestamp and actor context.
- Explicitly block auto-link by email match.

Acceptance criteria:

- Linking fails without fresh step-up auth.
- Linking requires explicit user consent confirmation.
- Email-only match does not create links.

Test focus:

- Security tests for unattended-session abuse case.
- Link/unlink authorization and consent tests.

## PR-08: Provider Adapter Framework + Feature Flags

Goal: Prepare social provider rollout behind strict feature flags.

Changes:

- Create provider adapter interface and provider-specific stubs.
- Gate each provider bean with @ConditionalOnProperty.
- Add provider flags in application.yml with secure defaults OFF.
- Add callback/state/nonce scaffolding for provider flows.

Acceptance criteria:

- Disabled providers do not load adapter beans.
- Enabled provider only activates when explicitly configured.
- Provider rollout can be done per environment without code changes.

Test focus:

- Conditional bean loading tests.
- Endpoint availability tests by flag state.

## PR-09: App Endpoint Realignment

Goal: Keep only app-owned endpoints while SAS owns protocol endpoints.

Changes:

- Reduce AuthenticationController to app-specific concerns.
- Keep sign-up, account recovery, and account linking endpoints.
- Remove duplicated protocol behavior from app controllers.
- Update OpenAPI docs to reflect ownership boundaries.

Acceptance criteria:

- No overlap between app endpoints and SAS protocol endpoints.
- OpenAPI and README describe endpoint boundaries clearly.

Test focus:

- Controller tests for retained app endpoints.
- Contract tests for removed endpoint behavior.

## PR-10: Final Hardening + E2E Matrix

Goal: Validate production-level security behavior before enabling social providers.

Changes:

- Add full integration matrix for LOCAL + linked identities.
- Add audit logging checks for login, token issuance, revocation, linking.
- Add rate-limit and secret-rotation operational runbooks.
- Final documentation for local setup and environment flags.

Acceptance criteria:

- End-to-end auth flows pass across web/mobile/service clients.
- Security regressions are blocked by automated tests.
- Runbooks are complete for key rotation and provider outage handling.

Test focus:

- End-to-end matrix tests.
- Security-focused negative-path tests.

## Suggested Sequence and Size Targets

1. PR-01 to PR-04 are mandatory before product feature work.
2. PR-05 and PR-06 establish stable multi-client token governance.
3. PR-07 and PR-08 complete social-readiness security posture.
4. PR-09 and PR-10 finalize boundary cleanup and hardening.

Target size:

- PR-01 to PR-04: 300-600 LOC each where feasible.
- PR-05 to PR-08: 400-900 LOC each due to integration/testing overhead.
- PR-09 to PR-10: variable, prioritize test depth over LOC limits.
