package com.medslot.platform;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.postgresql.PostgreSQLContainer;

public abstract class AbstractPostgresContainerTest {

    static final PostgreSQLContainer POSTGRES;

    static {
        if (DockerClientFactory.instance().isDockerAvailable()) {
            POSTGRES = new PostgreSQLContainer("postgres:17-alpine")
                    .withDatabaseName("medslot_test")
                    .withUsername("test_user")
                    .withPassword("test_pass");
            POSTGRES.start();
        } else {
            POSTGRES = null;
        }
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        if (POSTGRES != null) {
            registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
            registry.add("spring.datasource.username", POSTGRES::getUsername);
            registry.add("spring.datasource.password", POSTGRES::getPassword);
        }
    }
}