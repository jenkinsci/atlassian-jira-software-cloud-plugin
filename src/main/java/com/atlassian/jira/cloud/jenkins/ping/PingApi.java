package com.atlassian.jira.cloud.jenkins.ping;

import com.atlassian.jira.cloud.jenkins.common.client.JenkinsAppApi;
import com.atlassian.jira.cloud.jenkins.config.JiraCloudPluginConfig;
import com.atlassian.jira.cloud.jenkins.logging.PipelineLogger;
import com.fasterxml.jackson.databind.ObjectMapper;
// import jenkins.model.Jenkins;
import okhttp3.OkHttpClient;

import javax.inject.Inject;

public class PingApi extends JenkinsAppApi<PingResponse> {

    @Inject
    public PingApi(final OkHttpClient httpClient, final ObjectMapper objectMapper) {
        super(httpClient, objectMapper);
    }

    /**
     * Sends a "ping" to the Jenkins app. The Jenkins app will return true only if it could
     * successfully validate the JWT that contains the payload (i.e. only if the shared secret is
     * the same on both sides). This effectively tests if the connection between Jenkins plugin and
     * app is valid.
     */
    public boolean sendPing(
            final String webhookUrl,
            final String secret,
            final PipelineLogger pipelineLogger,
            final String ipAddress,
            final Boolean autoBuildsEnabled,
            final String autoBuildRegex) {

        JenkinsAppPingRequest request =
                new JenkinsAppPingRequest(ipAddress, autoBuildsEnabled, autoBuildRegex);
        PingResponse response =
                sendRequestAsJwt(webhookUrl, secret, request, PingResponse.class, pipelineLogger);
        return response.getSuccess();
    }
}
