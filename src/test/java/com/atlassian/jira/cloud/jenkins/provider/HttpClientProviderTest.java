package com.atlassian.jira.cloud.jenkins.provider;

import com.atlassian.jira.cloud.jenkins.BaseMockServerTest;
import com.atlassian.jira.cloud.jenkins.Config;
import com.atlassian.jira.cloud.jenkins.HttpClientProviderTestGenerator;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.Before;
import org.junit.Test;

import javax.inject.Inject;
import java.io.IOException;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class HttpClientProviderTest extends BaseMockServerTest {

    private static final int LIMIT_FOR_PERIOD = 3;

    @Inject private OkHttpClient httpClient;

    @Before
    public void setup() throws IOException {
        super.setup();
        httpClient = new HttpClientProvider().httpClient();
        final RateLimiterRegistry rateLimiterRegistry = Config.RATE_LIMITER_REGISTRY;

        rateLimiterRegistry.addConfiguration(
                Config.ATLASSIAN_RATE_LIMITER_CONFIG,
                RateLimiterConfig.custom()
                        .limitForPeriod(LIMIT_FOR_PERIOD)
                        .limitRefreshPeriod(Duration.ofMinutes(1))
                        .timeoutDuration(Duration.ofSeconds(1))
                        .build());
    }

    @Test
    public void testNoRetryFor2XX() throws IOException {
        // setup
        HttpClientProviderTestGenerator.succeedWith2XXOnInitialAttempt(this);
        final Request request = getRequest();

        // execute
        final Response response = httpClient.newCall(request).execute();

        // verify
        assertThat(response.code()).isEqualTo(202);
        assertThat(server.getRequestCount()).isEqualTo(1); // first request only
    }

    @Test
    public void testNoRetryForGeneral4XX() throws IOException {
        // setup
        HttpClientProviderTestGenerator.failWith4XXOnInitialAttempt(this);
        final Request request = getRequest();

        // execute
        final Response response = httpClient.newCall(request).execute();

        // verify
        assertThat(response.code()).isEqualTo(404);
        assertThat(server.getRequestCount()).isEqualTo(1); // first request only
    }

    @Test
    public void testRetryOnceFor5XXAndSucceed() throws IOException {
        // setup
        HttpClientProviderTestGenerator.failWith503AndThenSucceed2XX(this);
        final Request request = getRequest();

        // execute
        final Response response = httpClient.newCall(request).execute();

        // verify
        assertThat(response.code()).isEqualTo(202);
        assertThat(server.getRequestCount()).isEqualTo(2); // 1 actual request + 1 retry
    }

    @Test
    public void testFailFor5XXAfterThreeRetries() throws IOException {
        // setup
        HttpClientProviderTestGenerator.failWith503ForAllAttempts(this);
        final Request request = getRequest();

        // execute
        final Response response = httpClient.newCall(request).execute();

        // verify
        assertThat(response.code()).isEqualTo(503);
        assertThat(server.getRequestCount()).isEqualTo(4); // 1 actual request + 3 retries
    }

    @Test
    public void testUserAgentHeader() throws Exception {
        // setup
        HttpClientProviderTestGenerator.succeedWith2XXOnInitialAttempt(this);
        final Request request = getRequest();

        // execute
        final Response response = httpClient.newCall(request).execute();

        // verify
        assertThat(response.code()).isEqualTo(202);
        final RecordedRequest recordedRequest = server.takeRequest();
        assertThat(recordedRequest.getHeader("User-Agent"))
                .isEqualTo("atlassian-jira-software-cloud-plugin");
    }

    @Test
    public void testFailForGate404AfterThreeRetries() throws IOException {
        // setup
        HttpClientProviderTestGenerator.failWith404ForAllAttempts(this);
        final Request request = getGateRequest();

        // execute
        final Response response = httpClient.newCall(request).execute();

        // verify
        assertThat(response.code()).isEqualTo(404);
        assertThat(server.getRequestCount()).isEqualTo(4); // 1 actual request + 3 retries
    }

    @Test
    public void testRetryOnceFor404AndSucceed() throws IOException {
        // setup
        HttpClientProviderTestGenerator.failWith404AndThenSucceed2XX(this);
        final Request request = getGateRequest();

        // execute
        final Response response = httpClient.newCall(request).execute();

        // verify
        assertThat(response.code()).isEqualTo(200);
        assertThat(server.getRequestCount()).isEqualTo(2); // 1 actual request + 1 retry
    }

    @Test
    public void testClientRichLimits() {
        // setup
        final int numberOfAttempts = LIMIT_FOR_PERIOD + 1;
        HttpClientProviderTestGenerator.succeedWith2XXForAttempts(this, numberOfAttempts);
        final String clientId = "1";
        final Request request = getRequest().newBuilder().tag(String.class, clientId).build();

        // execute
        try {
            for (int i = 0; i < numberOfAttempts; i++) {
                httpClient.newCall(request).execute();
            }
        }

        // verify
        catch (Exception e) {
            assertTrue(e instanceof RequestNotPermitted);
            assertEquals(
                    String.format("RateLimiter '%s' does not permit further calls", clientId),
                    e.getMessage());
        }
    }

    @Test
    public void testClientIdDoesntProvided_limitsDidntRich() throws Exception {
        // setup
        final int numberOfAttempts = LIMIT_FOR_PERIOD + 1;
        HttpClientProviderTestGenerator.succeedWith2XXForAttempts(this, numberOfAttempts);
        final Request request = getRequest();

        // execute
        for (int i = 0; i < numberOfAttempts; i++) {
            final Response response = httpClient.newCall(request).execute();
        }

        // verify
        assertThat(server.getRequestCount()).isEqualTo(numberOfAttempts);
    }

    private Request getRequest() {
        return new Request.Builder().url(server.url("/test")).build();
    }

    private Request getGateRequest() {
        return new Request.Builder().url(server.url("/gating-status")).build();
    }
}
