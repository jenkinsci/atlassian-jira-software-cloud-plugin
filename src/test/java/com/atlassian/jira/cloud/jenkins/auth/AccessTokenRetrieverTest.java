package com.atlassian.jira.cloud.jenkins.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.atlassian.jira.cloud.jenkins.AccessTokenRetrieverTestGenerator;
import com.atlassian.jira.cloud.jenkins.BaseMockServerTest;
import com.atlassian.jira.cloud.jenkins.common.model.AppCredential;
import okhttp3.OkHttpClient;
import org.junit.Before;
import org.junit.Test;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class AccessTokenRetrieverTest extends BaseMockServerTest {

    @Inject private OkHttpClient httpClient;
    @Inject private ObjectMapper objectMapper;
    private AccessTokenRetriever accessTokenRetriever;
    private static final String CLIENT_ID = "CLIENT_ID";
    private static final String CLIENT_SECRET = "CLIENT_SECRET";

    @Before
    public void setup() throws IOException {
        super.setup();
        accessTokenRetriever = new AccessTokenRetriever(httpClient, objectMapper);
        accessTokenRetriever.setAccessTokenEndpoint(server.url("").toString());
    }

    @Test
    public void testAccessTokenIsRetrievedSuccessfully() {
        server.enqueue(AccessTokenRetrieverTestGenerator.successfulResponse());
        Optional<String> accessToken =
                accessTokenRetriever.getAccessToken(
                        new AppCredential(CLIENT_ID, CLIENT_SECRET), "");
        assertThat(accessToken.orElse("")).isEqualTo("successful_access_token");
    }

    @Test
    public void testAccessTokenClientError() {
        server.enqueue(AccessTokenRetrieverTestGenerator.notFound());
        Optional<String> accessToken =
                accessTokenRetriever.getAccessToken(
                        new AppCredential(CLIENT_ID, CLIENT_SECRET), "");
        assertThat(accessToken.isPresent()).isFalse();
    }

    @Test
    public void testAccessTokenServerError() {
        server.enqueue(AccessTokenRetrieverTestGenerator.serverError());
        Optional<String> accessToken =
                accessTokenRetriever.getAccessToken(
                        new AppCredential(CLIENT_ID, CLIENT_SECRET), "");
        assertThat(accessToken.isPresent()).isFalse();
    }

    @Test
    public void testAccessTokenNotAJsonResponse() {
        server.enqueue(AccessTokenRetrieverTestGenerator.invalidResponseBody());
        Optional<String> accessToken =
                accessTokenRetriever.getAccessToken(
                        new AppCredential(CLIENT_ID, CLIENT_SECRET), "");
        assertThat(accessToken.isPresent()).isFalse();
    }

    @Test
    public void testNoAccessToken() {
        server.enqueue(AccessTokenRetrieverTestGenerator.accessTokenAbsent());
        Optional<String> accessToken =
                accessTokenRetriever.getAccessToken(
                        new AppCredential(CLIENT_ID, CLIENT_SECRET), "");
        assertThat(accessToken.isPresent()).isFalse();
    }
}
