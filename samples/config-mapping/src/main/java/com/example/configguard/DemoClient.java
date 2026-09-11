package com.example.configguard;

import org.springframework.beans.factory.annotation.Value;

/** Static-analysis input only; no network request or application entry point. */
public class DemoClient {
    @Value("${demo.service.url}")
    private String serviceUrl;

    @Value("${demo.remote.token:DEMO_DEFAULT_DO_NOT_USE}")
    private String optionalToken;

    @Value("${demo.required.key}")
    private String requiredKey;
}
