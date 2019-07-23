package com.atlassian.jira.cloud.jenkins.provider;

import com.atlassian.jira.cloud.jenkins.BaseMockServerTest;
import com.atlassian.jira.cloud.jenkins.HttpClientProviderTestGenerator;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.Before;
import org.junit.Test;

import javax.inject.Inject;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class HttpClientProviderTest extends BaseMockServerTest {

    @Inject
    private OkHttpClient httpClient;

    @Before
    public void setup() throws IOException {
        super.setup();
        httpClient = new HttpClientProvider().httpClient();
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
    public void testNoRetryFor4XX() throws IOException {
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

    private Request getRequest() {
        return new Request.Builder().url(server.url("/test")).build();
    }

}
