package com.workhive.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Normalizes cloud provider database URLs (Render, Railway, Heroku) by extracting credentials
 * and producing clean JDBC URLs compliant with org.postgresql.Driver.
 */
public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String raw = environment.getProperty("spring.datasource.url");
        if (raw == null || raw.isBlank()) {
            raw = environment.getProperty("SPRING_DATASOURCE_URL");
        }
        if (raw == null || raw.isBlank()) {
            raw = environment.getProperty("DATABASE_URL");
        }

        if (raw != null && !raw.isBlank()) {
            String url = raw.trim();
            try {
                String uriStr = url;
                if (uriStr.startsWith("jdbc:postgresql://")) {
                    uriStr = "postgresql://" + uriStr.substring("jdbc:postgresql://".length());
                } else if (uriStr.startsWith("jdbc:postgres://")) {
                    uriStr = "postgresql://" + uriStr.substring("jdbc:postgres://".length());
                } else if (uriStr.startsWith("postgres://")) {
                    uriStr = "postgresql://" + uriStr.substring("postgres://".length());
                }

                if (uriStr.startsWith("postgresql://")) {
                    URI uri = new URI(uriStr);
                    String userInfo = uri.getUserInfo();
                    String host = uri.getHost();
                    int port = uri.getPort();
                    String path = uri.getPath();
                    String query = uri.getQuery();

                    Map<String, Object> props = new HashMap<>();

                    if (userInfo != null && userInfo.contains(":")) {
                        String[] parts = userInfo.split(":", 2);
                        props.put("spring.datasource.username", parts[0]);
                        props.put("spring.datasource.password", parts[1]);
                    } else if (userInfo != null && !userInfo.isBlank()) {
                        props.put("spring.datasource.username", userInfo);
                    }

                    StringBuilder cleanJdbcUrl = new StringBuilder("jdbc:postgresql://");
                    cleanJdbcUrl.append(host != null ? host : "localhost");
                    if (port > 0) {
                        cleanJdbcUrl.append(":").append(port);
                    }
                    if (path != null && !path.isBlank()) {
                        cleanJdbcUrl.append(path);
                    }
                    if (query != null && !query.isBlank()) {
                        cleanJdbcUrl.append("?").append(query);
                    }

                    props.put("spring.datasource.url", cleanJdbcUrl.toString());
                    environment.getPropertySources().addFirst(new MapPropertySource("normalizedDatabaseUrl", props));
                    return;
                }
            } catch (Exception ignored) {
                // If URI parsing fails, fallback to simple jdbc: prefix
            }

            // Fallback: simple prefix check
            if (url.startsWith("postgres://")) {
                url = "jdbc:postgresql://" + url.substring("postgres://".length());
            } else if (url.startsWith("postgresql://")) {
                url = "jdbc:postgresql://" + url.substring("postgresql://".length());
            }

            Map<String, Object> props = new HashMap<>();
            props.put("spring.datasource.url", url);
            environment.getPropertySources().addFirst(new MapPropertySource("normalizedDatabaseUrl", props));
        }
    }
}
