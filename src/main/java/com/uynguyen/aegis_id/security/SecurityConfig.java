package com.uynguyen.aegis_id.security;

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final List<String> BASE_PUBLIC_URLS = List.of(
        "/api/v1/auth/login",
        "/api/v1/auth/register",
        "/api/v1/auth/refresh",
        "/v2/api-docs",
        "/v3/api-docs",
        "/v3/api-docs/**",
        "/swagger-resources",
        "/swagger-resources/**",
        "/configuration/ui",
        "/configuration/security",
        "/swagger-ui/**",
        "/webjars/**",
        "/swagger-ui.html"
    );

    private static final List<String> DEV_PUBLIC_URLS = List.of(
        "/",
        "/index.html",
        "/css/**",
        "/js/**"
    );

    private final Environment environment;
    private final JwtFilter jwtFilter;

    @Bean
    @Order(1)
    public SecurityFilterChain actuatorFilterChain(
        final HttpSecurity http,
        @Value("${management.server.port:8081}") int managementPort
    ) {
        try {
            return http
                .securityMatcher(
                    request -> request.getLocalPort() == managementPort
                )
                .authorizeHttpRequests(auth ->
                    auth
                        .requestMatchers("/actuator/**")
                        .permitAll()
                        .anyRequest()
                        .denyAll()
                )
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sess ->
                    sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .build();
        } catch (Exception e) {
            throw new IllegalStateException(
                "Failed to build actuator security filter chain",
                e
            );
        }
    }

    @Bean
    @Order(2)
    public SecurityFilterChain filterChain(final HttpSecurity http) {
        List<String> publicUrls = new ArrayList<>(BASE_PUBLIC_URLS);
        if (environment.acceptsProfiles(Profiles.of("dev"))) {
            publicUrls.addAll(DEV_PUBLIC_URLS);
        }

        try {
            return http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth ->
                    auth
                        .requestMatchers(publicUrls.toArray(new String[0]))
                        .permitAll()
                        .anyRequest()
                        .authenticated()
                )
                .sessionManagement(sess ->
                    sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .addFilterBefore(
                    this.jwtFilter,
                    UsernamePasswordAuthenticationFilter.class
                )
                .build();
        } catch (Exception e) {
            throw new IllegalStateException(
                "Failed to build security filter chain",
                e
            );
        }
    }
}
