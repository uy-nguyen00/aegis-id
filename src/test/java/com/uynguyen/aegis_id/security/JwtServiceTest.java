package com.uynguyen.aegis_id.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.uynguyen.aegis_id.exception.BusinessException;
import com.uynguyen.aegis_id.exception.ErrorCode;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("JwtService Unit Tests")
class JwtServiceTest {

    private static final String ISSUER = "test-issuer";
    private static final String AUDIENCE = "test-audience";

    private String privateKeyBase64;
    private String publicKeyBase64;
    private JwtService jwtService;

    @BeforeEach
    void setUp() throws NoSuchAlgorithmException {
        final KeyPair keyPair = generateRsaKeyPair();
        this.privateKeyBase64 = Base64.getEncoder().encodeToString(
            keyPair.getPrivate().getEncoded()
        );
        this.publicKeyBase64 = Base64.getEncoder().encodeToString(
            keyPair.getPublic().getEncoded()
        );

        this.jwtService = new JwtService(
            this.privateKeyBase64,
            this.publicKeyBase64
        );
        ReflectionTestUtils.setField(
            this.jwtService,
            "accessTokenExpiration",
            60_000L
        );
        ReflectionTestUtils.setField(
            this.jwtService,
            "refreshTokenExpiration",
            120_000L
        );
        ReflectionTestUtils.setField(this.jwtService, "issuer", ISSUER);
        ReflectionTestUtils.setField(this.jwtService, "audience", AUDIENCE);
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName(
            "Should throw IllegalArgumentException when private key is invalid"
        )
        void shouldThrowWhenPrivateKeyIsInvalid() {
            final IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new JwtService("not-base64", publicKeyBase64)
            );

            assertTrue(
                exception.getMessage().contains("app.security.jwt.private-key")
            );
        }

        @Test
        @DisplayName(
            "Should throw IllegalArgumentException when public key is invalid"
        )
        void shouldThrowWhenPublicKeyIsInvalid() {
            final IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new JwtService(privateKeyBase64, "not-base64")
            );

            assertTrue(
                exception.getMessage().contains("app.security.jwt.public-key")
            );
        }
    }

    @Nested
    @DisplayName("Token Operations Tests")
    class TokenOperationsTests {

        @Test
        @DisplayName(
            "Should generate and validate access token with expected claims"
        )
        void shouldGenerateAndValidateAccessToken() {
            final List<String> roles = List.of("ROLE_USER", "ROLE_ADMIN");
            final String userId = "0195f40d-c748-7f9a-a819-cd664f4f4d41";
            final String otherUserId = "0195f40e-1f9d-7c86-9c2a-cf7e8aefab33";

            final String accessToken = jwtService.generateAccessToken(
                userId,
                roles
            );

            assertNotNull(accessToken);
            assertTrue(jwtService.isTokenValid(accessToken, userId));
            assertFalse(jwtService.isTokenValid(accessToken, otherUserId));
            assertEquals(
                userId,
                jwtService.extractUserIdFromToken(accessToken)
            );
            assertEquals(roles, jwtService.extractRolesFromToken(accessToken));
        }

        @Test
        @DisplayName("Should refresh access token from a valid refresh token")
        void shouldRefreshAccessToken() {
            final List<String> roles = List.of("ROLE_USER");
            final String userId = "0195f40f-56bd-78fd-b74d-a0eb9aa48612";
            final String refreshToken = jwtService.generateRefreshToken(
                userId,
                roles
            );

            final String newAccessToken = jwtService.refreshAccessToken(
                refreshToken
            );

            assertNotNull(newAccessToken);
            assertTrue(jwtService.isTokenValid(newAccessToken, userId));
            assertEquals(
                roles,
                jwtService.extractRolesFromToken(newAccessToken)
            );
        }

        @Test
        @DisplayName(
            "Should throw BusinessException when refreshing with access token"
        )
        void shouldThrowWhenRefreshingWithAccessToken() {
            final String accessToken = jwtService.generateAccessToken(
                "0195f410-0482-7f7c-9c5c-b93578cb2a9d",
                List.of("ROLE_USER")
            );

            final BusinessException exception = assertThrows(
                BusinessException.class,
                () -> jwtService.refreshAccessToken(accessToken)
            );

            assertEquals(ErrorCode.INVALID_JWT_TOKEN, exception.getErrorCode());
        }

        @Test
        @DisplayName(
            "Should throw BusinessException when refresh token is malformed"
        )
        void shouldThrowWhenRefreshTokenIsMalformed() {
            final BusinessException exception = assertThrows(
                BusinessException.class,
                () -> jwtService.refreshAccessToken("malformed-token")
            );

            assertEquals(ErrorCode.INVALID_JWT_TOKEN, exception.getErrorCode());
        }

        @Test
        @DisplayName(
            "Should throw BusinessException when refresh token is expired"
        )
        void shouldThrowWhenRefreshTokenIsExpired() {
            final JwtService expiredJwtService = new JwtService(
                privateKeyBase64,
                publicKeyBase64
            );
            ReflectionTestUtils.setField(
                expiredJwtService,
                "accessTokenExpiration",
                60_000L
            );
            ReflectionTestUtils.setField(
                expiredJwtService,
                "refreshTokenExpiration",
                -1L
            );
            ReflectionTestUtils.setField(expiredJwtService, "issuer", ISSUER);
            ReflectionTestUtils.setField(
                expiredJwtService,
                "audience",
                AUDIENCE
            );

            final String expiredRefreshToken =
                expiredJwtService.generateRefreshToken(
                    "0195f411-6937-75b4-aea2-a0820d643fe7",
                    List.of("ROLE_USER")
                );

            final BusinessException exception = assertThrows(
                BusinessException.class,
                () -> expiredJwtService.refreshAccessToken(expiredRefreshToken)
            );

            assertEquals(ErrorCode.INVALID_JWT_TOKEN, exception.getErrorCode());
        }
    }

    private KeyPair generateRsaKeyPair() throws NoSuchAlgorithmException {
        final KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(
            "RSA"
        );
        keyPairGenerator.initialize(2048);
        return keyPairGenerator.generateKeyPair();
    }
}
