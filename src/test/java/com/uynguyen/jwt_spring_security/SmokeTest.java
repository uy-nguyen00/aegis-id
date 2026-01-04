package com.uynguyen.jwt_spring_security;

import static org.assertj.core.api.Assertions.assertThat;

import com.uynguyen.jwt_spring_security.user.UserController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class SmokeTest {

    @Autowired
    private HomeController homeController;

    @Autowired
    private UserController userController;

    @Test
    void contextLoads() throws Exception {
        assertThat(homeController).isNotNull();
        assertThat(userController).isNotNull();
    }
}
