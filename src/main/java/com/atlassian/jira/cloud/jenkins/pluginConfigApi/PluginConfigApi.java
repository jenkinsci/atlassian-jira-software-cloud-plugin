package com.atlassian.jira.cloud.jenkins.pluginConfigApi;

import com.atlassian.jira.cloud.jenkins.common.client.JenkinsAppApi;
import com.atlassian.jira.cloud.jenkins.logging.PipelineLogger;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;

import javax.inject.Inject;

public class PluginConfigApi extends JenkinsAppApi<PluginConfigResponse> {

    @Inject
    public PluginConfigApi(final OkHttpClient httpClient, final ObjectMapper objectMapper) {
        super(httpClient, objectMapper);
    }

    public PluginConfigResponse sendConnectionData(
            final String webhookUrl,
            final String secret,
            final String ipAddress,
            final Boolean autoBuildsEnabled,
            final String autoBuildRegex,
            final Boolean autoDeploymentsEnabled,
            final String autoDeploymentsRegex,
            final PipelineLogger pipelineLogger) {

        JenkinsAppPluginConfigRequest request =
                new JenkinsAppPluginConfigRequest(
                        ipAddress,
                        autoBuildsEnabled,
                        autoBuildRegex,
                        autoDeploymentsEnabled,
                        autoDeploymentsRegex);

        return sendRequestAsJwt(
                webhookUrl, secret, request, PluginConfigResponse.class, pipelineLogger);
    }
}
