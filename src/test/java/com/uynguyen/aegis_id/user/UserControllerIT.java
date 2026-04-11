package com.uynguyen.aegis_id.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.uynguyen.aegis_id.exception.BusinessException;
import com.uynguyen.aegis_id.exception.ErrorCode;
import com.uynguyen.aegis_id.handler.ErrorResponse;
import com.uynguyen.aegis_id.security.JwtService;
import com.uynguyen.aegis_id.testsupport.PostgresTestContainerConfig;
import com.uynguyen.aegis_id.user.request.ChangePasswordRequest;
import com.uynguyen.aegis_id.user.request.ProfileUpdateRequest;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@ActiveProfiles("prod")
@Import(PostgresTestContainerConfig.class)
class UserControllerIT {

    private static final String API_PREFIX = "/api/v1/users/";
    private static final String TOKEN = "valid-token";
    private static final String USER_ID = "user-id";
    private static final String USER_EMAIL = "user@example.com";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String NEW_PASSWORD = "newPass";

    @Autowired
    private RestTestClient restTestClient;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        User user = User.builder().id(USER_ID).email(USER_EMAIL).build();

        when(this.jwtService.extractUserIdFromToken(TOKEN)).thenReturn(USER_ID);
        when(this.jwtService.isTokenValid(TOKEN, USER_ID)).thenReturn(true);
        when(this.userRepository.findWithRolesById(USER_ID)).thenReturn(
            Optional.of(user)
        );
    }

    @Test
    @DisplayName("Should return 403 Forbidden when token is invalid")
    void shouldReturnForbiddenWhenTokenIsInvalid() {
        String invalidToken = "invalid-token";
        ProfileUpdateRequest request = ProfileUpdateRequest.builder()
            .firstName("John")
            .lastName("Doe")
            .dateOfBirth(LocalDate.of(1990, 1, 1))
            .build();

        restTestClient
            .patch()
            .uri(API_PREFIX + "me")
            .header(AUTHORIZATION_HEADER, BEARER_PREFIX + invalidToken)
            .body(request)
            .exchange()
            .expectStatus()
            .isForbidden();
    }

    @Test
    @DisplayName("Should return 403 Forbidden when no token is provided")
    void shouldReturnForbiddenWhenNoTokenIsProvided() {
        ProfileUpdateRequest request = ProfileUpdateRequest.builder()
            .firstName("John")
            .lastName("Doe")
            .dateOfBirth(LocalDate.of(1990, 1, 1))
            .build();

        restTestClient
            .patch()
            .uri(API_PREFIX + "me")
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
        void shouldUpdateProfileWhenRequestIsValid() {
            ProfileUpdateRequest request = ProfileUpdateRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .build();

            restTestClient
                .patch()
                .uri(API_PREFIX + "me")
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + TOKEN)
                .body(request)
                .exchange()
                .expectStatus()
                .isNoContent();

            verify(userService).updateProfileInfo(
                any(ProfileUpdateRequest.class),
                eq(USER_ID)
            );
        }

        @Test
        @DisplayName("Should update profile when last name is null")
        void shouldUpdateProfileWhenLastNameIsNull() {
            ProfileUpdateRequest request = ProfileUpdateRequest.builder()
                .firstName("John")
                .lastName(null)
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .build();

            restTestClient
                .patch()
                .uri(API_PREFIX + "me")
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + TOKEN)
                .body(request)
                .exchange()
                .expectStatus()
                .isNoContent();

            verify(userService).updateProfileInfo(
                any(ProfileUpdateRequest.class),
                eq(USER_ID)
            );
        }

        @Test
        @DisplayName(
            "Should return 400 Bad Request when request body is invalid"
        )
        void shouldReturnBadRequestWhenRequestBodyIsInvalid() {
            ProfileUpdateRequest request = ProfileUpdateRequest.builder()
                .firstName("") // Invalid: empty
                .lastName("Doe")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .build();

            restTestClient
                .patch()
                .uri(API_PREFIX + "me")
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + TOKEN)
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
        void shouldChangePasswordWhenRequestIsValid() {
            ChangePasswordRequest request = ChangePasswordRequest.builder()
                .oldPassword("oldPass")
                .newPassword(NEW_PASSWORD)
                .confirmNewPassword(NEW_PASSWORD)
                .build();

            restTestClient
                .post()
                .uri(API_PREFIX + "me/password")
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + TOKEN)
                .body(request)
                .exchange()
                .expectStatus()
                .isNoContent();

            verify(userService).changePassword(
                any(ChangePasswordRequest.class),
                eq(USER_ID)
            );
        }

        @Test
        @DisplayName(
            "Should return 400 Bad Request when passwords do not match"
        )
        void shouldReturnBadRequestWhenPasswordsDoNotMatch() {
            ChangePasswordRequest request = ChangePasswordRequest.builder()
                .oldPassword("oldPass")
                .newPassword(NEW_PASSWORD)
                .confirmNewPassword("mismatch")
                .build();

            doThrow(new BusinessException(ErrorCode.CHANGE_PASSWORD_MISMATCH))
                .when(userService)
                .changePassword(any(ChangePasswordRequest.class), eq(USER_ID));

            restTestClient
                .post()
                .uri(API_PREFIX + "me/password")
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + TOKEN)
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
        void shouldDeactivateAccountWhenRequestIsValid() {
            restTestClient
                .patch()
                .uri(API_PREFIX + "me/deactivate")
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + TOKEN)
                .exchange()
                .expectStatus()
                .isNoContent();

            verify(userService).deactivateAccount(USER_ID);
        }

        @Test
        @DisplayName(
            "Should return 400 Bad Request when account is already deactivated"
        )
        void shouldReturnBadRequestWhenAccountAlreadyDeactivated() {
            doThrow(
                new BusinessException(ErrorCode.ACCOUNT_ALREADY_DEACTIVATED)
            )
                .when(userService)
                .deactivateAccount(USER_ID);

            restTestClient
                .patch()
                .uri(API_PREFIX + "me/deactivate")
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + TOKEN)
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
        void shouldReactivateAccountWhenRequestIsValid() {
            restTestClient
                .patch()
                .uri(API_PREFIX + "me/reactivate")
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + TOKEN)
                .exchange()
                .expectStatus()
                .isNoContent();

            verify(userService).reactivateAccount(USER_ID);
        }

        @Test
        @DisplayName(
            "Should return 400 Bad Request when account is already activated"
        )
        void shouldReturnBadRequestWhenAccountAlreadyActivated() {
            doThrow(new BusinessException(ErrorCode.ACCOUNT_ALREADY_ACTIVATED))
                .when(userService)
                .reactivateAccount(USER_ID);

            restTestClient
                .patch()
                .uri(API_PREFIX + "me/reactivate")
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + TOKEN)
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

    @Nested
    @DisplayName("Delete Account Tests")
    class DeleteAccountTests {

        @Test
        @DisplayName("Should delete account successfully")
        void shouldDeleteAccountWhenRequestIsValid() {
            restTestClient
                .delete()
                .uri(API_PREFIX + "me")
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + TOKEN)
                .exchange()
                .expectStatus()
                .isNoContent();

            verify(userService).deleteAccount(USER_ID);
        }

        @Test
        @DisplayName(
            "Should return 400 Bad Request when account is already activated"
        )
        void shouldReturnBadRequestWhenAccountAlreadyActivated() {
            doThrow(new BusinessException(ErrorCode.ACCOUNT_ALREADY_ACTIVATED))
                .when(userService)
                .deleteAccount(USER_ID);

            restTestClient
                .delete()
                .uri(API_PREFIX + "me")
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + TOKEN)
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
