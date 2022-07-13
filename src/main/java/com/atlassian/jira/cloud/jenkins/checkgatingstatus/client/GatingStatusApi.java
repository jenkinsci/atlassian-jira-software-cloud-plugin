package com.atlassian.jira.cloud.jenkins.checkgatingstatus.client;

import com.atlassian.jira.cloud.jenkins.checkgatingstatus.client.model.GatingStatusRequest;
import com.atlassian.jira.cloud.jenkins.checkgatingstatus.client.model.GatingStatusResponse;
import com.atlassian.jira.cloud.jenkins.common.client.JenkinsAppApi;
import com.atlassian.jira.cloud.jenkins.logging.PipelineLogger;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;

public class GatingStatusApi extends JenkinsAppApi<GatingStatusResponse> {

    public GatingStatusApi(
            final OkHttpClient httpClient,
            final ObjectMapper objectMapper,
            final PipelineLogger pipelineLogger) {
        super(httpClient, objectMapper, pipelineLogger);
    }

    public GatingStatusResponse getGatingStatus(
            final String webhookUrl,
            final String secret,
            final String deploymentId,
            final String pipelineId,
            final String environmentId) {
        return sendRequestAsJwt(
                webhookUrl,
                secret,
                new GatingStatusRequest(deploymentId, pipelineId, environmentId),
                GatingStatusResponse.class);
    }
}
