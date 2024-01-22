package com.atlassian.jira.cloud.jenkins.common.model;

import java.util.Optional;

public class PluginSiteData {
    private String webhookUrl;
    private String credentialsId;

    public PluginSiteData(final String webhookUrl, final String credentialsId) {
        this.webhookUrl = webhookUrl;
        this.credentialsId = credentialsId;
    }

    public String getWebhookUrl() {
        return webhookUrl;
    }

    public String getCredentialsId() {
        return credentialsId;
    }
}
