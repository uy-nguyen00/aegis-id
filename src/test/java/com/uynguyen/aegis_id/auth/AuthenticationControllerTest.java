package com.uynguyen.aegis_id.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.uynguyen.aegis_id.auth.request.AuthenticationRequest;
import com.uynguyen.aegis_id.auth.request.AuthorizeRequest;
import com.uynguyen.aegis_id.auth.request.CodeExchangeRequest;
import com.uynguyen.aegis_id.auth.request.RefreshTokenRequest;
import com.uynguyen.aegis_id.auth.request.RegistrationRequest;
import com.uynguyen.aegis_id.auth.response.AuthenticationResponse;
import com.uynguyen.aegis_id.auth.response.AuthorizeResponse;
import com.uynguyen.aegis_id.exception.BusinessException;
import com.uynguyen.aegis_id.exception.ErrorCode;
import com.uynguyen.aegis_id.handler.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@ActiveProfiles("prod")
public class AuthenticationControllerTest {

    @Autowired
    private RestTestClient restTestClient;

    @MockitoBean
    private AuthenticationService authenticationService;

    private final String apiPrefix = "/api/v1/auth/";

    @BeforeEach
    void setUp() {}

    @Nested
    @DisplayName("/login Tests")
    class LoginEndpointTests {

        @Test
        @DisplayName("Should return tokens with valid credentials")
        void shouldReturnToken_WithValidCredentials() {
            AuthenticationRequest request = AuthenticationRequest.builder()
                .email("john.doe@example.com")
                .password("user-password")
                .build();
            AuthenticationResponse expectedResponse =
                AuthenticationResponse.builder()
                    .accessToken("access-token")
                    .refreshToken("refresh-token")
                    .tokenType("Bearer")
                    .build();

            when(
                authenticationService.login(any(AuthenticationRequest.class))
            ).thenReturn(expectedResponse);

            restTestClient
                .post()
                .uri(apiPrefix + "login")
                .body(request)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(AuthenticationResponse.class)
                .value(response -> {
                    assertNotNull(response);
                    assertEquals(
                        "access-token",
                        expectedResponse.getAccessToken()
                    );
                    assertEquals(
                        "refresh-token",
                        expectedResponse.getRefreshToken()
                    );
                    assertEquals("Bearer", expectedResponse.getTokenType());
                });
        }

        @Test
        @DisplayName(
            "Should return 401 Unauthorized when credentials are invalid"
        )
        void shouldReturnUnauthorized_WhenCredentialsAreInvalid() {
            AuthenticationRequest request = AuthenticationRequest.builder()
                .email("john.doe@example.com")
                .password("wrong-password")
                .build();

            when(
                authenticationService.login(any(AuthenticationRequest.class))
            ).thenThrow(new BadCredentialsException("Invalid credentials"));

            restTestClient
                .post()
                .uri(apiPrefix + "login")
                .body(request)
                .exchange()
                .expectStatus()
                .isUnauthorized();
        }

        @Test
        @DisplayName(
            "Should return 400 Bad Request when email format is invalid"
        )
        void shouldReturnBadRequest_WhenEmailIsInvalid() {
            AuthenticationRequest request = AuthenticationRequest.builder()
                .email("invalid-email-format")
                .password("password123")
                .build();

            restTestClient
                .post()
                .uri(apiPrefix + "login")
                .body(request)
                .exchange()
                .expectStatus()
                .isBadRequest();
        }

        @Test
        @DisplayName("Should return 400 Bad Request when password is empty")
        void shouldReturnBadRequest_WhenPasswordIsEmpty() {
            AuthenticationRequest request = AuthenticationRequest.builder()
                .email("john.doe@example.com")
                .password("")
                .build();

            restTestClient
                .post()
                .uri(apiPrefix + "login")
                .body(request)
                .exchange()
                .expectStatus()
                .isBadRequest();
        }
    }

    @Nested
    @DisplayName("/register Tests")
    class RegisterEndpointTests {

        @Test
        @DisplayName("Should create user with valid request")
        void shouldCreateUser_WithValidRequest() {
            RegistrationRequest request = RegistrationRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .phoneNumber("+1234567890")
                .password("Password123!")
                .confirmPassword("Password123!")
                .build();

            restTestClient
                .post()
                .uri(apiPrefix + "register")
                .body(request)
                .exchange()
                .expectStatus()
                .isCreated();
        }

        @Test
        @DisplayName("Should return 400 Bad Request when email is invalid")
        void shouldReturnBadRequest_WhenEmailIsInvalid() {
            RegistrationRequest request = RegistrationRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .email("invalid-email")
                .phoneNumber("+1234567890")
                .password("Password123!")
                .confirmPassword("Password123!")
                .build();

            restTestClient
                .post()
                .uri(apiPrefix + "register")
                .body(request)
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody(ErrorResponse.class)
                .value(response ->
                    assertEquals("VALIDATION_ERROR", response.getCode())
                );
        }

        @Test
        @DisplayName("Should return 400 Bad Request when password is too short")
        void shouldReturnBadRequest_WhenPasswordIsTooShort() {
            RegistrationRequest request = RegistrationRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .phoneNumber("+1234567890")
                .password("12")
                .confirmPassword("Password123!")
                .build();

            restTestClient
                .post()
                .uri(apiPrefix + "register")
                .body(request)
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody(ErrorResponse.class)
                .value(response ->
                    assertEquals("VALIDATION_ERROR", response.getCode())
                );
        }

        @Test
        @DisplayName(
            "Should return 400 Bad Request when passwords do not match"
        )
        void shouldReturnBadRequest_WhenPasswordsDoNotMatch() {
            RegistrationRequest request = RegistrationRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .phoneNumber("+1234567890")
                .password("Password123_")
                .confirmPassword("Password123!")
                .build();

            restTestClient
                .post()
                .uri(apiPrefix + "register")
                .body(request)
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody(ErrorResponse.class)
                .value(response ->
                    assertEquals("VALIDATION_ERROR", response.getCode())
                );
        }
    }

    @Nested
    @DisplayName("/refresh Tests")
    class RefreshEndpointTests {

        @Test
        @DisplayName("Should return tokens when refresh token is valid")
        void shouldReturnToken_WhenRefreshTokenIsValid() {
            RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken("valid-refresh-token")
                .build();
            AuthenticationResponse expectedResponse =
                AuthenticationResponse.builder()
                    .accessToken("new-access-token")
                    .refreshToken("new-refresh-token")
                    .tokenType("Bearer")
                    .build();

            when(
                authenticationService.refreshToken(
                    any(RefreshTokenRequest.class)
                )
            ).thenReturn(expectedResponse);

            restTestClient
                .post()
                .uri(apiPrefix + "refresh")
                .body(request)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(AuthenticationResponse.class)
                .value(response -> {
                    assertNotNull(response);
                    assertEquals(
                        "new-access-token",
                        expectedResponse.getAccessToken()
                    );
                    assertEquals(
                        "new-refresh-token",
                        expectedResponse.getRefreshToken()
                    );
                    assertEquals("Bearer", expectedResponse.getTokenType());
                });
        }

        @Test
        @DisplayName(
            "Should return 400 Bad Request when refresh token is blank"
        )
        void shouldReturnBadRequest_WhenRefreshTokenIsBlank() {
            RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken("")
                .build();

            restTestClient
                .post()
                .uri(apiPrefix + "refresh")
                .body(request)
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody(ErrorResponse.class)
                .value(response ->
                    assertEquals("VALIDATION_ERROR", response.getCode())
                );
        }

        @Test
        @DisplayName(
            "Should return 400 Bad Request when refresh token is invalid"
        )
        void shouldReturnBadRequest_WhenRefreshTokenIsInvalid() {
            RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken("invalid-token")
                .build();

            when(
                authenticationService.refreshToken(
                    any(RefreshTokenRequest.class)
                )
            ).thenThrow(new BusinessException(ErrorCode.INVALID_JWT_TOKEN));

            restTestClient
                .post()
                .uri(apiPrefix + "refresh")
                .body(request)
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody(ErrorResponse.class)
                .value(response ->
                    assertEquals(
                        ErrorCode.INVALID_JWT_TOKEN.getCode(),
                        response.getCode()
                    )
                );
        }
    }

    @Nested
    @DisplayName("/authorize Tests")
    class AuthorizeEndpointTests {

        @Test
        @DisplayName(
            "Should return code with valid credentials and redirect URI"
        )
        void shouldReturnCode_WithValidRequest() {
            AuthorizeRequest request = AuthorizeRequest.builder()
                .email("john.doe@example.com")
                .password("user-password")
                .redirectUri("http://localhost:3000/callback")
                .build();
            AuthorizeResponse expectedResponse = AuthorizeResponse.builder()
                .code("test-code")
                .redirectUri("http://localhost:3000/callback")
                .build();

            when(
                authenticationService.authorize(any(AuthorizeRequest.class))
            ).thenReturn(expectedResponse);

            restTestClient
                .post()
                .uri(apiPrefix + "authorize")
                .body(request)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(AuthorizeResponse.class)
                .value(response -> {
                    assertNotNull(response);
                    assertEquals("test-code", response.getCode());
                    assertEquals(
                        "http://localhost:3000/callback",
                        response.getRedirectUri()
                    );
                });
        }

        @Test
        @DisplayName("Should return 400 when redirect URI is blank")
        void shouldReturnBadRequest_WhenRedirectUriIsBlank() {
            AuthorizeRequest request = AuthorizeRequest.builder()
                .email("john.doe@example.com")
                .password("user-password")
                .redirectUri("")
                .build();

            restTestClient
                .post()
                .uri(apiPrefix + "authorize")
                .body(request)
                .exchange()
                .expectStatus()
                .isBadRequest();
        }

        @Test
        @DisplayName("Should return 400 when redirect URI is not allowed")
        void shouldReturnBadRequest_WhenRedirectUriNotAllowed() {
            AuthorizeRequest request = AuthorizeRequest.builder()
                .email("john.doe@example.com")
                .password("user-password")
                .redirectUri("https://evil.com/callback")
                .build();

            when(
                authenticationService.authorize(any(AuthorizeRequest.class))
            ).thenThrow(
                new BusinessException(
                    ErrorCode.INVALID_REDIRECT_URI,
                    "https://evil.com/callback"
                )
            );

            restTestClient
                .post()
                .uri(apiPrefix + "authorize")
                .body(request)
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody(ErrorResponse.class)
                .value(response ->
                    assertEquals(
                        ErrorCode.INVALID_REDIRECT_URI.getCode(),
                        response.getCode()
                    )
                );
        }

        @Test
        @DisplayName("Should return 401 when credentials are invalid")
        void shouldReturnUnauthorized_WhenCredentialsAreInvalid() {
            AuthorizeRequest request = AuthorizeRequest.builder()
                .email("john.doe@example.com")
                .password("wrong-password")
                .redirectUri("http://localhost:3000/callback")
                .build();

            when(
                authenticationService.authorize(any(AuthorizeRequest.class))
            ).thenThrow(new BadCredentialsException("Invalid credentials"));

            restTestClient
                .post()
                .uri(apiPrefix + "authorize")
                .body(request)
                .exchange()
                .expectStatus()
                .isUnauthorized();
        }
    }

    @Nested
    @DisplayName("/exchange Tests")
    class ExchangeEndpointTests {

        @Test
        @DisplayName("Should return tokens with valid code")
        void shouldReturnTokens_WithValidCode() {
            CodeExchangeRequest request = CodeExchangeRequest.builder()
                .code("valid-code")
                .build();
            AuthenticationResponse expectedResponse =
                AuthenticationResponse.builder()
                    .accessToken("access-token")
                    .refreshToken("refresh-token")
                    .tokenType("Bearer")
                    .build();

            when(
                authenticationService.exchangeCode(
                    any(CodeExchangeRequest.class)
                )
            ).thenReturn(expectedResponse);

            restTestClient
                .post()
                .uri(apiPrefix + "exchange")
                .body(request)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(AuthenticationResponse.class)
                .value(response -> {
                    assertNotNull(response);
                    assertEquals("access-token", response.getAccessToken());
                    assertEquals("refresh-token", response.getRefreshToken());
                    assertEquals("Bearer", response.getTokenType());
                });
        }

        @Test
        @DisplayName("Should return 400 when code is blank")
        void shouldReturnBadRequest_WhenCodeIsBlank() {
            CodeExchangeRequest request = CodeExchangeRequest.builder()
                .code("")
                .build();

            restTestClient
                .post()
                .uri(apiPrefix + "exchange")
                .body(request)
                .exchange()
                .expectStatus()
                .isBadRequest();
        }

        @Test
        @DisplayName("Should return 400 when code is invalid")
        void shouldReturnBadRequest_WhenCodeIsInvalid() {
            CodeExchangeRequest request = CodeExchangeRequest.builder()
                .code("invalid-code")
                .build();

            when(
                authenticationService.exchangeCode(
                    any(CodeExchangeRequest.class)
                )
            ).thenThrow(
                new BusinessException(ErrorCode.INVALID_AUTHORIZATION_CODE)
            );

            restTestClient
                .post()
                .uri(apiPrefix + "exchange")
                .body(request)
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody(ErrorResponse.class)
                .value(response ->
                    assertEquals(
                        ErrorCode.INVALID_AUTHORIZATION_CODE.getCode(),
                        response.getCode()
                    )
                );
        }
    }
}
