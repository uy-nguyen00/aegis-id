package com.uynguyen.aegis_id.security;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.client.RestTestClient;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@ActiveProfiles("dev")
@DisplayName("SecurityConfig - dev profile")
class SecurityConfigDevProfileTest {

    @Autowired
    private RestTestClient restTestClient;

    @Test
    @DisplayName("Should allow root path in dev profile")
    void shouldAllowRootPathInDevProfile() {
        restTestClient
            .get()
            .uri("/")
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(String.class)
            .value(body -> {
                assertTrue(body.contains("Welcome to"));
                assertTrue(body.contains("API Docs"));
            });
    }
}
