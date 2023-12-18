package com.atlassian.jira.cloud.jenkins.listeners;

import com.atlassian.jira.cloud.jenkins.config.JiraCloudPluginConfig;
import com.atlassian.jira.cloud.jenkins.config.JiraCloudSiteConfig;
import com.atlassian.jira.cloud.jenkins.logging.PipelineLogger;
import com.atlassian.jira.cloud.jenkins.pluginConfigApi.PluginConfigApi;
import com.atlassian.jira.cloud.jenkins.util.SecretRetriever;
import com.fasterxml.jackson.databind.ObjectMapper;
import hudson.Plugin;
import jenkins.model.Jenkins;
import okhttp3.OkHttpClient;

import java.util.Optional;
import java.util.logging.Logger;

import static com.atlassian.jira.cloud.jenkins.util.IpAddressProvider.getIpAddress;

public class InitializePluginListener extends Plugin {
    private static final Logger LOGGER = Logger.getLogger(InitializePluginListener.class.getName());
    transient PluginConfigApi pluginConfigApi;
    transient SecretRetriever secretRetriever;

    public InitializePluginListener() {
        this.secretRetriever = new SecretRetriever();
        this.pluginConfigApi = new PluginConfigApi(new OkHttpClient(), new ObjectMapper());
    }

    @Override
    public void postInitialize() throws Exception {
        super.postInitialize();
        JiraCloudPluginConfig config = JiraCloudPluginConfig.get();
        sendConfigDataToJira(config);
    }

    void sendConfigDataToJira(final JiraCloudPluginConfig config) {
        for (JiraCloudSiteConfig siteConfig : config.getSites()) {
            String webhookUrl = siteConfig.getWebhookUrl();
            String credentialsId = siteConfig.getCredentialsId();

            final Optional<String> maybeSecret = this.secretRetriever.getSecretFor(credentialsId);
            try {
                this.pluginConfigApi.sendConnectionData(
                        webhookUrl,
                        maybeSecret.get(),
                        getIpAddress(),
                        config.getAutoBuildsEnabled(),
                        config.getAutoBuildsRegex(),
                        config.getAutoDeploymentsEnabled(),
                        config.getAutoDeploymentsRegex(),
                        PipelineLogger.noopInstance());
            } catch (Exception e) {
                LOGGER.severe(e.getMessage());
            }
        }
    }
}
