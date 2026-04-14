package com.uynguyen.aegis_id.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.uynguyen.aegis_id.auth.request.RegistrationRequest;
import com.uynguyen.aegis_id.user.request.ProfileUpdateRequest;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserMapper Unit Tests")
class UserMapperTest {

    @Mock
    private PasswordEncoder passwordEncoder;

    @Nested
    @DisplayName("mergeUserInfo Tests")
    class MergeUserInfoTests {

        @Test
        @DisplayName("Should merge profile fields into existing user")
        void shouldMergeProfileFieldsIntoExistingUser() {
            UserMapper userMapper = new UserMapper(passwordEncoder);
            User user = User.builder()
                .firstName("Old")
                .lastName("Name")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .build();
            ProfileUpdateRequest request = ProfileUpdateRequest.builder()
                .firstName("New")
                .lastName("Person")
                .dateOfBirth(LocalDate.of(2000, 2, 2))
                .build();

            userMapper.mergeUserInfo(user, request);

            assertEquals("New", user.getFirstName());
            assertEquals("Person", user.getLastName());
            assertEquals(LocalDate.of(2000, 2, 2), user.getDateOfBirth());
        }

        @Test
        @DisplayName("Should support partial field updates")
        void shouldSupportPartialFieldUpdates() {
            UserMapper userMapper = new UserMapper(passwordEncoder);
            User user = User.builder()
                .firstName("Old")
                .lastName("Existing")
                .dateOfBirth(LocalDate.of(1995, 5, 15))
                .build();
            ProfileUpdateRequest request = ProfileUpdateRequest.builder()
                .firstName("Updated")
                .lastName(null)
                .dateOfBirth(LocalDate.of(2001, 3, 9))
                .build();

            userMapper.mergeUserInfo(user, request);

            assertEquals("Updated", user.getFirstName());
            assertNull(user.getLastName());
            assertEquals(LocalDate.of(2001, 3, 9), user.getDateOfBirth());
        }

        @Test
        @DisplayName("Should throw NullPointerException when user is null")
        void shouldThrowWhenUserIsNull() {
            UserMapper userMapper = new UserMapper(passwordEncoder);
            ProfileUpdateRequest request = ProfileUpdateRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(LocalDate.of(2000, 1, 1))
                .build();

            assertThrows(NullPointerException.class, () ->
                userMapper.mergeUserInfo(null, request)
            );
        }

        @Test
        @DisplayName("Should throw NullPointerException when request is null")
        void shouldThrowWhenRequestIsNull() {
            UserMapper userMapper = new UserMapper(passwordEncoder);
            User user = new User();

            assertThrows(NullPointerException.class, () ->
                userMapper.mergeUserInfo(user, null)
            );
        }
    }

    @Nested
    @DisplayName("toUser Tests")
    class ToUserTests {

        @Test
        @DisplayName(
            "Should map registration request and initialize default account flags"
        )
        void shouldMapRegistrationRequestAndInitializeDefaultFlags() {
            UserMapper userMapper = new UserMapper(passwordEncoder);
            RegistrationRequest request = RegistrationRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .phoneNumber("+1234567890")
                .password("Password123!")
                .confirmPassword("Password123!")
                .build();
            when(passwordEncoder.encode("Password123!")).thenReturn(
                "encoded-password"
            );

            User result = userMapper.toUser(request);

            assertNotNull(result);
            assertEquals("John", result.getFirstName());
            assertEquals("Doe", result.getLastName());
            assertEquals("john.doe@example.com", result.getEmail());
            assertEquals("+1234567890", result.getPhoneNumber());
            assertEquals("encoded-password", result.getPassword());
            assertTrue(result.isEnabled());
            assertFalse(result.isLocked());
            assertFalse(result.isCredentialsExpired());
            assertFalse(result.isEmailVerified());
            assertFalse(result.isPhoneVerified());
            verify(passwordEncoder).encode("Password123!");
        }

        @Test
        @DisplayName("Should map null optional request fields")
        void shouldMapNullOptionalRequestFields() {
            UserMapper userMapper = new UserMapper(passwordEncoder);
            RegistrationRequest request = RegistrationRequest.builder()
                .firstName("Jane")
                .lastName(null)
                .email("jane@example.com")
                .phoneNumber(null)
                .password("Password123!")
                .confirmPassword("Password123!")
                .build();
            when(passwordEncoder.encode("Password123!")).thenReturn(
                "encoded-password"
            );

            User result = userMapper.toUser(request);

            assertEquals("Jane", result.getFirstName());
            assertNull(result.getLastName());
            assertEquals("jane@example.com", result.getEmail());
            assertNull(result.getPhoneNumber());
        }

        @Test
        @DisplayName("Should throw NullPointerException when request is null")
        void shouldThrowWhenToUserRequestIsNull() {
            UserMapper userMapper = new UserMapper(passwordEncoder);

            assertThrows(NullPointerException.class, () ->
                userMapper.toUser(null)
            );
        }
    }
}
