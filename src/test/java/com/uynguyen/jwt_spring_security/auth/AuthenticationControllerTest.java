package com.uynguyen.jwt_spring_security.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.uynguyen.jwt_spring_security.auth.request.AuthenticationRequest;
import com.uynguyen.jwt_spring_security.auth.request.RegistrationRequest;
import com.uynguyen.jwt_spring_security.auth.response.AuthenticationResponse;
import com.uynguyen.jwt_spring_security.handler.ErrorResponse;
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
                .uri("/api/v1/auth/login")
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
                .uri("/api/v1/auth/login")
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
                .uri("/api/v1/auth/login")
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
                .uri("/api/v1/auth/login")
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
                .uri("/api/v1/auth/register")
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
                .uri("/api/v1/auth/register")
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
                .uri("/api/v1/auth/register")
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
                .uri("/api/v1/auth/register")
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
}
