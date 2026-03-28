package com.uynguyen.aegis_id.auth.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.uynguyen.aegis_id.auth.AuthenticationService;
import com.uynguyen.aegis_id.auth.AuthorizationCode;
import com.uynguyen.aegis_id.auth.AuthorizationCodeRepository;
import com.uynguyen.aegis_id.auth.request.AuthenticationRequest;
import com.uynguyen.aegis_id.auth.request.AuthorizeRequest;
import com.uynguyen.aegis_id.auth.request.CodeExchangeRequest;
import com.uynguyen.aegis_id.auth.request.RefreshTokenRequest;
import com.uynguyen.aegis_id.auth.request.RegistrationRequest;
import com.uynguyen.aegis_id.auth.response.AuthenticationResponse;
import com.uynguyen.aegis_id.auth.response.AuthorizeResponse;
import com.uynguyen.aegis_id.exception.BusinessException;
import com.uynguyen.aegis_id.exception.ErrorCode;
import com.uynguyen.aegis_id.role.Role;
import com.uynguyen.aegis_id.role.RoleRepository;
import com.uynguyen.aegis_id.security.JwtService;
import com.uynguyen.aegis_id.security.RedirectUriValidator;
import com.uynguyen.aegis_id.user.User;
import com.uynguyen.aegis_id.user.UserMapper;
import com.uynguyen.aegis_id.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
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
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

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

    @Mock
    private RedirectUriValidator redirectUriValidator;

    @Mock
    private AuthorizationCodeRepository authorizationCodeRepository;

    @InjectMocks
    private AuthenticationServiceImpl authenticationService;

    @BeforeEach
    void setUp() {}

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
            user.setEmail("test@example.com");

            Authentication authentication = mock(Authentication.class);
            when(authentication.getPrincipal()).thenReturn(user);

            when(
                authenticationManager.authenticate(
                    any(UsernamePasswordAuthenticationToken.class)
                )
            ).thenReturn(authentication);

            when(jwtService.generateAccessToken(any(), any())).thenReturn(
                "access-token"
            );
            when(jwtService.generateRefreshToken(any(), any())).thenReturn(
                "refresh-token"
            );

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
            verify(jwtService).generateAccessToken(any(), any());
            verify(jwtService).generateRefreshToken(any(), any());
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

            verify(jwtService, never()).generateAccessToken(anyString(), any());
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

    @Nested
    @DisplayName("authorize Tests")
    class AuthorizeTests {

        @Test
        @DisplayName("Should return authorization code on valid request")
        void shouldReturnAuthorizationCode() {
            // Given
            AuthorizeRequest request = AuthorizeRequest.builder()
                .email("test@example.com")
                .password("password")
                .redirectUri("http://localhost:3000/callback")
                .build();

            User user = new User();
            user.setId("user-id");
            user.setEmail("test@example.com");

            Authentication authentication = mock(Authentication.class);
            when(authentication.getPrincipal()).thenReturn(user);
            when(
                authenticationManager.authenticate(
                    any(UsernamePasswordAuthenticationToken.class)
                )
            ).thenReturn(authentication);

            doNothing().when(redirectUriValidator).validate(anyString());

            // When
            AuthorizeResponse response = authenticationService.authorize(
                request
            );

            // Then
            assertNotNull(response);
            assertNotNull(response.getCode());
            assertEquals(
                "http://localhost:3000/callback",
                response.getRedirectUri()
            );

            verify(redirectUriValidator).validate(
                "http://localhost:3000/callback"
            );
            verify(authorizationCodeRepository).save(
                any(AuthorizationCode.class)
            );
        }

        @Test
        @DisplayName("Should throw when redirect URI is not allowed")
        void shouldThrowWhenRedirectUriInvalid() {
            // Given
            AuthorizeRequest request = AuthorizeRequest.builder()
                .email("test@example.com")
                .password("password")
                .redirectUri("https://evil.com/callback")
                .build();

            doThrow(
                new BusinessException(
                    ErrorCode.INVALID_REDIRECT_URI,
                    "https://evil.com/callback"
                )
            )
                .when(redirectUriValidator)
                .validate(anyString());

            // When & Then
            BusinessException ex = assertThrows(BusinessException.class, () ->
                authenticationService.authorize(request)
            );
            assertEquals(ErrorCode.INVALID_REDIRECT_URI, ex.getErrorCode());
            verify(authorizationCodeRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw when credentials are invalid")
        void shouldThrowWhenCredentialsInvalid() {
            // Given
            AuthorizeRequest request = AuthorizeRequest.builder()
                .email("test@example.com")
                .password("wrong")
                .redirectUri("http://localhost:3000/callback")
                .build();

            doNothing().when(redirectUriValidator).validate(anyString());
            when(
                authenticationManager.authenticate(
                    any(UsernamePasswordAuthenticationToken.class)
                )
            ).thenThrow(new BadCredentialsException("Bad credentials"));

            // When & Then
            assertThrows(BadCredentialsException.class, () ->
                authenticationService.authorize(request)
            );
            verify(authorizationCodeRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("exchangeCode Tests")
    class ExchangeCodeTests {

        @Test
        @DisplayName("Should return tokens for valid code")
        void shouldReturnTokensForValidCode() {
            // Given
            CodeExchangeRequest request = CodeExchangeRequest.builder()
                .code("valid-code")
                .build();

            AuthorizationCode authCode = AuthorizationCode.builder()
                .code("valid-code")
                .userId("user-id")
                .redirectUri("http://localhost:3000/callback")
                .expiresAt(Instant.now().plusSeconds(30))
                .used(false)
                .build();

            User user = new User();
            user.setId("user-id");
            user.setEmail("test@example.com");

            when(
                authorizationCodeRepository.findByCode("valid-code")
            ).thenReturn(Optional.of(authCode));
            when(userRepository.findById("user-id")).thenReturn(
                Optional.of(user)
            );
            when(jwtService.generateAccessToken(any(), any())).thenReturn(
                "access-token"
            );
            when(jwtService.generateRefreshToken(any(), any())).thenReturn(
                "refresh-token"
            );

            // When
            AuthenticationResponse response =
                authenticationService.exchangeCode(request);

            // Then
            assertNotNull(response);
            assertEquals("access-token", response.getAccessToken());
            assertEquals("refresh-token", response.getRefreshToken());
            assertEquals("Bearer", response.getTokenType());
            assertTrue(authCode.isUsed());
            verify(authorizationCodeRepository).save(authCode);
        }

        @Test
        @DisplayName("Should throw when code is not found")
        void shouldThrowWhenCodeNotFound() {
            // Given
            CodeExchangeRequest request = CodeExchangeRequest.builder()
                .code("unknown-code")
                .build();

            when(
                authorizationCodeRepository.findByCode("unknown-code")
            ).thenReturn(Optional.empty());

            // When & Then
            BusinessException ex = assertThrows(BusinessException.class, () ->
                authenticationService.exchangeCode(request)
            );
            assertEquals(
                ErrorCode.INVALID_AUTHORIZATION_CODE,
                ex.getErrorCode()
            );
        }

        @Test
        @DisplayName("Should throw when code is already used")
        void shouldThrowWhenCodeAlreadyUsed() {
            // Given
            CodeExchangeRequest request = CodeExchangeRequest.builder()
                .code("used-code")
                .build();

            AuthorizationCode authCode = AuthorizationCode.builder()
                .code("used-code")
                .userId("user-id")
                .redirectUri("http://localhost:3000/callback")
                .expiresAt(Instant.now().plusSeconds(30))
                .used(true)
                .build();

            when(
                authorizationCodeRepository.findByCode("used-code")
            ).thenReturn(Optional.of(authCode));

            // When & Then
            BusinessException ex = assertThrows(BusinessException.class, () ->
                authenticationService.exchangeCode(request)
            );
            assertEquals(
                ErrorCode.INVALID_AUTHORIZATION_CODE,
                ex.getErrorCode()
            );
        }

        @Test
        @DisplayName("Should throw when code is expired")
        void shouldThrowWhenCodeExpired() {
            // Given
            CodeExchangeRequest request = CodeExchangeRequest.builder()
                .code("expired-code")
                .build();

            AuthorizationCode authCode = AuthorizationCode.builder()
                .code("expired-code")
                .userId("user-id")
                .redirectUri("http://localhost:3000/callback")
                .expiresAt(Instant.now().minusSeconds(10))
                .used(false)
                .build();

            when(
                authorizationCodeRepository.findByCode("expired-code")
            ).thenReturn(Optional.of(authCode));

            // When & Then
            BusinessException ex = assertThrows(BusinessException.class, () ->
                authenticationService.exchangeCode(request)
            );
            assertEquals(
                ErrorCode.INVALID_AUTHORIZATION_CODE,
                ex.getErrorCode()
            );
        }
    }
}
