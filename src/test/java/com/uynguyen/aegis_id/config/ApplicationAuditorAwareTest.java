package com.uynguyen.aegis_id.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.uynguyen.aegis_id.user.User;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

@DisplayName("ApplicationAuditorAware Unit Tests")
class ApplicationAuditorAwareTest {

    private final ApplicationAuditorAware auditorAware =
        new ApplicationAuditorAware();

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Should return user id when principal is authenticated User")
    void shouldReturnUserIdWhenPrincipalIsAuthenticatedUser() {
        User user = User.builder().id("user-123").build();
        Authentication authentication = mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(user);

        SecurityContext context = mock(SecurityContext.class);
        when(context.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(context);

        Optional<String> currentAuditor = auditorAware.getCurrentAuditor();

        assertTrue(currentAuditor.isPresent());
        assertEquals("user-123", currentAuditor.get());
    }

    @Test
    @DisplayName("Should return empty when security context is empty")
    void shouldReturnEmptyWhenSecurityContextIsEmpty() {
        SecurityContextHolder.clearContext();

        Optional<String> currentAuditor = auditorAware.getCurrentAuditor();

        assertTrue(currentAuditor.isEmpty());
    }

    @Test
    @DisplayName("Should return empty when authentication is null")
    void shouldReturnEmptyWhenAuthenticationIsNull() {
        SecurityContext context = mock(SecurityContext.class);
        when(context.getAuthentication()).thenReturn(null);
        SecurityContextHolder.setContext(context);

        Optional<String> currentAuditor = auditorAware.getCurrentAuditor();

        assertTrue(currentAuditor.isEmpty());
    }

    @Test
    @DisplayName("Should return empty when authentication is not authenticated")
    void shouldReturnEmptyWhenAuthenticationIsNotAuthenticated() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(false);

        SecurityContext context = mock(SecurityContext.class);
        when(context.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(context);

        Optional<String> currentAuditor = auditorAware.getCurrentAuditor();

        assertTrue(currentAuditor.isEmpty());
    }

    @Test
    @DisplayName("Should return empty when authentication is anonymous")
    void shouldReturnEmptyWhenAuthenticationIsAnonymous() {
        Authentication authentication = new AnonymousAuthenticationToken(
            "test-key",
            "anonymousUser",
            List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))
        );

        SecurityContext context = mock(SecurityContext.class);
        when(context.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(context);

        Optional<String> currentAuditor = auditorAware.getCurrentAuditor();

        assertTrue(currentAuditor.isEmpty());
    }

    @Test
    @DisplayName("Should return empty when principal is not User instance")
    void shouldReturnEmptyWhenPrincipalIsNotUserInstance() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn("not-a-user");

        SecurityContext context = mock(SecurityContext.class);
        when(context.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(context);

        Optional<String> currentAuditor = auditorAware.getCurrentAuditor();

        assertTrue(currentAuditor.isEmpty());
    }
}
