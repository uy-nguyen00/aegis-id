package com.uynguyen.aegis_id.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.uynguyen.aegis_id.auth.request.AuthenticationRequest;
import com.uynguyen.aegis_id.auth.request.RefreshTokenRequest;
import com.uynguyen.aegis_id.auth.request.RegistrationRequest;
import com.uynguyen.aegis_id.auth.response.AuthenticationResponse;
import com.uynguyen.aegis_id.exception.BusinessException;
import com.uynguyen.aegis_id.exception.ErrorCode;
import com.uynguyen.aegis_id.handler.ErrorResponse;
import com.uynguyen.aegis_id.testsupport.PostgresTestContainerConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@ActiveProfiles("prod")
@Import(PostgresTestContainerConfig.class)
class AuthenticationControllerIT {

    private static final String API_PREFIX = "/api/v1/auth/";
    private static final String TEST_EMAIL = "john.doe@example.com";
    private static final String TOKEN_TYPE_BEARER = "Bearer";
    private static final String LOGIN_PATH = "login";
    private static final String REGISTER_PATH = "register";
    private static final String REFRESH_PATH = "refresh";
    private static final String TEST_PHONE_NUMBER = "+1234567890";
    private static final String STRONG_PASSWORD = "Password123!";
    private static final String VALIDATION_ERROR_CODE = "VALIDATION_ERROR";

    @Autowired
    private RestTestClient restTestClient;

    @MockitoBean
    private AuthenticationService authenticationService;

    @Nested
    @DisplayName("/login Tests")
    class LoginEndpointTests {

        @Test
        @DisplayName("Should return tokens with valid credentials")
        void shouldReturnTokenWithValidCredentials() {
            AuthenticationRequest request = AuthenticationRequest.builder()
                .email(TEST_EMAIL)
                .password("user-password")
                .build();
            AuthenticationResponse expectedResponse =
                AuthenticationResponse.builder()
                    .accessToken("access-token")
                    .refreshToken("refresh-token")
                    .tokenType(TOKEN_TYPE_BEARER)
                    .build();

            when(
                authenticationService.login(any(AuthenticationRequest.class))
            ).thenReturn(expectedResponse);

            restTestClient
                .post()
                .uri(API_PREFIX + LOGIN_PATH)
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
                    assertEquals(
                        TOKEN_TYPE_BEARER,
                        expectedResponse.getTokenType()
                    );
                });
        }

        @Test
        @DisplayName(
            "Should return 401 Unauthorized when credentials are invalid"
        )
        void shouldReturnUnauthorizedWhenCredentialsAreInvalid() {
            AuthenticationRequest request = AuthenticationRequest.builder()
                .email(TEST_EMAIL)
                .password("wrong-password")
                .build();

            when(
                authenticationService.login(any(AuthenticationRequest.class))
            ).thenThrow(new BadCredentialsException("Invalid credentials"));

            restTestClient
                .post()
                .uri(API_PREFIX + LOGIN_PATH)
                .body(request)
                .exchange()
                .expectStatus()
                .isUnauthorized();
        }

        @Test
        @DisplayName(
            "Should return 400 Bad Request when email format is invalid"
        )
        void shouldReturnBadRequestWhenEmailIsInvalid() {
            AuthenticationRequest request = AuthenticationRequest.builder()
                .email("invalid-email-format")
                .password("password123")
                .build();

            restTestClient
                .post()
                .uri(API_PREFIX + LOGIN_PATH)
                .body(request)
                .exchange()
                .expectStatus()
                .isBadRequest();
        }

        @Test
        @DisplayName("Should return 400 Bad Request when password is empty")
        void shouldReturnBadRequestWhenPasswordIsEmpty() {
            AuthenticationRequest request = AuthenticationRequest.builder()
                .email(TEST_EMAIL)
                .password("")
                .build();

            restTestClient
                .post()
                .uri(API_PREFIX + LOGIN_PATH)
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
        void shouldCreateUserWithValidRequest() {
            RegistrationRequest request = RegistrationRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .email(TEST_EMAIL)
                .phoneNumber(TEST_PHONE_NUMBER)
                .password(STRONG_PASSWORD)
                .confirmPassword(STRONG_PASSWORD)
                .build();

            restTestClient
                .post()
                .uri(API_PREFIX + REGISTER_PATH)
                .body(request)
                .exchange()
                .expectStatus()
                .isCreated();
        }

        @Test
        @DisplayName("Should create user when last name is null")
        void shouldCreateUserWhenLastNameIsNull() {
            RegistrationRequest request = RegistrationRequest.builder()
                .firstName("John Doe")
                .lastName(null)
                .email(TEST_EMAIL)
                .phoneNumber(TEST_PHONE_NUMBER)
                .password(STRONG_PASSWORD)
                .confirmPassword(STRONG_PASSWORD)
                .build();

            restTestClient
                .post()
                .uri(API_PREFIX + REGISTER_PATH)
                .body(request)
                .exchange()
                .expectStatus()
                .isCreated();
        }

        @Test
        @DisplayName("Should create user when phone number is null")
        void shouldCreateUserWhenPhoneNumberIsNull() {
            RegistrationRequest request = RegistrationRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .email(TEST_EMAIL)
                .phoneNumber(null)
                .password(STRONG_PASSWORD)
                .confirmPassword(STRONG_PASSWORD)
                .build();

            restTestClient
                .post()
                .uri(API_PREFIX + REGISTER_PATH)
                .body(request)
                .exchange()
                .expectStatus()
                .isCreated();
        }

        @Test
        @DisplayName("Should return 400 Bad Request when email is invalid")
        void shouldReturnBadRequestWhenEmailIsInvalid() {
            RegistrationRequest request = RegistrationRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .email("invalid-email")
                .phoneNumber(TEST_PHONE_NUMBER)
                .password(STRONG_PASSWORD)
                .confirmPassword(STRONG_PASSWORD)
                .build();

            restTestClient
                .post()
                .uri(API_PREFIX + REGISTER_PATH)
                .body(request)
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody(ErrorResponse.class)
                .value(response ->
                    assertEquals(VALIDATION_ERROR_CODE, response.getCode())
                );
        }

        @Test
        @DisplayName(
            "Should return 400 Bad Request when phone number does not start with plus"
        )
        void shouldReturnBadRequestWhenPhoneNumberHasNoLeadingPlus() {
            RegistrationRequest request = RegistrationRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .email(TEST_EMAIL)
                .phoneNumber("33123456789")
                .password(STRONG_PASSWORD)
                .confirmPassword(STRONG_PASSWORD)
                .build();

            restTestClient
                .post()
                .uri(API_PREFIX + REGISTER_PATH)
                .body(request)
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody(ErrorResponse.class)
                .value(response ->
                    assertEquals(VALIDATION_ERROR_CODE, response.getCode())
                );
        }

        @Test
        @DisplayName("Should return 400 Bad Request when password is too short")
        void shouldReturnBadRequestWhenPasswordIsTooShort() {
            RegistrationRequest request = RegistrationRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .email(TEST_EMAIL)
                .phoneNumber(TEST_PHONE_NUMBER)
                .password("12")
                .confirmPassword(STRONG_PASSWORD)
                .build();

            restTestClient
                .post()
                .uri(API_PREFIX + REGISTER_PATH)
                .body(request)
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody(ErrorResponse.class)
                .value(response ->
                    assertEquals(VALIDATION_ERROR_CODE, response.getCode())
                );
        }

        @Test
        @DisplayName(
            "Should return 400 Bad Request when passwords do not match"
        )
        void shouldReturnBadRequestWhenPasswordsDoNotMatch() {
            RegistrationRequest request = RegistrationRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .email(TEST_EMAIL)
                .phoneNumber(TEST_PHONE_NUMBER)
                .password("Password123_")
                .confirmPassword(STRONG_PASSWORD)
                .build();

            restTestClient
                .post()
                .uri(API_PREFIX + REGISTER_PATH)
                .body(request)
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody(ErrorResponse.class)
                .value(response ->
                    assertEquals(VALIDATION_ERROR_CODE, response.getCode())
                );
        }
    }

    @Nested
    @DisplayName("/refresh Tests")
    class RefreshEndpointTests {

        @Test
        @DisplayName("Should return tokens when refresh token is valid")
        void shouldReturnTokenWhenRefreshTokenIsValid() {
            RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken("valid-refresh-token")
                .build();
            AuthenticationResponse expectedResponse =
                AuthenticationResponse.builder()
                    .accessToken("new-access-token")
                    .refreshToken("new-refresh-token")
                    .tokenType(TOKEN_TYPE_BEARER)
                    .build();

            when(
                authenticationService.refreshToken(
                    any(RefreshTokenRequest.class)
                )
            ).thenReturn(expectedResponse);

            restTestClient
                .post()
                .uri(API_PREFIX + REFRESH_PATH)
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
                    assertEquals(
                        TOKEN_TYPE_BEARER,
                        expectedResponse.getTokenType()
                    );
                });
        }

        @Test
        @DisplayName(
            "Should return 400 Bad Request when refresh token is blank"
        )
        void shouldReturnBadRequestWhenRefreshTokenIsBlank() {
            RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken("")
                .build();

            restTestClient
                .post()
                .uri(API_PREFIX + REFRESH_PATH)
                .body(request)
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody(ErrorResponse.class)
                .value(response ->
                    assertEquals(VALIDATION_ERROR_CODE, response.getCode())
                );
        }

        @Test
        @DisplayName(
            "Should return 400 Bad Request when refresh token is invalid"
        )
        void shouldReturnBadRequestWhenRefreshTokenIsInvalid() {
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
                .uri(API_PREFIX + REFRESH_PATH)
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
}
