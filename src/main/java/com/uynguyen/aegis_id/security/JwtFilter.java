package com.uynguyen.aegis_id.security;

import com.uynguyen.aegis_id.user.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        // For minor speed boost
        if (request.getServletPath().contains("/api/v1/auth")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        final String jwtToken;
        final String tokenSubject;

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        jwtToken = authHeader.substring(7);
        tokenSubject = this.jwtService.extractUserIdFromToken(jwtToken);

        if (
            tokenSubject != null &&
            SecurityContextHolder.getContext().getAuthentication() == null
        ) {
            final Optional<UserDetails> userDetails = loadUserDetails(
                tokenSubject
            );

            if (
                userDetails.isPresent() &&
                this.jwtService.isTokenValid(jwtToken, tokenSubject)
            ) {
                final UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken =
                    new UsernamePasswordAuthenticationToken(
                        userDetails.get(),
                        null,
                        userDetails.get().getAuthorities()
                    );

                usernamePasswordAuthenticationToken.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request)
                );

                SecurityContextHolder.getContext().setAuthentication(
                    usernamePasswordAuthenticationToken
                );
            }
        }

        filterChain.doFilter(request, response);
    }

    private Optional<UserDetails> loadUserDetails(final String tokenSubject) {
        final Optional<UserDetails> userById = this.userRepository.findById(
            tokenSubject
        ).map(UserDetails.class::cast);

        if (userById.isPresent()) {
            return userById;
        }

        try {
            return Optional.of(
                this.userDetailsService.loadUserByUsername(tokenSubject)
            );
        } catch (UsernameNotFoundException _) {
            return Optional.empty();
        }
    }
}
