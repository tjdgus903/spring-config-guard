package com.example.configguard;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Exercises direct record components, a nested record and an unmatched component. */
@ConfigurationProperties(prefix = "demo")
public record DemoProperties(int maxRetries, Client client, String region) {
    public static record Client(int timeoutMs) {
    }
}
