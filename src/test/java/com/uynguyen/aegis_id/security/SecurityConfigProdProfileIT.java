package com.uynguyen.aegis_id.security;

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
@ActiveProfiles("prod")
@DisplayName("SecurityConfig - prod profile")
class SecurityConfigProdProfileIT {

    @Autowired
    private RestTestClient restTestClient;

    @Test
    @DisplayName("Should require authentication for root path in prod profile")
    void shouldRequireAuthenticationForRootPathInProdProfile() {
        restTestClient.get().uri("/").exchange().expectStatus().isForbidden();
    }
}
