package com.uynguyen.aegis_id.auth.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.uynguyen.aegis_id.auth.request.AuthenticationRequest;
import com.uynguyen.aegis_id.auth.request.RefreshTokenRequest;
import com.uynguyen.aegis_id.auth.request.RegistrationRequest;
import com.uynguyen.aegis_id.auth.response.AuthenticationResponse;
import com.uynguyen.aegis_id.exception.BusinessException;
import com.uynguyen.aegis_id.exception.ErrorCode;
import com.uynguyen.aegis_id.security.JwtService;
import com.uynguyen.aegis_id.user.User;
import com.uynguyen.aegis_id.user.UserMapper;
import com.uynguyen.aegis_id.user.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthenticationServiceImpl Unit Tests")
class AuthenticationServiceImplTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private AuthenticationServiceImpl authenticationService;

    @Nested
    @DisplayName("register Tests")
    class RegisterTests {

        @Test
        @DisplayName("Should register user successfully when all checks pass")
        void shouldRegisterUserSuccessfully() {
            // Given
            RegistrationRequest request = mock(RegistrationRequest.class);
            when(request.getEmail()).thenReturn("test@example.com");
            when(request.getPhoneNumber()).thenReturn("1234567890");
            when(request.getPassword()).thenReturn("password");
            when(request.getConfirmPassword()).thenReturn("password");

            User user = new User();
            user.setId("user-id");
            user.setEmail("test@example.com");
            user.setFirstName("John");
            user.setLastName("Doe");

            when(
                userRepository.existsByEmailIgnoreCase(anyString())
            ).thenReturn(false);
            when(userRepository.existsByPhoneNumber(anyString())).thenReturn(
                false
            );
            when(userMapper.toUser(any(RegistrationRequest.class))).thenReturn(
                user
            );

            // When
            authenticationService.register(request);

            // Then
            verify(userRepository).save(user);
            assertNotNull(user.getRoles());
            assertTrue(user.getRoles().isEmpty());
        }

        @Test
        @DisplayName(
            "Should register user successfully when phone number is null"
        )
        void shouldRegisterUserSuccessfullyWhenPhoneNumberIsNull() {
            // Given
            RegistrationRequest request = mock(RegistrationRequest.class);
            when(request.getEmail()).thenReturn("test@example.com");
            when(request.getPhoneNumber()).thenReturn(null);
            when(request.getPassword()).thenReturn("password");
            when(request.getConfirmPassword()).thenReturn("password");

            User user = new User();
            user.setId("user-id");
            user.setEmail("test@example.com");
            user.setFirstName("John");
            user.setLastName("Doe");

            when(
                userRepository.existsByEmailIgnoreCase(anyString())
            ).thenReturn(false);
            when(userMapper.toUser(any(RegistrationRequest.class))).thenReturn(
                user
            );

            // When
            authenticationService.register(request);

            // Then
            verify(userRepository, never()).existsByPhoneNumber(anyString());
            verify(userRepository).save(user);
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
    }

    @Nested
    @DisplayName("login Tests")
    class LoginTests {

        @Test
        @DisplayName("Should login successfully and return tokens")
        void shouldLoginSuccessfully() {
            // Given
            AuthenticationRequest request = mock(AuthenticationRequest.class);
            when(request.getEmail()).thenReturn("test@example.com");
            when(request.getPassword()).thenReturn("password");

            User user = new User();
            user.setId("user-id");
            user.setEmail("test@example.com");
            user.setFirstName("John");
            user.setLastName("Doe");

            Authentication authentication = mock(Authentication.class);
            when(authentication.getPrincipal()).thenReturn(user);

            when(
                authenticationManager.authenticate(
                    any(UsernamePasswordAuthenticationToken.class)
                )
            ).thenReturn(authentication);

            when(
                jwtService.generateAccessToken(any(), any(), any(), any())
            ).thenReturn("access-token");
            when(
                jwtService.generateRefreshToken(any(), any(), any(), any())
            ).thenReturn("refresh-token");

            // When
            AuthenticationResponse response = authenticationService.login(
                request
            );

            // Then
            assertNotNull(response);
            assertEquals("access-token", response.getAccessToken());
            assertEquals("refresh-token", response.getRefreshToken());
            assertEquals("Bearer", response.getTokenType());

            verify(authenticationManager).authenticate(
                any(UsernamePasswordAuthenticationToken.class)
            );
            verify(jwtService).generateAccessToken(
                eq("user-id"),
                any(),
                eq("John"),
                eq("Doe")
            );
            verify(jwtService).generateRefreshToken(
                eq("user-id"),
                any(),
                eq("John"),
                eq("Doe")
            );
        }

        @Test
        @DisplayName("Should throw exception when authentication fails")
        void shouldThrowExceptionWhenAuthFails() {
            // Given
            AuthenticationRequest request = mock(AuthenticationRequest.class);
            when(request.getEmail()).thenReturn("test@example.com");
            when(request.getPassword()).thenReturn("wrong-password");

            when(
                authenticationManager.authenticate(
                    any(UsernamePasswordAuthenticationToken.class)
                )
            ).thenThrow(new BadCredentialsException("Bad credentials"));

            // When & Then
            assertThrows(BadCredentialsException.class, () ->
                authenticationService.login(request)
            );

            verify(jwtService, never()).generateAccessToken(
                anyString(),
                any(),
                any(),
                any()
            );
        }
    }

    @Nested
    @DisplayName("refreshToken Tests")
    class RefreshTokenTests {

        @Test
        @DisplayName("Should refresh token successfully")
        void shouldRefreshTokenSuccessfully() {
            // Given
            RefreshTokenRequest request = mock(RefreshTokenRequest.class);
            when(request.getRefreshToken()).thenReturn("valid-refresh-token");

            when(
                jwtService.refreshAccessToken("valid-refresh-token")
            ).thenReturn("new-access-token");

            // When
            AuthenticationResponse response =
                authenticationService.refreshToken(request);

            // Then
            assertNotNull(response);
            assertEquals("new-access-token", response.getAccessToken());
            assertEquals("valid-refresh-token", response.getRefreshToken());
            assertEquals("Bearer", response.getTokenType());

            verify(jwtService).refreshAccessToken("valid-refresh-token");
        }

        @Test
        @DisplayName("Should propagate exception when token refresh fails")
        void shouldPropagateExceptionWhenRefreshFails() {
            // Given
            RefreshTokenRequest request = mock(RefreshTokenRequest.class);
            when(request.getRefreshToken()).thenReturn("invalid-token");

            when(jwtService.refreshAccessToken("invalid-token")).thenThrow(
                new RuntimeException("Invalid token")
            );

            // When & Then
            assertThrows(RuntimeException.class, () ->
                authenticationService.refreshToken(request)
            );

            verify(jwtService).refreshAccessToken("invalid-token");
        }
    }
}
