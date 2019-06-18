package com.atlassian.jira.cloud.jenkins.tenantinfo;

/**
 * Response object when resolving Jira site URL to cloud ID
 */
public class TenantInfo {

    private String cloudId;

    public String getCloudId() {
        return cloudId;
    }

    public void setCloudId(final String cloudId) {
        this.cloudId = cloudId;
    }
}
