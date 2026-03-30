package com.uynguyen.aegis_id;

import static org.assertj.core.api.Assertions.assertThat;

import com.uynguyen.aegis_id.user.UserController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

@SpringBootTest
class SmokeTest {

    @Autowired(required = false)
    private HomeController homeController;

    @Autowired
    private UserController userController;

    @Autowired
    private Environment environment;

    @Test
    void contextLoads() {
        if (environment.acceptsProfiles(Profiles.of("dev"))) {
            assertThat(homeController).isNotNull();
        } else {
            assertThat(homeController).isNull();
        }

        assertThat(userController).isNotNull();
    }
}
