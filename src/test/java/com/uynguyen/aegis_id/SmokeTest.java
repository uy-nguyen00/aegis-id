package com.uynguyen.aegis_id;

import static org.assertj.core.api.Assertions.assertThat;

import com.uynguyen.aegis_id.user.UserController;
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
