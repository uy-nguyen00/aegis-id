package com.uynguyen.aegis_id.testsupport;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;

@TestConfiguration(proxyBeanMethods = false)
public class PostgresTestContainerConfig {

    @SuppressWarnings("resource")
    private static final PostgreSQLContainer<?> POSTGRES_CONTAINER =
        new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("aegis_id_test")
            .withUsername("test")
            .withPassword("test");

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgresContainer() {
        return POSTGRES_CONTAINER;
    }
}
