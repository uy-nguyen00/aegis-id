package com.uynguyen.aegis_id;

import com.uynguyen.aegis_id.testsupport.PostgresTestContainerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(PostgresTestContainerConfig.class)
class AegisIdApplicationIT {

    @Test
    void contextLoads() {
        // Context bootstrap without exceptions is the assertion for this smoke test.
    }
}
