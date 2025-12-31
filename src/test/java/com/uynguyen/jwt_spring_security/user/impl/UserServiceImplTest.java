package com.uynguyen.jwt_spring_security.user.impl;

import com.uynguyen.jwt_spring_security.user.User;
import com.uynguyen.jwt_spring_security.user.UserMapper;
import com.uynguyen.jwt_spring_security.user.UserRepository;
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

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    class loadUserByUsernameTests {

        @Test
        @DisplayName("Should load user with valid username")
        void shouldLoadUser_WithValidUsername() {
            final String userEmail = "john.doe@example.com";
            when(userRepository.findByEmailIgnoreCase(userEmail))
                    .thenReturn(Optional.of(testUser));

            final UserDetails userDetails = userService.loadUserByUsername(userEmail);

            assertNotNull(userDetails);
            assertEquals(userDetails.getUsername(), testUser.getUsername());
            verify(userRepository).findByEmailIgnoreCase(userEmail);
        }

        @Test
        @DisplayName("Should throw UsernameNotFoundException when user not found")
        void shouldThrowException_WhenUserNotFound() {
            final String nonExistentEmail = "nonexistent@example.com";
            when(userRepository.findByEmailIgnoreCase(nonExistentEmail))
                    .thenReturn(Optional.empty());

            assertThrows(UsernameNotFoundException.class,
                    () -> userService.loadUserByUsername(nonExistentEmail));
            verify(userRepository).findByEmailIgnoreCase(nonExistentEmail);
        }

        @Test
        @DisplayName("Should load user with uppercase email")
        void shouldLoadUser_WithUppercaseEmail() {
            final String upperCaseEmail = "JOHN.DOE@EXAMPLE.COM";
            when(userRepository.findByEmailIgnoreCase(upperCaseEmail))
                    .thenReturn(Optional.of(testUser));

            final UserDetails userDetails = userService.loadUserByUsername(upperCaseEmail);

            assertNotNull(userDetails);
            assertEquals(testUser.getUsername(), userDetails.getUsername());
            verify(userRepository).findByEmailIgnoreCase(upperCaseEmail);
        }

        @Test
        @DisplayName("Should load user with mixed case email")
        void shouldLoadUser_WithMixedCaseEmail() {
            final String mixedCaseEmail = "JoHn.DoE@ExAmPlE.cOm";
            when(userRepository.findByEmailIgnoreCase(mixedCaseEmail))
                    .thenReturn(Optional.of(testUser));

            final UserDetails userDetails = userService.loadUserByUsername(mixedCaseEmail);

            assertNotNull(userDetails);
            assertEquals(testUser.getUsername(), userDetails.getUsername());
            verify(userRepository).findByEmailIgnoreCase(mixedCaseEmail);
        }

        @Test
        @DisplayName("Should throw UsernameNotFoundException when null username provided")
        void shouldThrowException_WhenNullUsername() {
            when(userRepository.findByEmailIgnoreCase(null))
                    .thenReturn(Optional.empty());

            assertThrows(UsernameNotFoundException.class,
                    () -> userService.loadUserByUsername(null));
            verify(userRepository).findByEmailIgnoreCase(null);
        }

        @Test
        @DisplayName("Should throw UsernameNotFoundException when empty username provided")
        void shouldThrowException_WhenEmptyUsername() {
            final String emptyEmail = "";
            when(userRepository.findByEmailIgnoreCase(emptyEmail))
                    .thenReturn(Optional.empty());

            assertThrows(UsernameNotFoundException.class,
                    () -> userService.loadUserByUsername(emptyEmail));
            verify(userRepository).findByEmailIgnoreCase(emptyEmail);
        }

        @Test
        @DisplayName("Should throw UsernameNotFoundException with correct message")
        void shouldThrowExceptionWithCorrectMessage_WhenUserNotFound() {
            final String email = "test@example.com";
            when(userRepository.findByEmailIgnoreCase(email))
                    .thenReturn(Optional.empty());

            UsernameNotFoundException exception = assertThrows(
                    UsernameNotFoundException.class,
                    () -> userService.loadUserByUsername(email)
            );

            assertEquals("User not found with user email : " + email, exception.getMessage());
        }

        @Test
        @DisplayName("Should handle email with leading and trailing spaces")
        void shouldLoadUser_WithEmailHavingWhitespace() {
            final String emailWithSpaces = "  john.doe@example.com  ";
            when(userRepository.findByEmailIgnoreCase(emailWithSpaces))
                    .thenReturn(Optional.of(testUser));

            final UserDetails userDetails = userService.loadUserByUsername(emailWithSpaces);

            assertNotNull(userDetails);
            verify(userRepository).findByEmailIgnoreCase(emailWithSpaces);
        }
    }
}
