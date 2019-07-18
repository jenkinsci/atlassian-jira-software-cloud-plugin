package com.atlassian.jira.cloud.jenkins.provider;

import com.google.inject.Provides;
import okhttp3.Interceptor;
import okhttp3.Interceptor.Chain;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;

/** OkHttpClient with appropriate default timeouts */
public class HttpClientProvider {

    private static final Logger log = LoggerFactory.getLogger(HttpClientProvider.class);
    private final OkHttpClient httpClient;

    public HttpClientProvider() {
        httpClient =
                new OkHttpClient.Builder()
                        .connectTimeout(Duration.ofMillis(5000))
                        .readTimeout(Duration.ofMillis(5000))
                        .writeTimeout(Duration.ofMillis(5000))
                        .addInterceptor(retryInterceptor())
                        .build();
    }

    private Interceptor retryInterceptor() {
        return chain -> {
            Request request = chain.request();
            Response response = chain.proceed(request);
            response = performRetry(chain, request, response);

            return response;
        };
    }

    private Response performRetry(
            final Chain chain, final Request request, final Response originalResponse)
            throws IOException {
        Response response = originalResponse;
        final int MAX_RETRIES = 3;
        int currentAttempt = 1;

        while (response.code() >= 500 && currentAttempt <= MAX_RETRIES) {
            log.warn(
                    String.format(
                            "Received %d for request to %s. Retry attempt %d of %d.",
                            response.code(),
                            response.request().url(),
                            currentAttempt,
                            MAX_RETRIES));
            response.close();
            try {
                Thread.sleep(2000); // delay between each retry
            } catch (InterruptedException e) {
                log.error("Retry delay interrupted: " + e.getMessage());
            }
            response = chain.proceed(request);
            currentAttempt++;
        }

        return response;
    }

    @Provides
    public OkHttpClient httpClient() {
        return httpClient;
    }
}
