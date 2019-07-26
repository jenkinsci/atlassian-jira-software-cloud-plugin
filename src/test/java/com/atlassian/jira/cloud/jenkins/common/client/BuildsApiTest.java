package com.atlassian.jira.cloud.jenkins.common.client;

import com.atlassian.jira.cloud.jenkins.BaseMockServerTest;
import com.atlassian.jira.cloud.jenkins.BuildsApiTestGenerator;
import com.atlassian.jira.cloud.jenkins.buildinfo.client.model.BuildApiResponse;
import com.atlassian.jira.cloud.jenkins.buildinfo.client.model.Builds;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import org.junit.Before;
import org.junit.Test;

import javax.inject.Inject;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class BuildsApiTest extends BaseMockServerTest {

    @Inject private OkHttpClient httpClient;
    @Inject private ObjectMapper objectMapper;
    private JiraApi buildsApi;

    private static String CLOUD_ID = "cloud_id";
    private static String ACCESS_TOKEN = "access_token";
    private static String JIRA_SITE_URL = "https://site.atlassian.net";

    @Before
    public void setup() throws IOException {
        super.setup();
        buildsApi = new JiraApi(httpClient, objectMapper, JIRA_SITE_URL);
        buildsApi.setApiEndpoint(server.url("").toString());
    }

    @Test
    public void testBuildIsSubmittedSuccessfully() throws IOException {
        // setup
        BuildsApiTestGenerator.successfulResponse(this);

        // execute
        final PostUpdateResult<BuildApiResponse> result =
                buildsApi.postUpdate(
                        CLOUD_ID,
                        ACCESS_TOKEN,
                        JIRA_SITE_URL,
                        mock(Builds.class),
                        BuildApiResponse.class);

        // verify
        assertThat(result.getResponseEntity().isPresent()).isTrue();
        final BuildApiResponse buildApiResponse = result.getResponseEntity().get();
        assertThat(buildApiResponse.getAcceptedBuilds()).hasSize(1);
        assertThat(buildApiResponse.getAcceptedBuilds().get(0).getBuildNumber()).isEqualTo(35);
        assertThat(buildApiResponse.getAcceptedBuilds().get(0).getPipelineId())
                .isEqualTo("Test Pipeline Build");
        assertThat(buildApiResponse.getRejectedBuilds()).hasSize(0);
    }

    @Test
    public void testBuildWithInvalidURL() throws IOException {
        // setup
        BuildsApiTestGenerator.failedResponseInvalidURL(this);

        // execute
        final PostUpdateResult<BuildApiResponse> result =
                buildsApi.postUpdate(
                        CLOUD_ID,
                        ACCESS_TOKEN,
                        JIRA_SITE_URL,
                        mock(Builds.class),
                        BuildApiResponse.class);

        // verify
        assertThat(result.getResponseEntity().isPresent()).isTrue();
        final BuildApiResponse buildApiResponse = result.getResponseEntity().get();
        assertThat(buildApiResponse.getAcceptedBuilds()).hasSize(0);
        assertThat(buildApiResponse.getRejectedBuilds()).hasSize(1);
        assertThat(buildApiResponse.getRejectedBuilds().get(0).getKey().getBuildNumber())
                .isEqualTo(36);
        assertThat(buildApiResponse.getRejectedBuilds().get(0).getKey().getPipelineId())
                .isEqualTo("Test Pipeline Build");
        assertThat(buildApiResponse.getRejectedBuilds().get(0).getErrors()).hasSize(1);
        assertThat(buildApiResponse.getRejectedBuilds().get(0).getErrors().get(0).getMessage())
                .isEqualTo("'url' is required");
    }

    @Test
    public void testBuildUnknownIssueKeys() throws IOException {
        // setup
        BuildsApiTestGenerator.failedResponseUnknownIssueKey(this);

        // execute
        final PostUpdateResult<BuildApiResponse> result =
                buildsApi.postUpdate(
                        CLOUD_ID,
                        ACCESS_TOKEN,
                        JIRA_SITE_URL,
                        mock(Builds.class),
                        BuildApiResponse.class);

        // verify
        assertThat(result.getResponseEntity().isPresent()).isTrue();
        final BuildApiResponse buildApiResponse = result.getResponseEntity().get();
        assertThat(buildApiResponse.getAcceptedBuilds()).hasSize(0);
        assertThat(buildApiResponse.getRejectedBuilds()).hasSize(1);
        assertThat(buildApiResponse.getRejectedBuilds().get(0).getKey().getBuildNumber())
                .isEqualTo(36);
        assertThat(buildApiResponse.getRejectedBuilds().get(0).getKey().getPipelineId())
                .isEqualTo("Test Pipeline Build");
        assertThat(buildApiResponse.getRejectedBuilds().get(0).getErrors()).hasSize(1);
        assertThat(buildApiResponse.getRejectedBuilds().get(0).getErrors().get(0).getMessage())
                .isEqualTo("No valid issues found for build");
        assertThat(buildApiResponse.getUnknownIssueKeys()).hasSize(1);
        assertThat(buildApiResponse.getUnknownIssueKeys().get(0)).isEqualTo("TEST-110");
    }

    @Test
    public void testInvalidAccessToken() throws IOException {
        // setup
        BuildsApiTestGenerator.unauthorizedResponse(this);

        // execute
        final PostUpdateResult<BuildApiResponse> result =
                buildsApi.postUpdate(
                        CLOUD_ID,
                        ACCESS_TOKEN,
                        JIRA_SITE_URL,
                        mock(Builds.class),
                        BuildApiResponse.class);

        // verify
        assertThat(result.getErrorMessage().isPresent()).isTrue();
        assertThat(result.getErrorMessage().get())
                .isEqualTo("Error response code 401 when submitting to https://site.atlassian.net");
    }

    @Test
    public void testServerError() throws IOException {
        // setup
        BuildsApiTestGenerator.serverError(this);

        // execute
        final PostUpdateResult<BuildApiResponse> result =
                buildsApi.postUpdate(
                        CLOUD_ID,
                        ACCESS_TOKEN,
                        JIRA_SITE_URL,
                        mock(Builds.class),
                        BuildApiResponse.class);

        // verify
        assertThat(result.getErrorMessage().isPresent()).isTrue();
        assertThat(result.getErrorMessage().get())
                .isEqualTo("Error response code 500 when submitting to https://site.atlassian.net");
    }
}
