package com.atlassian.jira.cloud.jenkins.buildinfo.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.atlassian.jira.cloud.jenkins.BaseMockServerTest;
import com.atlassian.jira.cloud.jenkins.BuildsApiTestGenerator;
import com.atlassian.jira.cloud.jenkins.buildinfo.client.model.JiraBuildInfo;
import com.atlassian.jira.cloud.jenkins.buildinfo.client.model.BuildApiResponse;
import okhttp3.OkHttpClient;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class BuildsApiTest extends BaseMockServerTest {

    @Inject private OkHttpClient httpClient;
    @Inject private ObjectMapper objectMapper;
    private BuildsApi buildsApi;

    private static String CLOUD_ID = "cloud_id";
    private static String ACCESS_TOKEN = "access_token";
    private static String JIRA_SITE_URL = "https://site.atlassian.net";

    @Rule public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setup() throws IOException {
        super.setup();
        buildsApi = new BuildsApi(httpClient, objectMapper);
        buildsApi.setBuildsApiEndpoint(server.url("").toString());
    }

    @Test
    public void testBuildIsSubmittedSuccessfully() throws IOException {
        // setup
        BuildsApiTestGenerator.successfulResponse(this);

        // execute
        Optional<BuildApiResponse> buildApiResponse =
                buildsApi.postBuildUpdate(
                        CLOUD_ID, ACCESS_TOKEN, JIRA_SITE_URL, mock(JiraBuildInfo.class));

        // verify
        assertThat(buildApiResponse.isPresent()).isTrue();
        assertThat(buildApiResponse.get().getAcceptedBuilds()).hasSize(1);
        assertThat(buildApiResponse.get().getAcceptedBuilds().get(0).getBuildNumber())
                .isEqualTo(35);
        assertThat(buildApiResponse.get().getAcceptedBuilds().get(0).getPipelineId())
                .isEqualTo("Test Pipeline Build");
        assertThat(buildApiResponse.get().getRejectedBuilds()).hasSize(0);
    }

    @Test
    public void testBuildWithInvalidURL() throws IOException {
        // setup
        BuildsApiTestGenerator.failedResponseInvalidURL(this);

        // execute
        Optional<BuildApiResponse> buildApiResponse =
                buildsApi.postBuildUpdate(
                        CLOUD_ID, ACCESS_TOKEN, JIRA_SITE_URL, mock(JiraBuildInfo.class));

        // verify
        assertThat(buildApiResponse.isPresent()).isTrue();
        assertThat(buildApiResponse.get().getAcceptedBuilds()).hasSize(0);
        assertThat(buildApiResponse.get().getRejectedBuilds()).hasSize(1);
        assertThat(buildApiResponse.get().getRejectedBuilds().get(0).getKey().getBuildNumber())
                .isEqualTo(36);
        assertThat(buildApiResponse.get().getRejectedBuilds().get(0).getKey().getPipelineId())
                .isEqualTo("Test Pipeline Build");
        assertThat(buildApiResponse.get().getRejectedBuilds().get(0).getErrors()).hasSize(1);
        assertThat(
                        buildApiResponse
                                .get()
                                .getRejectedBuilds()
                                .get(0)
                                .getErrors()
                                .get(0)
                                .getMessage())
                .isEqualTo("'url' is required");
    }

    @Test
    public void testBuildUnknownIssueKeys() throws IOException {
        // setup
        BuildsApiTestGenerator.failedResponseUnknownIssueKey(this);

        // execute
        Optional<BuildApiResponse> buildApiResponse =
                buildsApi.postBuildUpdate(
                        CLOUD_ID, ACCESS_TOKEN, JIRA_SITE_URL, mock(JiraBuildInfo.class));

        // verify
        assertThat(buildApiResponse.isPresent()).isTrue();
        assertThat(buildApiResponse.get().getAcceptedBuilds()).hasSize(0);
        assertThat(buildApiResponse.get().getRejectedBuilds()).hasSize(1);
        assertThat(buildApiResponse.get().getRejectedBuilds().get(0).getKey().getBuildNumber())
                .isEqualTo(36);
        assertThat(buildApiResponse.get().getRejectedBuilds().get(0).getKey().getPipelineId())
                .isEqualTo("Test Pipeline Build");
        assertThat(buildApiResponse.get().getRejectedBuilds().get(0).getErrors()).hasSize(1);
        assertThat(
                        buildApiResponse
                                .get()
                                .getRejectedBuilds()
                                .get(0)
                                .getErrors()
                                .get(0)
                                .getMessage())
                .isEqualTo("No valid issues found for build");
        assertThat(buildApiResponse.get().getUnknownIssueKeys()).hasSize(1);
        assertThat(buildApiResponse.get().getUnknownIssueKeys().get(0)).isEqualTo("TEST-110");
    }

    @Test
    public void testInvalidAccessToken() throws IOException {
        // setup
        BuildsApiTestGenerator.unauthorizedResponse(this);

        // assertions
        expectedException.expect(BuildUpdateFailedException.class);
        expectedException.expectMessage(
                "Error response code 401 when submitting builds for https://site.atlassian.net");

        // execute
        buildsApi.postBuildUpdate(CLOUD_ID, ACCESS_TOKEN, JIRA_SITE_URL, mock(JiraBuildInfo.class));
    }

    @Test
    public void testServerError() throws IOException {
        // setup
        BuildsApiTestGenerator.serverError(this);

        // assertions
        expectedException.expect(BuildUpdateFailedException.class);
        expectedException.expectMessage(
                "Error response code 500 when submitting builds for https://site.atlassian.net");

        // execute
        buildsApi.postBuildUpdate(CLOUD_ID, ACCESS_TOKEN, JIRA_SITE_URL, mock(JiraBuildInfo.class));
    }
}
