package com.uynguyen.jwt_spring_security.auth.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.uynguyen.jwt_spring_security.auth.request.RegistrationRequest;
import com.uynguyen.jwt_spring_security.exception.BusinessException;
import com.uynguyen.jwt_spring_security.exception.ErrorCode;
import com.uynguyen.jwt_spring_security.role.Role;
import com.uynguyen.jwt_spring_security.role.RoleRepository;
import com.uynguyen.jwt_spring_security.security.JwtService;
import com.uynguyen.jwt_spring_security.user.User;
import com.uynguyen.jwt_spring_security.user.UserMapper;
import com.uynguyen.jwt_spring_security.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthenticationServiceImpl Unit Tests")
public class AuthenticationServiceImplTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private AuthenticationServiceImpl authenticationService;

    @BeforeEach
    void setUp() {}

    @Nested
    @DisplayName("register Tests")
    class registerTests {

        @Test
        @DisplayName("Should register user successfully when all checks pass")
        void shouldRegisterUserSuccessfully() {
            // Given
            RegistrationRequest request = mock(RegistrationRequest.class);
            when(request.getEmail()).thenReturn("test@example.com");
            when(request.getPhoneNumber()).thenReturn("1234567890");
            when(request.getPassword()).thenReturn("password");
            when(request.getConfirmPassword()).thenReturn("password");

            Role role = new Role();
            role.setName("ROLE_USER");

            User user = new User();
            user.setEmail("test@example.com");

            when(
                userRepository.existsByEmailIgnoreCase(anyString())
            ).thenReturn(false);
            when(userRepository.existsByPhoneNumber(anyString())).thenReturn(
                false
            );
            when(roleRepository.findByName("ROLE_USER")).thenReturn(
                Optional.of(role)
            );
            when(userMapper.toUser(any(RegistrationRequest.class))).thenReturn(
                user
            );

            // When
            authenticationService.register(request);

            // Then
            verify(userRepository).save(user);
            verify(roleRepository).findByName("ROLE_USER");
        }

        @Test
        @DisplayName("Should throw BusinessException when email already exists")
        void shouldThrowExceptionWhenEmailExists() {
            // Given
            RegistrationRequest request = mock(RegistrationRequest.class);
            when(request.getEmail()).thenReturn("existing@example.com");

            when(
                userRepository.existsByEmailIgnoreCase(anyString())
            ).thenReturn(true);

            // When & Then
            BusinessException exception = assertThrows(
                BusinessException.class,
                () -> authenticationService.register(request)
            );

            assertEquals(
                ErrorCode.EMAIL_ALREADY_EXISTS,
                exception.getErrorCode()
            );
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName(
            "Should throw BusinessException when phone number already exists"
        )
        void shouldThrowExceptionWhenPhoneExists() {
            // Given
            RegistrationRequest request = mock(RegistrationRequest.class);
            when(request.getEmail()).thenReturn("test@example.com");
            when(request.getPhoneNumber()).thenReturn("existing-phone");

            when(
                userRepository.existsByEmailIgnoreCase(anyString())
            ).thenReturn(false);
            when(userRepository.existsByPhoneNumber(anyString())).thenReturn(
                true
            );

            // When & Then
            BusinessException exception = assertThrows(
                BusinessException.class,
                () -> authenticationService.register(request)
            );

            assertEquals(
                ErrorCode.PHONE_ALREADY_EXISTS,
                exception.getErrorCode()
            );
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName(
            "Should throw BusinessException when passwords do not match"
        )
        void shouldThrowExceptionWhenPasswordsMismatch() {
            // Given
            RegistrationRequest request = mock(RegistrationRequest.class);
            when(request.getEmail()).thenReturn("test@example.com");
            when(request.getPhoneNumber()).thenReturn("1234567890");
            when(request.getPassword()).thenReturn("password");
            when(request.getConfirmPassword()).thenReturn("different-password");

            when(
                userRepository.existsByEmailIgnoreCase(anyString())
            ).thenReturn(false);
            when(userRepository.existsByPhoneNumber(anyString())).thenReturn(
                false
            );

            // When & Then
            BusinessException exception = assertThrows(
                BusinessException.class,
                () -> authenticationService.register(request)
            );

            assertEquals(
                ErrorCode.PASSWORDS_MISMATCH,
                exception.getErrorCode()
            );
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName(
            "Should throw EntityNotFoundException when ROLE_USER does not exist"
        )
        void shouldThrowExceptionWhenRoleNotFound() {
            // Given
            RegistrationRequest request = mock(RegistrationRequest.class);
            when(request.getEmail()).thenReturn("test@example.com");
            when(request.getPhoneNumber()).thenReturn("1234567890");
            when(request.getPassword()).thenReturn("password");
            when(request.getConfirmPassword()).thenReturn("password");

            when(
                userRepository.existsByEmailIgnoreCase(anyString())
            ).thenReturn(false);
            when(userRepository.existsByPhoneNumber(anyString())).thenReturn(
                false
            );
            when(roleRepository.findByName("ROLE_USER")).thenReturn(
                Optional.empty()
            );

            // When & Then
            assertThrows(EntityNotFoundException.class, () ->
                authenticationService.register(request)
            );

            verify(userRepository, never()).save(any());
        }
    }
}
