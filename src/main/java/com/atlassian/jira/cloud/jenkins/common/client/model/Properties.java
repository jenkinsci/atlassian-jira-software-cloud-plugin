package com.atlassian.jira.cloud.jenkins.common.client.model;

/**
 * Metadata properties that can be attached to a build or a deployment
 * when submitting to Jira. This can be used to bulk delete items when needed.
 */
public class Properties {
    private String accountId;
    private String projectId;

    public Properties(final String accountId, final String projectId) {
        this.accountId = accountId;
        this.projectId = projectId;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getProjectId() {
        return projectId;
    }
}
