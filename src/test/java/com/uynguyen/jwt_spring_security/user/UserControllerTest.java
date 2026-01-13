package com.uynguyen.jwt_spring_security.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.uynguyen.jwt_spring_security.exception.BusinessException;
import com.uynguyen.jwt_spring_security.exception.ErrorCode;
import com.uynguyen.jwt_spring_security.handler.ErrorResponse;
import com.uynguyen.jwt_spring_security.security.JwtService;
import com.uynguyen.jwt_spring_security.user.request.ChangePasswordRequest;
import com.uynguyen.jwt_spring_security.user.request.ProfileUpdateRequest;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@ActiveProfiles("prod")
public class UserControllerTest {

    @Autowired
    private RestTestClient restTestClient;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtService jwtService;

    private final String apiPrefix = "/api/v1/users/";
    private final String token = "valid-token";
    private final String username = "user@example.com";
    private User user;

    @BeforeEach
    void setUp() {
        this.user = User.builder().id("user-id").email(username).build();

        when(this.jwtService.extractUsernameFromToken(token)).thenReturn(
            username
        );
        when(this.jwtService.isTokenValid(token, username)).thenReturn(true);
        when(this.userService.loadUserByUsername(username)).thenReturn(
            this.user
        );
    }

    @Test
    @DisplayName("Should return 403 Forbidden when token is invalid")
    void shouldReturnForbidden_WhenTokenIsInvalid() {
        String invalidToken = "invalid-token";
        ProfileUpdateRequest request = ProfileUpdateRequest.builder()
            .firstName("John")
            .lastName("Doe")
            .dateOfBirth(LocalDate.of(1990, 1, 1))
            .build();

        restTestClient
            .patch()
            .uri(apiPrefix + "me")
            .header("Authorization", "Bearer " + invalidToken)
            .body(request)
            .exchange()
            .expectStatus()
            .isForbidden();
    }

    @Test
    @DisplayName("Should return 403 Forbidden when no token is provided")
    void shouldReturnForbidden_WhenNoTokenIsProvided() {
        ProfileUpdateRequest request = ProfileUpdateRequest.builder()
            .firstName("John")
            .lastName("Doe")
            .dateOfBirth(LocalDate.of(1990, 1, 1))
            .build();

        restTestClient
            .patch()
            .uri(apiPrefix + "me")
            .body(request)
            .exchange()
            .expectStatus()
            .isForbidden();
    }

    @Nested
    @DisplayName("Update Profile Info Tests")
    class UpdateProfileInfoTests {

        @Test
        @DisplayName("Should retrieve user profile successfully")
        void shouldUpdateProfile_WhenRequestIsValid() {
            ProfileUpdateRequest request = ProfileUpdateRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .build();

            restTestClient
                .patch()
                .uri(apiPrefix + "me")
                .header("Authorization", "Bearer " + token)
                .body(request)
                .exchange()
                .expectStatus()
                .isNoContent();

            verify(userService).updateProfileInfo(
                any(ProfileUpdateRequest.class),
                eq("user-id")
            );
        }

        @Test
        @DisplayName(
            "Should return 400 Bad Request when request body is invalid"
        )
        void shouldReturnBadRequest_WhenRequestBodyIsInvalid() {
            ProfileUpdateRequest request = ProfileUpdateRequest.builder()
                .firstName("") // Invalid: empty
                .lastName("Doe")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .build();

            restTestClient
                .patch()
                .uri(apiPrefix + "me")
                .header("Authorization", "Bearer " + token)
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
    @DisplayName("Change Password Tests")
    class ChangePasswordTests {

        @Test
        @DisplayName("Should change password successfully")
        void shouldChangePassword_WhenRequestIsValid() {
            ChangePasswordRequest request = ChangePasswordRequest.builder()
                .oldPassword("oldPass")
                .newPassword("newPass")
                .confirmNewPassword("newPass")
                .build();

            restTestClient
                .post()
                .uri(apiPrefix + "me/password")
                .header("Authorization", "Bearer " + token)
                .body(request)
                .exchange()
                .expectStatus()
                .isNoContent();

            verify(userService).changePassword(
                any(ChangePasswordRequest.class),
                eq("user-id")
            );
        }

        @Test
        @DisplayName(
            "Should return 400 Bad Request when passwords do not match"
        )
        void shouldReturnBadRequest_WhenPasswordsDoNotMatch() {
            ChangePasswordRequest request = ChangePasswordRequest.builder()
                .oldPassword("oldPass")
                .newPassword("newPass")
                .confirmNewPassword("mismatch")
                .build();

            doThrow(new BusinessException(ErrorCode.CHANGE_PASSWORD_MISMATCH))
                .when(userService)
                .changePassword(
                    any(ChangePasswordRequest.class),
                    eq("user-id")
                );

            restTestClient
                .post()
                .uri(apiPrefix + "me/password")
                .header("Authorization", "Bearer " + token)
                .body(request)
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody(ErrorResponse.class)
                .value(response ->
                    assertEquals(
                        ErrorCode.CHANGE_PASSWORD_MISMATCH.getCode(),
                        response.getCode()
                    )
                );
        }
    }

    @Nested
    @DisplayName("Deactivate Account Tests")
    class DeactivateAccountTests {

        @Test
        @DisplayName("Should deactivate account successfully")
        void shouldDeactivateAccount_WhenRequestIsValid() {
            restTestClient
                .patch()
                .uri(apiPrefix + "me/deactivate")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus()
                .isNoContent();

            verify(userService).deactivateAccount(eq("user-id"));
        }

        @Test
        @DisplayName(
            "Should return 400 Bad Request when account is already deactivated"
        )
        void shouldReturnBadRequest_WhenAccountAlreadyDeactivated() {
            doThrow(
                new BusinessException(ErrorCode.ACCOUNT_ALREADY_DEACTIVATED)
            )
                .when(userService)
                .deactivateAccount(eq("user-id"));

            restTestClient
                .patch()
                .uri(apiPrefix + "me/deactivate")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody(ErrorResponse.class)
                .value(response ->
                    assertEquals(
                        ErrorCode.ACCOUNT_ALREADY_DEACTIVATED.getCode(),
                        response.getCode()
                    )
                );
        }
    }

    @Nested
    @DisplayName("Reactivate Account Tests")
    class ReactivateAccountTests {

        @Test
        @DisplayName("Should reactivate account successfully")
        void shouldReactivateAccount_WhenRequestIsValid() {
            restTestClient
                .patch()
                .uri(apiPrefix + "me/reactivate")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus()
                .isNoContent();

            verify(userService).reactivateAccount(eq("user-id"));
        }

        @Test
        @DisplayName(
            "Should return 400 Bad Request when account is already activated"
        )
        void shouldReturnBadRequest_WhenAccountAlreadyActivated() {
            doThrow(new BusinessException(ErrorCode.ACCOUNT_ALREADY_ACTIVATED))
                .when(userService)
                .reactivateAccount(eq("user-id"));

            restTestClient
                .patch()
                .uri(apiPrefix + "me/reactivate")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody(ErrorResponse.class)
                .value(response ->
                    assertEquals(
                        ErrorCode.ACCOUNT_ALREADY_ACTIVATED.getCode(),
                        response.getCode()
                    )
                );
        }
    }
}
