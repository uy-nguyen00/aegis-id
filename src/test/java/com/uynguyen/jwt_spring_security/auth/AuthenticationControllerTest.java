package com.uynguyen.jwt_spring_security.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.uynguyen.jwt_spring_security.auth.request.AuthenticationRequest;
import com.uynguyen.jwt_spring_security.auth.response.AuthenticationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@ActiveProfiles("prod")
public class AuthenticationControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private RestTestClient restTestClient;

    @MockitoBean
    private AuthenticationService authenticationService;

    @BeforeEach
    void setUp() {}

    @Nested
    @DisplayName("/login Tests")
    class LoginEndpointTests {

        @Test
        @DisplayName("Should return tokens with valid credentials")
        void shouldReturnToken_WithValidCredentials() {
            AuthenticationRequest request = AuthenticationRequest.builder()
                .email("john.doe@example.com")
                .password("user-password")
                .build();
            AuthenticationResponse expectedResponse =
                AuthenticationResponse.builder()
                    .accessToken("access-token")
                    .refreshToken("refresh-token")
                    .tokenType("Bearer")
                    .build();

            when(
                authenticationService.login(any(AuthenticationRequest.class))
            ).thenReturn(expectedResponse);

            restTestClient
                .post()
                .uri("/api/v1/auth/login")
                .body(request)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(AuthenticationResponse.class)
                .value(response -> {
                    assertNotNull(response);
                    assertEquals(
                        "access-token",
                        expectedResponse.getAccessToken()
                    );
                    assertEquals(
                        "refresh-token",
                        expectedResponse.getRefreshToken()
                    );
                    assertEquals("Bearer", expectedResponse.getTokenType());
                });
        }
    }
}
