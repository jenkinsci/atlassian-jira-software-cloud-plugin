package com.atlassian.jira.cloud.jenkins.buildinfo.client;

import com.atlassian.jira.cloud.jenkins.buildinfo.client.model.BuildApiResponse;
import com.atlassian.jira.cloud.jenkins.buildinfo.client.model.Builds;
import com.atlassian.jira.cloud.jenkins.common.client.ApiUpdateFailedException;
import com.atlassian.jira.cloud.jenkins.common.client.JenkinsAppApi;
import com.atlassian.jira.cloud.jenkins.common.client.JenkinsAppRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;

import java.time.Instant;

public class BuildsApi extends JenkinsAppApi<BuildApiResponse> {

    public BuildsApi(final OkHttpClient httpClient, final ObjectMapper objectMapper) {
        super(httpClient, objectMapper);
    }

    /**
     * Sends a build event to the Jenkins app in Jira.
     *
     * @param webhookUrl URL to the Jenkins app webhook.
     * @param buildsRequest the payload of the builds request.
     * @return the response of the Jenkins app webhook.
     * @throws ApiUpdateFailedException if the webhook responded with a non-successful HTTP status
     *     code.
     */
    public BuildApiResponse sendBuild(final String webhookUrl, final Builds buildsRequest)
            throws ApiUpdateFailedException {

        JenkinsAppRequest request =
                new JenkinsAppRequest(
                        JenkinsAppRequest.RequestType.EVENT,
                        JenkinsAppRequest.EventType.BUILD,
                        buildsRequest.getBuild().getPipelineId(),
                        buildsRequest.getBuild().getDisplayName(),
                        buildsRequest.getBuild().getState(),
                        buildsRequest.getBuild().getLastUpdated(),
                        buildsRequest);

        return this.sendRequest(webhookUrl, request, BuildApiResponse.class);
    }
}
