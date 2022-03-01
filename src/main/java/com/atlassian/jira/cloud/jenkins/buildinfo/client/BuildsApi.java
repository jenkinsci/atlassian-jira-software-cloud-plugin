package com.atlassian.jira.cloud.jenkins.buildinfo.client;

import com.atlassian.jira.cloud.jenkins.buildinfo.client.model.BuildApiResponse;
import com.atlassian.jira.cloud.jenkins.buildinfo.client.model.Builds;
import com.atlassian.jira.cloud.jenkins.common.client.ApiUpdateFailedException;
import com.atlassian.jira.cloud.jenkins.common.client.JenkinsAppApi;
import com.atlassian.jira.cloud.jenkins.common.client.JenkinsAppEventRequest;
import com.atlassian.jira.cloud.jenkins.common.client.JenkinsAppRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import org.jetbrains.annotations.NotNull;

public class BuildsApi extends JenkinsAppApi<BuildApiResponse> {

    public BuildsApi(final OkHttpClient httpClient, final ObjectMapper objectMapper) {
        super(httpClient, objectMapper);
    }

    public BuildApiResponse sendBuildAsJwt(
            final String webhookUrl, final Builds buildsRequest, final String secret)
            throws ApiUpdateFailedException {

        JenkinsAppRequest request = createRequest(buildsRequest);
        return this.sendRequestAsJwt(webhookUrl, secret, request, BuildApiResponse.class);
    }

    @NotNull
    private JenkinsAppRequest createRequest(final Builds buildsRequest) {
        return new JenkinsAppEventRequest(
                JenkinsAppEventRequest.EventType.BUILD,
                buildsRequest.getBuild().getPipelineId(),
                buildsRequest.getBuild().getDisplayName(),
                buildsRequest.getBuild().getState(),
                buildsRequest.getBuild().getLastUpdated(),
                buildsRequest);
    }
}
