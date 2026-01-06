package com.uynguyen.jwt_spring_security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.client.RestTestClient;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@ActiveProfiles("dev")
public class HomeControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private RestTestClient restTestClient;

    @Value("${spring.application.name}")
    private String appName;

    @Test
    void homepageShouldReturnWelcomeMessage() {
        restTestClient
            .get()
            .uri("http://localhost:%d/".formatted(port))
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(String.class)
            .value(response -> {
                assertThat(response).startsWith("Welcome to " + appName);
            });
    }
}
