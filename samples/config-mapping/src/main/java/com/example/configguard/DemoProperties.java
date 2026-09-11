package com.example.configguard;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Exercises direct fields, a static nested class and an unmatched field. */
@ConfigurationProperties(prefix = "demo")
public class DemoProperties {
    private int maxRetries;
    private Client client;
    private String region;

    public int getMaxRetries() { return maxRetries; }
    public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }
    public Client getClient() { return client; }
    public void setClient(Client client) { this.client = client; }
    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    public static class Client {
        private int timeoutMs;

        public int getTimeoutMs() { return timeoutMs; }
        public void setTimeoutMs(int timeoutMs) { this.timeoutMs = timeoutMs; }
    }
}
