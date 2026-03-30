package com.uynguyen.aegis_id.security;

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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

    @Value("${spring.profiles.active}")
    private String env;

    private List<String> publicUrls = new ArrayList<>(
        List.of(
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
            "/swagger-ui.html",
            "/index.html",
            "/css/**",
            "/js/**"
        )
    );

    private final JwtFilter jwtFilter;

    @Bean
    public SecurityFilterChain filterChain(final HttpSecurity http) {
        if (env.indexOf("dev") >= 0) {
            publicUrls.add("/");
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
