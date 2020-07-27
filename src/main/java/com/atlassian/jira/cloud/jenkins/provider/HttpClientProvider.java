package com.atlassian.jira.cloud.jenkins.provider;

import com.atlassian.jira.cloud.jenkins.Config;
import com.google.inject.Provides;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
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

    private static final String USER_AGENT = "atlassian-jira-software-cloud-plugin";

    private static final Logger log = LoggerFactory.getLogger(HttpClientProvider.class);
    private final OkHttpClient httpClient;

    public HttpClientProvider() {
        final RateLimiterRegistry rateLimiterRegistry = Config.RATE_LIMITER_REGISTRY;
        httpClient =
                new OkHttpClient.Builder()
                        .connectTimeout(Duration.ofMillis(5000))
                        .readTimeout(Duration.ofMillis(5000))
                        .writeTimeout(Duration.ofMillis(5000))
                        .addInterceptor(userAgentInterceptor())
                        .addInterceptor(retryInterceptor())
                        .addInterceptor(rateLimiterInterceptor(rateLimiterRegistry))
                        .build();
    }

    private Interceptor rateLimiterInterceptor(final RateLimiterRegistry rateLimiterRegistry) {
        return chain -> {
            final Request request = chain.request();
            final String clientId = request.header("clientId");
            if (clientId != null) {
                final RateLimiter rateLimiter =
                        rateLimiterRegistry.rateLimiter(
                                clientId, Config.ATLASSIAN_RATE_LIMITER_CONFIG);
                try {
                    return rateLimiter.executeCallable(() -> chain.proceed(request));
                } catch (Exception e) {
                    // case Http client errors
                    // it should be always IOException since it's a contract of Interceptor
                    if (e instanceof IOException) {
                        throw (IOException) e;
                    }
                    // case over limits
                    else if (e instanceof RequestNotPermitted) {
                        throw (RequestNotPermitted) e;
                    }
                    // all other cases
                    else {
                        throw new RuntimeException(e);
                    }
                }
            }
            return chain.proceed(request);
        };
    }

    private Interceptor retryInterceptor() {
        return chain -> {
            Request request = chain.request();
            Response response = chain.proceed(request);
            response = performRetry(chain, request, response);

            return response;
        };
    }

    private Interceptor userAgentInterceptor() {
        return chain -> {
            final Request originalRequest = chain.request();
            final Request userAgentRequest =
                    originalRequest.newBuilder().header("User-Agent", USER_AGENT).build();
            return chain.proceed(userAgentRequest);
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
                Thread.currentThread().interrupt();
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
