package com.workhive.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseUrlEnvironmentPostProcessorTest {

    @Test
    @DisplayName("Verify Render PostgreSQL URL with credentials is normalized to clean JDBC URL and credentials")
    void testNormalizeRenderPostgresUrl() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("SPRING_DATASOURCE_URL", "postgresql://workhive:SecretPass123@dpg-dac5u5ifngtc73bencr0-a/workhive_actz");

        DatabaseUrlEnvironmentPostProcessor processor = new DatabaseUrlEnvironmentPostProcessor();
        processor.postProcessEnvironment(env, new SpringApplication());

        assertEquals("jdbc:postgresql://dpg-dac5u5ifngtc73bencr0-a/workhive_actz", env.getProperty("spring.datasource.url"));
        assertEquals("workhive", env.getProperty("spring.datasource.username"));
        assertEquals("SecretPass123", env.getProperty("spring.datasource.password"));
    }

    @Test
    @DisplayName("Verify Render PostgreSQL URL with port is normalized correctly")
    void testNormalizeWithPort() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("DATABASE_URL", "postgresql://myuser:mypass@db.render.com:5432/mydb?sslmode=require");

        DatabaseUrlEnvironmentPostProcessor processor = new DatabaseUrlEnvironmentPostProcessor();
        processor.postProcessEnvironment(env, new SpringApplication());

        assertEquals("jdbc:postgresql://db.render.com:5432/mydb?sslmode=require", env.getProperty("spring.datasource.url"));
        assertEquals("myuser", env.getProperty("spring.datasource.username"));
        assertEquals("mypass", env.getProperty("spring.datasource.password"));
    }
}
