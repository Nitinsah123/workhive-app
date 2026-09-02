package com.workhive.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

/**
 * Normalizes cloud provider database URLs (Render, Railway, Heroku) to ensure they
 * start with the required "jdbc:postgresql://" prefix.
 */
public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String url = environment.getProperty("spring.datasource.url");
        if (url == null || url.isBlank()) {
            url = environment.getProperty("SPRING_DATASOURCE_URL");
        }
        if (url == null || url.isBlank()) {
            url = environment.getProperty("DATABASE_URL");
        }

        if (url != null && !url.isBlank()) {
            String trimmed = url.trim();
            if (trimmed.startsWith("postgres://")) {
                trimmed = "jdbc:postgresql://" + trimmed.substring("postgres://".length());
            } else if (trimmed.startsWith("postgresql://")) {
                trimmed = "jdbc:postgresql://" + trimmed.substring("postgresql://".length());
            }

            Map<String, Object> props = new HashMap<>();
            props.put("spring.datasource.url", trimmed);
            environment.getPropertySources().addFirst(new MapPropertySource("normalizedDatabaseUrl", props));
        }
    }
}
