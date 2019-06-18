package com.atlassian.jira.cloud.jenkins.provider;

import com.google.inject.Provides;
import okhttp3.OkHttpClient;

import java.time.Duration;

/**
 * OkHttpClient with appropriate default timeouts
 */
public class HttpClientProvider {

    private final OkHttpClient httpClient;

    public HttpClientProvider() {
        httpClient = new OkHttpClient.Builder()
                .connectTimeout(Duration.ofMillis(5000))
                .readTimeout(Duration.ofMillis(5000))
                .writeTimeout(Duration.ofMillis(5000))
                .build();
    }

    @Provides
    public OkHttpClient httpClient() {
        return httpClient;
    }
}
