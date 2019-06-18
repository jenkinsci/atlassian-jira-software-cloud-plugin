package com.atlassian.jira.cloud.jenkins.tenantinfo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.atlassian.jira.cloud.jenkins.BaseMockServerTest;
import com.atlassian.jira.cloud.jenkins.CloudIdResolverTestGenerator;
import okhttp3.OkHttpClient;
import org.junit.Before;
import org.junit.Test;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class CloudIdResolverTest extends BaseMockServerTest {

    @Inject private OkHttpClient httpClient;
    @Inject private ObjectMapper objectMapper;
    private CloudIdResolver cloudIdResolver;
    private String mockServerBaseUrl;

    @Before
    public void setup() throws IOException {
        super.setup();
        cloudIdResolver = new CloudIdResolver(httpClient, objectMapper);
        mockServerBaseUrl = server.url(CloudIdResolver.TENANT_INFO_ENDPOINT).toString();
    }

    @Test
    public void testCloudIdIsRetrievedSuccessfully() {
        server.enqueue(CloudIdResolverTestGenerator.successfulResponse());
        Optional<String> cloudId = cloudIdResolver.getCloudId(mockServerBaseUrl);
        assertThat(cloudId.orElse("")).isEqualTo("cloud-id");
    }

    @Test
    public void testTenantInfoClientError() {
        server.enqueue(CloudIdResolverTestGenerator.notFound());
        Optional<String> cloudId = cloudIdResolver.getCloudId(mockServerBaseUrl);
        assertThat(cloudId.isPresent()).isFalse();
    }

    @Test
    public void testTenantInfoServerError() {
        server.enqueue(CloudIdResolverTestGenerator.serverError());
        Optional<String> cloudId = cloudIdResolver.getCloudId(mockServerBaseUrl);
        assertThat(cloudId.isPresent()).isFalse();
    }

    @Test
    public void testTenantInfoNotAJsonResponse() {
        server.enqueue(CloudIdResolverTestGenerator.invalidResponseBody());
        Optional<String> cloudId = cloudIdResolver.getCloudId(mockServerBaseUrl);
        assertThat(cloudId.isPresent()).isFalse();
    }

    @Test
    public void testTenantInfoNoCloudId() {
        server.enqueue(CloudIdResolverTestGenerator.cloudIdAbsent());
        Optional<String> cloudId = cloudIdResolver.getCloudId(mockServerBaseUrl);
        assertThat(cloudId.isPresent()).isFalse();
    }
}
