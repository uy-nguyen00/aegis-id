package com.uynguyen.jwt_spring_security.user.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.uynguyen.jwt_spring_security.exception.BusinessException;
import com.uynguyen.jwt_spring_security.exception.ErrorCode;
import com.uynguyen.jwt_spring_security.user.User;
import com.uynguyen.jwt_spring_security.user.UserMapper;
import com.uynguyen.jwt_spring_security.user.UserRepository;
import com.uynguyen.jwt_spring_security.user.request.ChangePasswordRequest;
import com.uynguyen.jwt_spring_security.user.request.ProfileUpdateRequest;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserServiceImpl Unit Tests")
public class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserServiceImpl userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        this.testUser = User.builder()
            .id("user-123")
            .firstName("John")
            .lastName("Doe")
            .email("john.doe@example.com")
            .build();
    }

    @Nested
    @DisplayName("loadUserByUsername Tests")
    class LoadUserByUsernameTests {

        @Test
        @DisplayName("Should load user with valid username")
        void shouldLoadUser_WithValidUsername() {
            final String userEmail = "john.doe@example.com";
            when(userRepository.findByEmailIgnoreCase(userEmail)).thenReturn(
                Optional.of(testUser)
            );

            final UserDetails userDetails = userService.loadUserByUsername(
                userEmail
            );

            assertNotNull(userDetails);
            assertEquals(userDetails.getUsername(), testUser.getUsername());
            verify(userRepository).findByEmailIgnoreCase(userEmail);
        }

        @Test
        @DisplayName(
            "Should throw UsernameNotFoundException when user not found"
        )
        void shouldThrowException_WhenUserNotFound() {
            final String nonExistentEmail = "nonexistent@example.com";
            when(
                userRepository.findByEmailIgnoreCase(nonExistentEmail)
            ).thenReturn(Optional.empty());

            assertThrows(UsernameNotFoundException.class, () ->
                userService.loadUserByUsername(nonExistentEmail)
            );
            verify(userRepository).findByEmailIgnoreCase(nonExistentEmail);
        }

        @Test
        @DisplayName("Should load user with uppercase email")
        void shouldLoadUser_WithUppercaseEmail() {
            final String upperCaseEmail = "JOHN.DOE@EXAMPLE.COM";
            when(
                userRepository.findByEmailIgnoreCase(upperCaseEmail)
            ).thenReturn(Optional.of(testUser));

            final UserDetails userDetails = userService.loadUserByUsername(
                upperCaseEmail
            );

            assertNotNull(userDetails);
            assertEquals(testUser.getUsername(), userDetails.getUsername());
            verify(userRepository).findByEmailIgnoreCase(upperCaseEmail);
        }

        @Test
        @DisplayName("Should load user with mixed case email")
        void shouldLoadUser_WithMixedCaseEmail() {
            final String mixedCaseEmail = "JoHn.DoE@ExAmPlE.cOm";
            when(
                userRepository.findByEmailIgnoreCase(mixedCaseEmail)
            ).thenReturn(Optional.of(testUser));

            final UserDetails userDetails = userService.loadUserByUsername(
                mixedCaseEmail
            );

            assertNotNull(userDetails);
            assertEquals(testUser.getUsername(), userDetails.getUsername());
            verify(userRepository).findByEmailIgnoreCase(mixedCaseEmail);
        }

        @Test
        @DisplayName(
            "Should throw UsernameNotFoundException when null username provided"
        )
        void shouldThrowException_WhenNullUsername() {
            when(userRepository.findByEmailIgnoreCase(null)).thenReturn(
                Optional.empty()
            );

            assertThrows(UsernameNotFoundException.class, () ->
                userService.loadUserByUsername(null)
            );
            verify(userRepository).findByEmailIgnoreCase(null);
        }

        @Test
        @DisplayName(
            "Should throw UsernameNotFoundException when empty username provided"
        )
        void shouldThrowException_WhenEmptyUsername() {
            final String emptyEmail = "";
            when(userRepository.findByEmailIgnoreCase(emptyEmail)).thenReturn(
                Optional.empty()
            );

            assertThrows(UsernameNotFoundException.class, () ->
                userService.loadUserByUsername(emptyEmail)
            );
            verify(userRepository).findByEmailIgnoreCase(emptyEmail);
        }

        @Test
        @DisplayName(
            "Should throw UsernameNotFoundException with correct message"
        )
        void shouldThrowExceptionWithCorrectMessage_WhenUserNotFound() {
            final String email = "test@example.com";
            when(userRepository.findByEmailIgnoreCase(email)).thenReturn(
                Optional.empty()
            );

            UsernameNotFoundException exception = assertThrows(
                UsernameNotFoundException.class,
                () -> userService.loadUserByUsername(email)
            );

            assertEquals(
                "User not found with user email : " + email,
                exception.getMessage()
            );
        }

        @Test
        @DisplayName("Should handle email with leading and trailing spaces")
        void shouldLoadUser_WithEmailHavingWhitespace() {
            final String emailWithSpaces = "  john.doe@example.com  ";
            when(
                userRepository.findByEmailIgnoreCase(emailWithSpaces)
            ).thenReturn(Optional.of(testUser));

            final UserDetails userDetails = userService.loadUserByUsername(
                emailWithSpaces
            );

            assertNotNull(userDetails);
            verify(userRepository).findByEmailIgnoreCase(emailWithSpaces);
        }
    }

    @Nested
    @DisplayName("updateProfileInfo Tests")
    class UpdateProfileInfoTests {

        @Test
        @DisplayName("Should update profile successfully when user exists")
        void shouldUpdateProfile_WhenUserExists() {
            String userId = "user-123";
            ProfileUpdateRequest request = mock(ProfileUpdateRequest.class);

            when(userRepository.findById(userId)).thenReturn(
                Optional.of(testUser)
            );

            userService.updateProfileInfo(request, userId);

            verify(userRepository).findById(userId);
            verify(userMapper).mergeUserInfo(testUser, request);
            verify(userRepository).save(testUser);
        }

        @Test
        @DisplayName("Should throw BusinessException when user not found")
        void shouldThrowException_WhenUserNotFound() {
            String userId = "non-existent-id";
            ProfileUpdateRequest request = mock(ProfileUpdateRequest.class);

            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            BusinessException exception = assertThrows(
                BusinessException.class,
                () -> userService.updateProfileInfo(request, userId)
            );

            assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
            verify(userRepository).findById(userId);
            verifyNoInteractions(userMapper);
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw NullPointerException when userId is null")
        void shouldThrowException_WhenUserIdIsNull() {
            ProfileUpdateRequest request = mock(ProfileUpdateRequest.class);

            assertThrows(NullPointerException.class, () ->
                userService.updateProfileInfo(request, null)
            );
        }
    }

    @Nested
    @DisplayName("changePassword Tests")
    class ChangePasswordTests {

        @Test
        @DisplayName(
            "Should change password successfully when inputs are valid"
        )
        void shouldChangePassword_WhenInputsAreValid() {
            String userId = "user-123";
            String oldPassword = "oldPassword";
            String newPassword = "newPassword";
            String encodedNewPassword = "encodedNewPassword";
            String currentEncodedPassword = "currentEncodedPassword";

            ChangePasswordRequest request = mock(ChangePasswordRequest.class);
            when(request.getNewPassword()).thenReturn(newPassword);
            when(request.getConfirmNewPassword()).thenReturn(newPassword);
            when(request.getOldPassword()).thenReturn(oldPassword);

            testUser.setPassword(currentEncodedPassword);

            when(userRepository.findById(userId)).thenReturn(
                Optional.of(testUser)
            );
            when(
                passwordEncoder.matches(oldPassword, currentEncodedPassword)
            ).thenReturn(true);
            when(passwordEncoder.encode(newPassword)).thenReturn(
                encodedNewPassword
            );

            userService.changePassword(request, userId);

            assertEquals(encodedNewPassword, testUser.getPassword());
            verify(userRepository).save(testUser);
        }

        @Test
        @DisplayName("Should throw exception when passwords do not match")
        void shouldThrowException_WhenPasswordsDoNotMatch() {
            String userId = "user-123";
            ChangePasswordRequest request = mock(ChangePasswordRequest.class);
            when(request.getNewPassword()).thenReturn("pass1");
            when(request.getConfirmNewPassword()).thenReturn("pass2");

            BusinessException exception = assertThrows(
                BusinessException.class,
                () -> userService.changePassword(request, userId)
            );

            assertEquals(
                ErrorCode.CHANGE_PASSWORD_MISMATCH,
                exception.getErrorCode()
            );
            verifyNoInteractions(userRepository);
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void shouldThrowException_WhenUserNotFound() {
            String userId = "non-existent";
            ChangePasswordRequest request = mock(ChangePasswordRequest.class);
            when(request.getNewPassword()).thenReturn("pass");
            when(request.getConfirmNewPassword()).thenReturn("pass");

            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            BusinessException exception = assertThrows(
                BusinessException.class,
                () -> userService.changePassword(request, userId)
            );

            assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
        }

        @Test
        @DisplayName("Should throw exception when old password is invalid")
        void shouldThrowException_WhenOldPasswordIsInvalid() {
            String userId = "user-123";
            String oldPassword = "wrongPassword";
            String currentEncodedPassword = "currentEncodedPassword";

            ChangePasswordRequest request = mock(ChangePasswordRequest.class);
            when(request.getNewPassword()).thenReturn("newPass");
            when(request.getConfirmNewPassword()).thenReturn("newPass");
            when(request.getOldPassword()).thenReturn(oldPassword);

            testUser.setPassword(currentEncodedPassword);

            when(userRepository.findById(userId)).thenReturn(
                Optional.of(testUser)
            );
            when(
                passwordEncoder.matches(oldPassword, currentEncodedPassword)
            ).thenReturn(false);

            BusinessException exception = assertThrows(
                BusinessException.class,
                () -> userService.changePassword(request, userId)
            );

            assertEquals(
                ErrorCode.INVALID_OLD_PASSWORD,
                exception.getErrorCode()
            );
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw NullPointerException when userId is null")
        void shouldThrowException_WhenUserIdIsNull() {
            ChangePasswordRequest request = mock(ChangePasswordRequest.class);

            assertThrows(NullPointerException.class, () ->
                userService.changePassword(request, null)
            );
        }
    }

    @Nested
    @DisplayName("deactivateAccount Tests")
    class DeactivateAccountTests {

        @Test
        @DisplayName(
            "Should deactivate account successfully when user exists and is enabled"
        )
        void shouldDeactivateAccount_WhenUserExistsAndEnabled() {
            String userId = "user-123";
            testUser.setEnabled(true);

            when(userRepository.findById(userId)).thenReturn(
                Optional.of(testUser)
            );

            userService.deactivateAccount(userId);

            assertFalse(testUser.isEnabled());
            verify(userRepository).save(testUser);
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void shouldThrowException_WhenUserNotFound() {
            String userId = "non-existent";
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            BusinessException exception = assertThrows(
                BusinessException.class,
                () -> userService.deactivateAccount(userId)
            );

            assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName(
            "Should throw exception when account is already deactivated"
        )
        void shouldThrowException_WhenAccountAlreadyDeactivated() {
            String userId = "user-123";
            testUser.setEnabled(false);

            when(userRepository.findById(userId)).thenReturn(
                Optional.of(testUser)
            );

            BusinessException exception = assertThrows(
                BusinessException.class,
                () -> userService.deactivateAccount(userId)
            );

            assertEquals(
                ErrorCode.ACCOUNT_ALREADY_DEACTIVATED,
                exception.getErrorCode()
            );
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw NullPointerException when userId is null")
        void shouldThrowException_WhenUserIdIsNull() {
            assertThrows(NullPointerException.class, () ->
                userService.deactivateAccount(null)
            );
        }
    }

    @Nested
    @DisplayName("reactivateAccount Tests")
    class ReactivateAccountTests {

        @Test
        @DisplayName(
            "Should reactivate account successfully when user exists and is disabled"
        )
        void shouldReactivateAccount_WhenUserExistsAndDisabled() {
            String userId = "user-123";
            testUser.setEnabled(false);

            when(userRepository.findById(userId)).thenReturn(
                Optional.of(testUser)
            );

            userService.reactivateAccount(userId);

            assertTrue(testUser.isEnabled());
            verify(userRepository).save(testUser);
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void shouldThrowException_WhenUserNotFound() {
            String userId = "non-existent";
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            BusinessException exception = assertThrows(
                BusinessException.class,
                () -> userService.reactivateAccount(userId)
            );

            assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when account is already activated")
        void shouldThrowException_WhenAccountAlreadyActivated() {
            String userId = "user-123";
            testUser.setEnabled(true);

            when(userRepository.findById(userId)).thenReturn(
                Optional.of(testUser)
            );

            BusinessException exception = assertThrows(
                BusinessException.class,
                () -> userService.reactivateAccount(userId)
            );

            assertEquals(
                ErrorCode.ACCOUNT_ALREADY_ACTIVATED,
                exception.getErrorCode()
            );
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw NullPointerException when userId is null")
        void shouldThrowException_WhenUserIdIsNull() {
            assertThrows(NullPointerException.class, () ->
                userService.reactivateAccount(null)
            );
        }
    }

    @Nested
    @DisplayName("deleteAccount Tests")
    class DeleteAccountTests {

        @Test
        @DisplayName("Should delete account successfully when user exists")
        void shouldDeleteAccount_WhenUserExists() {
            String userId = "user-123";

            when(userRepository.findById(userId)).thenReturn(
                Optional.of(testUser)
            );

            userService.deleteAccount(userId);

            verify(userRepository).delete(testUser);
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void shouldThrowException_WhenUserNotFound() {
            String userId = "non-existent";
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            BusinessException exception = assertThrows(
                BusinessException.class,
                () -> userService.deleteAccount(userId)
            );

            assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
            verify(userRepository, never()).delete(any());
        }
    }
}
