package com.atlassian.jira.cloud.jenkins.listeners;

import com.atlassian.jira.cloud.jenkins.config.JiraCloudPluginConfig;
import com.atlassian.jira.cloud.jenkins.config.JiraCloudSiteConfig;
import com.atlassian.jira.cloud.jenkins.logging.PipelineLogger;
import com.atlassian.jira.cloud.jenkins.pluginConfigApi.PluginConfigApi;
import com.atlassian.jira.cloud.jenkins.util.SecretRetriever;
import com.fasterxml.jackson.databind.ObjectMapper;
import hudson.Plugin;
import hudson.init.InitMilestone;
import hudson.init.Initializer;
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

    @Initializer(after = InitMilestone.JOB_LOADED)
    public void onLoaded() {
        JiraCloudPluginConfig config = JiraCloudPluginConfig.get();
        if (config != null) {
            sendConfigDataToJira(config);
        }
    }

    void sendConfigDataToJira(final JiraCloudPluginConfig config) {
        for (JiraCloudSiteConfig siteConfig : config.getSites()) {
            String webhookUrl = siteConfig.getWebhookUrl();
            String credentialsId = siteConfig.getCredentialsId();

            final Optional<String> maybeSecret = this.secretRetriever.getSecretFor(credentialsId);
            if (!maybeSecret.isPresent() || maybeSecret.get().isEmpty()) {
                LOGGER.warning(
                        "Skipping Jira configuration sync for site "
                                + siteConfig.getSite()
                                + " because secret for credentials '"
                                + credentialsId
                                + "' is not available yet.");
                continue;
            }
            String secret = maybeSecret.get();

            try {
                this.pluginConfigApi.sendConnectionData(
                        webhookUrl,
                        secret,
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
