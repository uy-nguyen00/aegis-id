# Checklist for microservices setup

## Auth model & boundaries

- [ ] Auth service is clearly bounded: handles registration, login, token issuing/refresh, and *nothing* domain-specific.
- [ ] Users and credentials are stored in a database **owned** only by the Auth service (no cross-service shared tables).
- [ ] Access/refresh tokens are JWT (or opaque tokens) with clear claims: subject, roles/permissions, expiry, issuer, audience.
- [ ] Token TTLs and refresh policies are defined and externalized in configuration (per environment).
- [ ] JWT includes `iss` (issuer) and `aud` (audience) claims for downstream validation.
- [ ] Refresh token rotation implemented (old refresh token invalidated on use) to limit replay attacks.
- [ ] Token revocation strategy defined (blocklist in Redis/DB, or short-lived tokens only).

## Security & integration contracts

- [ ] A single, documented way for other services to validate tokens (JWKS endpoint, introspection endpoint, or shared public key).
- [ ] Clear contract for passing identity between services: HTTP `Authorization: Bearer <token>` and/or propagated security context for internal calls.
- [ ] Roles/scopes model defined (e.g., USER, ADMIN, SERVICE) and how downstream services read them from token claims.
- [ ] Login/registration/refresh endpoints are versioned and documented (OpenAPI/Swagger).

## Infrastructure & deployment readiness

- [X] Auth service is containerized (Dockerfile) and can run independently with its own config and database.
- [ ] Per-environment configuration is set up (profiles or config server): DB URL, secrets, token settings, CORS, etc.
- [ ] CI pipeline exists for Auth service: build, tests, image build, deploy to dev environment.
- [ ] Infrastructure decisions are made: API gateway yes/no, service discovery (Kubernetes services, Eureka, etc.), and how Auth is exposed.

## Kafka & event-driven concerns (for user-related events)

- [ ] Kafka cluster reachable from Auth service in all environments (dev/stage/prod) with topics defined (e.g., `user-registered`, `user-deleted`).
- [ ] Outbox or similar pattern decided for emitting user events atomically with DB writes (for registration, profile updates, etc.).
- [ ] Event schemas for user-related topics defined (Avro/JSON/Protobuf) and stable, with some form of schema governance if other services consume them.

## Observability, reliability & ops

- [ ] Central logging configured (correlation IDs, user ID or subject ID, request ID, and error details).
- [ ] Metrics and health checks in place: liveness/readiness endpoints, login/refresh error rates, token issuance counts, Kafka connectivity if used.
- [ ] Tracing configured so calls via gateway → Auth → other services can be correlated (traceId, spanId propagation).
- [ ] Backup/restore and DR story for the Auth database (since it is highly critical).

## Security hardening (Auth-specific)

- [ ] Password hashing uses a modern algorithm (bcrypt, scrypt, or Argon2) with appropriate cost factor.
- [ ] Rate limiting on login/registration endpoints (e.g., 5 failed attempts → temporary lockout or CAPTCHA).
- [ ] Account lockout or exponential backoff after repeated failed login attempts.
- [ ] Private key for JWT signing is stored securely (secrets manager, HSM, or encrypted volume) — not in repo or plaintext config.
- [ ] Key rotation strategy for asymmetric JWT keys (versioned `kid` claim, gradual rollover).
- [ ] Secure headers configured: `X-Content-Type-Options`, `X-Frame-Options`, `Strict-Transport-Security`.
- [ ] CORS policy explicitly whitelists allowed origins (no wildcard `*` in production).
- [ ] Input validation on all auth endpoints (email format, password complexity, username constraints).
- [ ] Password reset flow secured with short-lived, single-use tokens.
- [ ] MFA/2FA support planned or implemented (TOTP, SMS, email verification).
- [ ] Audit logging for security-sensitive events (login, logout, password change, failed attempts, role changes).

## Testing & quality gates

- [ ] Unit tests for token generation/validation, password hashing, and claim extraction.
- [ ] Integration tests for login/register/refresh flows (happy path + error cases).
- [ ] Security-focused tests: expired tokens, tampered tokens, invalid signatures, missing claims.
- [ ] Load/stress tests for auth endpoints to validate rate limiting and performance under load.
- [ ] Dependency vulnerability scanning (OWASP Dependency-Check, Snyk, Trivy).
- [ ] Static analysis for security issues (SpotBugs with FindSecBugs, SonarQube).

## API design & documentation

- [ ] Consistent error response format across all endpoints (error code, message, timestamp, traceId).
- [ ] API versioning strategy defined (path-based `/api/v1/` or header-based).
- [ ] OpenAPI spec includes authentication schemes and example requests/responses.
- [ ] Deprecation policy for old API versions documented.

## Secrets & configuration management

- [ ] No secrets in source code or docker-compose for production.
- [ ] Secrets injected via environment variables, Kubernetes secrets, or secrets manager (Vault, AWS Secrets Manager).
- [ ] Separate `.env` files or config per environment (dev/stage/prod) with different credentials.
- [ ] Database migrations managed with Flyway or Liquibase (not `ddl-auto: update` in production).
