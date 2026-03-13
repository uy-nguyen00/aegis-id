## Plan: SAS-First Multi-Client Auth With Social Readiness (Optimized)

This plan assumes no backward-compatibility requirements.

**Steps**

1. Phase 0 - Platform baseline (SAS first): add Spring Authorization Server as the only OAuth2/OIDC engine and lock a compatible Spring Boot + SAS version matrix before implementation.
2. Phase 0 - Identity foundation: introduce an ExternalIdentity model linked to User with unique (provider, providerUserId). Provider enum starts with LOCAL, GOOGLE, MICROSOFT, APPLE, GITHUB.
3. Phase 0 - User model hardening: use immutable userId as the canonical subject source; remove strict nullable=false assumptions for PASSWORD and PHONE_NUMBER to support social-only profiles.
4. Phase 0 - Principal contract: change UserDetails principal identity so getUsername() resolves to immutable userId; local credential login may still use email as input identifier, but principal name in security context and token subject is userId.
5. Phase 0 - Explicit linking policy: account linking is never auto-triggered by email match; linking requires an authenticated local session, step-up authentication, and explicit user consent confirmation.
6. Phase 1 - Token schema standardization: hard cutover to sub=userId, include email only as non-authoritative convenience claim, and standardize claims for client_id, azp, scope, jti, and auth_context.
7. Phase 1 - Decommission custom JWT stack immediately: delete custom JwtService, JwtFilter, and custom password-style /api/v1/auth/login flow once SAS endpoints are active.
8. Phase 1 - Security pipeline alignment: rely on SAS + resource server JWT processing and remove legacy email-subject fallback paths entirely.
9. Phase 1 - Client model and scopes: register web/mobile/service clients in SAS with explicit grant types, redirect URIs, PKCE requirements, and allowed scopes.
10. Phase 2 - Flow standardization: move first-party browser/mobile apps to authorization-code + PKCE and internal services to client_credentials; do not reintroduce password grant style APIs.
11. Phase 2 - Revocation and token state: store refresh token metadata (jti hash, principal, client, expiresAt, revokedAt) and enforce client-scoped revocation.
12. Phase 3 - Social readiness scaffolding: add provider adapter interfaces, callback/state/nonce primitives, and secure link/unlink endpoints.
13. Phase 3 - Strict provider feature flags: gate each provider adapter with @ConditionalOnProperty and explicit settings like aegis.auth.providers.github.enabled=false by default.
14. Phase 4 - Social provider rollout (deferred): implement Google/Microsoft/Apple/GitHub adapters, validation against provider JWKS/discovery metadata, and step-up + explicit-consent link/unlink UX.
15. Phase 4 - Operational controls: per-client rate limits, secret/key rotation playbooks, provider outage handling, and comprehensive auth audit trails.
16. Phase 5 - Documentation and verification: publish OAuth2/OIDC usage docs and run full end-to-end matrix for LOCAL plus linked social identities.

**Relevant files**

- /home/uynguyen/Code/aegis-id/pom.xml - add SAS/resource-server dependencies and pin compatible versions.
- /home/uynguyen/Code/aegis-id/src/main/resources/application.yml - configure SAS issuer, OIDC, client settings, feature flags, and provider placeholders.
- /home/uynguyen/Code/aegis-id/src/main/java/com/uynguyen/aegis_id/security/SecurityConfig.java - resource-server security and endpoint access policy alignment.
- /home/uynguyen/Code/aegis-id/src/main/java/com/uynguyen/aegis_id/security/JwtService.java - remove from runtime path after SAS cutover.
- /home/uynguyen/Code/aegis-id/src/main/java/com/uynguyen/aegis_id/security/JwtFilter.java - remove from runtime path after SAS cutover.
- /home/uynguyen/Code/aegis-id/src/main/java/com/uynguyen/aegis_id/user/User.java - make password/phone optional and keep immutable id as canonical identity key.
- /home/uynguyen/Code/aegis-id/src/main/java/com/uynguyen/aegis_id/user/UserRepository.java - add id-centric and external-identity-centric lookup paths.
- /home/uynguyen/Code/aegis-id/src/main/java/com/uynguyen/aegis_id/user/impl/UserServiceImpl.java - migrate principal loading logic from email-centric assumptions.
- /home/uynguyen/Code/aegis-id/src/main/java/com/uynguyen/aegis_id/auth/AuthenticationController.java - keep only app-specific endpoints (signup, recovery, link/unlink), not token endpoints.
- /home/uynguyen/Code/aegis-id/src/main/java/com/uynguyen/aegis_id/auth/impl/AuthenticationServiceImpl.java - retire password-style token issuance paths and align with SAS-driven flows.
- /home/uynguyen/Code/aegis-id/src/main/java/com/uynguyen/aegis_id/auth/request/AuthenticationRequest.java - keep only if local bootstrap/login flow remains outside SAS.
- /home/uynguyen/Code/aegis-id/src/main/java/com/uynguyen/aegis_id/auth/request/RefreshTokenRequest.java - keep only if non-standard refresh path remains.
- /home/uynguyen/Code/aegis-id/src/test/java/com/uynguyen/aegis_id/auth/AuthenticationControllerTest.java - update to SAS-driven contracts or retire tests for removed endpoints.
- /home/uynguyen/Code/aegis-id/src/test/java/com/uynguyen/aegis_id/auth/impl/AuthenticationServiceImplTest.java - align to id-centric principal model and explicit-linking policy.
- /home/uynguyen/Code/aegis-id/src/main/java/com/uynguyen/aegis_id/identity/ExternalIdentity.java - new entity for provider bindings.
- /home/uynguyen/Code/aegis-id/src/main/java/com/uynguyen/aegis_id/identity/ExternalIdentityRepository.java - new repository for provider identity resolution.
- /home/uynguyen/Code/aegis-id/src/main/java/com/uynguyen/aegis_id/security/AuthorizationServerConfig.java - new SAS configuration.
- /home/uynguyen/Code/aegis-id/src/main/java/com/uynguyen/aegis_id/auth/provider - new provider adapters gated by @ConditionalOnProperty.

**Verification**

1. Subject contract tests: every issued access token has sub=userId and never uses email as subject.
2. Identity tests: user records can exist without password/phone for social-only accounts.
3. External identity tests: provider+providerUserId uniqueness is enforced and account linking requires explicit consent.
4. OAuth2/OIDC tests: well-known metadata, JWKS, authorization-code+PKCE, refresh, and client_credentials all work through SAS.
5. Revocation tests: revoked refresh token and cross-client token reuse are rejected deterministically.
6. Security tests: account takeover scenarios through email collision are blocked by explicit-linking policy.
7. Decommission tests: no requests pass through custom JwtFilter/JwtService and custom /api/v1/auth/login is removed.
8. Step-up tests: linking endpoint always requires fresh authentication before persisting ExternalIdentity.
9. Feature-flag tests: disabled providers do not load adapter beans and their endpoints are unavailable.

**Decisions**

- We are dropping backward-compatibility and migration shims (no grace/fallback behavior needed for this side-project).
- We are standardizing early on SAS instead of extending custom OAuth2/JWT infrastructure.
- We are decommissioning custom JWT issuance/filtering immediately after SAS cutover to avoid dual-stack risks.
- We are enforcing step-up authentication for account linking and explicit consent for all social bindings.
- We are gating every provider with @ConditionalOnProperty and disabling providers by default.

**Further Considerations**

1. SAS handles OAuth2/OIDC protocol endpoints, but app-owned flows still remain in application controllers (signup, account recovery, linking).
2. Ensure local authentication UX still supports email as login identifier while Principal.getName() and token sub remain userId.
3. Keep adapter contracts provider-agnostic so adding new providers does not require security-chain rewrites.
