package com.uynguyen.aegis_id.security;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.uynguyen.aegis_id.user.User;
import com.uynguyen.aegis_id.user.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtFilter Unit Tests")
class JwtFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserDetailsService userDetailsService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("Header parsing")
    class HeaderParsing {

        @Test
        @DisplayName("Should bypass JWT parsing for auth endpoints")
        void shouldBypassJwtParsingForAuthEndpoints()
            throws ServletException, IOException {
            JwtFilter jwtFilter = new JwtFilter(
                jwtService,
                userRepository,
                userDetailsService
            );
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setServletPath("/api/v1/auth/login");
            request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer token-123");
            MockHttpServletResponse response = new MockHttpServletResponse();
            FilterChain filterChain = mock(FilterChain.class);

            jwtFilter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            verifyNoInteractions(
                jwtService,
                userRepository,
                userDetailsService
            );
            assertNull(SecurityContextHolder.getContext().getAuthentication());
        }

        @Test
        @DisplayName(
            "Should continue chain when Authorization header is missing"
        )
        void shouldContinueChainWhenAuthorizationHeaderIsMissing()
            throws ServletException, IOException {
            JwtFilter jwtFilter = new JwtFilter(
                jwtService,
                userRepository,
                userDetailsService
            );
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setServletPath("/api/v1/users/me");
            MockHttpServletResponse response = new MockHttpServletResponse();
            FilterChain filterChain = mock(FilterChain.class);

            jwtFilter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            verify(jwtService, never()).extractUserIdFromToken(
                org.mockito.ArgumentMatchers.anyString()
            );
            assertNull(SecurityContextHolder.getContext().getAuthentication());
        }
    }

    @Nested
    @DisplayName("User resolution")
    class UserResolution {

        @Test
        @DisplayName(
            "Should authenticate when user is loaded from fallback UserDetailsService"
        )
        void shouldAuthenticateWhenUserIsLoadedFromFallbackService()
            throws ServletException, IOException {
            JwtFilter jwtFilter = new JwtFilter(
                jwtService,
                userRepository,
                userDetailsService
            );
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setServletPath("/api/v1/users/me");
            request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer token-123");
            MockHttpServletResponse response = new MockHttpServletResponse();
            FilterChain filterChain = mock(FilterChain.class);

            UserDetails fallbackUser =
                new org.springframework.security.core.userdetails.User(
                    "fallback-user",
                    "n/a",
                    List.of(new SimpleGrantedAuthority("ROLE_USER"))
                );

            when(jwtService.extractUserIdFromToken("token-123")).thenReturn(
                "subject-1"
            );
            when(userRepository.findById("subject-1")).thenReturn(
                Optional.empty()
            );
            when(userDetailsService.loadUserByUsername("subject-1")).thenReturn(
                fallbackUser
            );
            when(jwtService.isTokenValid("token-123", "subject-1")).thenReturn(
                true
            );

            jwtFilter.doFilterInternal(request, response, filterChain);

            verify(userDetailsService).loadUserByUsername("subject-1");
            verify(filterChain).doFilter(request, response);
            assertNotNull(
                SecurityContextHolder.getContext().getAuthentication()
            );
        }

        @Test
        @DisplayName(
            "Should not authenticate when fallback UserDetailsService throws UsernameNotFoundException"
        )
        void shouldNotAuthenticateWhenFallbackUserNotFound()
            throws ServletException, IOException {
            JwtFilter jwtFilter = new JwtFilter(
                jwtService,
                userRepository,
                userDetailsService
            );
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setServletPath("/api/v1/users/me");
            request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer token-123");
            MockHttpServletResponse response = new MockHttpServletResponse();
            FilterChain filterChain = mock(FilterChain.class);

            when(jwtService.extractUserIdFromToken("token-123")).thenReturn(
                "subject-2"
            );
            when(userRepository.findById("subject-2")).thenReturn(
                Optional.empty()
            );
            when(userDetailsService.loadUserByUsername("subject-2")).thenThrow(
                new UsernameNotFoundException("not found")
            );

            jwtFilter.doFilterInternal(request, response, filterChain);

            verify(userDetailsService).loadUserByUsername("subject-2");
            verify(jwtService, never()).isTokenValid("token-123", "subject-2");
            verify(filterChain).doFilter(request, response);
            assertNull(SecurityContextHolder.getContext().getAuthentication());
        }

        @Test
        @DisplayName(
            "Should authenticate when user is loaded from repository by token subject"
        )
        void shouldAuthenticateWhenUserIsLoadedFromRepository()
            throws ServletException, IOException {
            JwtFilter jwtFilter = new JwtFilter(
                jwtService,
                userRepository,
                userDetailsService
            );
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setServletPath("/api/v1/users/me");
            request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer token-123");
            MockHttpServletResponse response = new MockHttpServletResponse();
            FilterChain filterChain = mock(FilterChain.class);

            User user = new User();
            user.setId("subject-3");

            when(jwtService.extractUserIdFromToken("token-123")).thenReturn(
                "subject-3"
            );
            when(userRepository.findById("subject-3")).thenReturn(
                Optional.of(user)
            );
            when(jwtService.isTokenValid("token-123", "subject-3")).thenReturn(
                true
            );

            jwtFilter.doFilterInternal(request, response, filterChain);

            verify(userDetailsService, never()).loadUserByUsername("subject-3");
            verify(filterChain).doFilter(request, response);
            assertNotNull(
                SecurityContextHolder.getContext().getAuthentication()
            );
        }
    }
}
